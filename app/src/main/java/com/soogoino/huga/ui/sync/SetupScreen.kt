package com.soogoino.huga.ui.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.data.prefs.AuthType
import com.soogoino.huga.domain.ScanPostsUseCase
import com.soogoino.huga.git.GitAuth
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.GitResult
import com.soogoino.huga.git.SshKeyManager
import com.soogoino.huga.git.isAuthError
import com.soogoino.huga.git.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// Screen-level imports
import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soogoino.huga.BuildConfig
import com.soogoino.huga.R

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class SetupUiState(
    val repoUrl: String = "",
    val sshPublicKey: String = "",
    val hasExistingSshKey: Boolean = false,
    val authorName: String = "",
    val authorEmail: String = "",
    val isCloning: Boolean = false,
    val cloneProgress: Int = 0,
    val cloneTask: String = "",
    val error: String? = null,
    val isComplete: Boolean = false,
    /** True when a previous clone was interrupted and a partial repo dir remains. */
    val partialCloneDetected: Boolean = false,
)

sealed class SetupEvent {
    data class ShowSnackbar(val message: String) : SetupEvent()
    object SetupComplete : SetupEvent()
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
    private val gitRepository: GitRepository,
    private val sshKeyManager: SshKeyManager,
    private val scanPostsUseCase: ScanPostsUseCase,
) : ViewModel() {

    private val sshDir get() = File(context.filesDir, ".ssh")

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SetupEvent>()
    val events: SharedFlow<SetupEvent> = _events.asSharedFlow()

    /** Tracks the active clone coroutine so it can be cancelled by the user. */
    private var cloneJob: Job? = null

    init {
        viewModelScope.launch {
            // THR-03: SshKeyManager reads files — must run on IO dispatcher
            val hasKey = withContext(Dispatchers.IO) { sshKeyManager.hasKey(sshDir) }
            val pubKey = if (hasKey) withContext(Dispatchers.IO) { sshKeyManager.readPublicKey(sshDir) ?: "" } else ""
            prefs.settings.first().let { s ->
                _uiState.update {
                    it.copy(
                        repoUrl = s.repoUrl,
                        sshPublicKey = pubKey,
                        hasExistingSshKey = hasKey,
                        authorName = s.authorName,
                        authorEmail = s.authorEmail,
                    )
                }
            }
            // Detect an incomplete clone left by a previous session that was interrupted
            val hasPartial = withContext(Dispatchers.IO) {
                val repoDir = File(context.filesDir, "repo")
                repoDir.exists() && !File(repoDir, ".git/config").exists()
            }
            if (hasPartial) _uiState.update { it.copy(partialCloneDetected = true) }
        }
    }

    fun onRepoUrlChanged(v: String) = _uiState.update { it.copy(repoUrl = v, error = null) }
    fun onAuthorNameChanged(v: String) = _uiState.update { it.copy(authorName = v) }
    fun onAuthorEmailChanged(v: String) = _uiState.update { it.copy(authorEmail = v) }

    /**
     * Cancels an in-flight clone, cleans up any partial repo directory, and resets UI state.
     * Safe to call from the UI when the user confirms they want to leave during a clone.
     */
    fun cancelClone() {
        cloneJob?.cancel()
        cloneJob = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(context.filesDir, "repo").deleteRecursively() }
        }
        _uiState.update { it.copy(isCloning = false, cloneProgress = 0, cloneTask = "") }
    }

    /** Deletes the leftover partial repo directory detected on startup. */
    fun cleanupPartialClone() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(context.filesDir, "repo").deleteRecursively() }
        }
        _uiState.update { it.copy(partialCloneDetected = false) }
    }

    fun generateSshKey() {
        viewModelScope.launch {
            runCatching {
                val pair = sshKeyManager.generateKeyPair(sshDir)
                _uiState.update { it.copy(sshPublicKey = pair.publicKeyOpenSsh, hasExistingSshKey = true) }
                _events.emit(SetupEvent.ShowSnackbar(context.getString(R.string.ssh_key_generated)))
            }.onFailure { e ->
                _events.emit(SetupEvent.ShowSnackbar(context.getString(R.string.key_generation_failed, e.message ?: "")))
            }
        }
    }

    /** Convert common HTTPS repo URLs to SSH form.
     *  Supports github.com, gitlab.com, bitbucket.org and any generic host. */
    private fun toSshUrl(url: String): String {
        val normalized = url.trim().trimEnd('/').removeSuffix(".git")
        // Match any https?://<host>/<owner>/<repo>
        val httpsRegex = Regex("""https?://([^/]+)/([^/]+)/([^/]+)$""")
        val match = httpsRegex.find(normalized) ?: return url.trim()
        val (host, owner, repo) = match.destructured
        return "git@$host:$owner/$repo.git"
    }

    fun cloneAndSetup() {
        val state = _uiState.value
        if (state.repoUrl.isBlank()) { _uiState.update { it.copy(error = "Repository URL is required") }; return }

        cloneJob = viewModelScope.launch {
            _uiState.update { it.copy(isCloning = true, error = null) }

            val localPath = File(context.filesDir, "repo").absolutePath
            // Always SSH — auto-convert HTTPS URLs to SSH format
            val remoteUrl = toSshUrl(state.repoUrl)
            val auth = GitAuth.SshKey(keyPath = File(sshDir, SshKeyManager.KEY_FILENAME).absolutePath)
            if (BuildConfig.DEBUG) Log.i("SetupVM", "cloneAndSetup: inputUrl=${state.repoUrl}  finalUrl=$remoteUrl")

            val result = gitRepository.clone(
                remoteUrl = remoteUrl,
                localPath = localPath,
                auth = auth,
                onProgress = { pct, task ->
                    _uiState.update { it.copy(cloneProgress = pct, cloneTask = task) }
                }
            )

            when (result) {
                is GitResult.Success -> {
                    prefs.update {
                        copy(
                            repoUrl = remoteUrl,
                            localRepoPath = localPath,
                            authType = AuthType.SSH,
                            sshKeyPath = File(sshDir, SshKeyManager.KEY_FILENAME).absolutePath,
                            authorName = state.authorName,
                            authorEmail = state.authorEmail,
                            isRepoSetup = true,
                        )
                    }
                    // Pre-populate Room so Home & Posts show data immediately on first entry
                    runCatching { scanPostsUseCase() }
                    _uiState.update { it.copy(isCloning = false, isComplete = true) }
                    _events.emit(SetupEvent.SetupComplete)
                }
                is GitResult.Failure -> {
                    val e = result.error
                    val msg = when {
                        isNetworkError(e) -> context.getString(R.string.error_network)
                        isAuthError(e) -> context.getString(R.string.error_auth_ssh)
                        else -> e.message ?: context.getString(R.string.error_clone_failed)
                    }
                    _uiState.update { it.copy(isCloning = false, error = msg) }
                }
            }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCancelCloneDialog by remember { mutableStateOf(false) }

    // Intercept back press while a clone is in progress
    BackHandler(enabled = uiState.isCloning) { showCancelCloneDialog = true }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SetupEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is SetupEvent.SetupComplete -> onSetupComplete()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_up_repository)) },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.isCloning) showCancelCloneDialog = true else onNavigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Risk disclaimer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.risk_disclaimer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.risk_disclaimer_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Step 1: Repo URL
            SetupSection(title = stringResource(R.string.repository_url)) {
                OutlinedTextField(
                    value = uiState.repoUrl,
                    onValueChange = viewModel::onRepoUrlChanged,
                    label = { Text(stringResource(R.string.url)) },
                    placeholder = { Text(stringResource(R.string.url_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error != null && uiState.repoUrl.isBlank(),
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    supportingText = if (uiState.repoUrl.trimStart().startsWith("http")) {
                        { Text(stringResource(R.string.ssh_auto_convert_note)) }
                    } else null,
                )
            }

            // Step 2: SSH Deploy Key
            SetupSection(title = stringResource(R.string.ssh_key)) {
                if (!uiState.hasExistingSshKey) {
                    FilledTonalButton(onClick = viewModel::generateSshKey, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Key, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.generate_key_pair))
                    }
                }
                if (uiState.sshPublicKey.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.public_key_card_label), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                uiState.sshPublicKey,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.public_key), uiState.sshPublicKey))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.copy_to_clipboard))
                            }
                        }
                    }
                }
            }

            // Step 3: Author info
            SetupSection(title = stringResource(R.string.author_info)) {
                OutlinedTextField(
                    value = uiState.authorName,
                    onValueChange = viewModel::onAuthorNameChanged,
                    label = { Text(stringResource(R.string.your_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.authorEmail,
                    onValueChange = viewModel::onAuthorEmailChanged,
                    label = { Text(stringResource(R.string.your_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Error
            uiState.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Clone progress
            AnimatedVisibility(uiState.isCloning) {
                Column {
                    LinearProgressIndicator(
                        progress = { if (uiState.cloneProgress > 0) uiState.cloneProgress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(uiState.cloneTask.ifBlank { stringResource(R.string.cloning) },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Clone button
            Button(
                onClick = viewModel::cloneAndSetup,
                enabled = !uiState.isCloning && uiState.repoUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isCloning) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cloning))
                } else {
                    Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.clone_repository))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Cancel-clone confirmation dialog ──────────────────────────────────────
    if (showCancelCloneDialog) {
        AlertDialog(
            onDismissRequest = { showCancelCloneDialog = false },
            title = { Text(stringResource(R.string.clone_in_progress_title)) },
            text = { Text(stringResource(R.string.clone_in_progress_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelCloneDialog = false
                        viewModel.cancelClone()
                        onNavigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.cancel_and_go_back)) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelCloneDialog = false }) {
                    Text(stringResource(R.string.keep_waiting))
                }
            },
        )
    }

    // ── Partial / interrupted clone detected on startup ───────────────────────
    if (uiState.partialCloneDetected) {
        AlertDialog(
            onDismissRequest = { /* non-dismissable — user must choose */ },
            title = { Text(stringResource(R.string.partial_clone_title)) },
            text = { Text(stringResource(R.string.partial_clone_body)) },
            confirmButton = {
                Button(onClick = viewModel::cleanupPartialClone) {
                    Text(stringResource(R.string.cleanup_and_retry))
                }
            },
        )
    }
}

@Composable
private fun SetupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}
