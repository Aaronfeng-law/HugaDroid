package com.soogoino.huga.ui.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.data.prefs.AuthType
import com.soogoino.huga.data.repository.SecureTokenStore
import com.soogoino.huga.git.GitAuth
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.GitResult
import com.soogoino.huga.git.SshKeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// Screen-level imports
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soogoino.huga.R

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class SetupUiState(
    val repoUrl: String = "",
    val authType: AuthType = AuthType.PAT,
    val patToken: String = "",
    val patUsername: String = "oauth2",
    val sshPublicKey: String = "",
    val hasExistingSshKey: Boolean = false,
    val authorName: String = "",
    val authorEmail: String = "",
    val isCloning: Boolean = false,
    val cloneProgress: Int = 0,
    val cloneTask: String = "",
    val error: String? = null,
    val isComplete: Boolean = false,
    val showToken: Boolean = false,
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
    private val secureTokenStore: SecureTokenStore,
) : ViewModel() {

    private val sshDir get() = File(context.filesDir, ".ssh")

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SetupEvent>()
    val events: SharedFlow<SetupEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // THR-03: SshKeyManager reads files — must run on IO dispatcher
            val hasKey = withContext(Dispatchers.IO) { sshKeyManager.hasKey(sshDir) }
            val pubKey = if (hasKey) withContext(Dispatchers.IO) { sshKeyManager.readPublicKey(sshDir) ?: "" } else ""
            val token = secureTokenStore.getToken()
            prefs.settings.first().let { s ->
                _uiState.update {
                    it.copy(
                        repoUrl = s.repoUrl,
                        authType = s.authType,
                        patToken = token,
                        sshPublicKey = pubKey,
                        hasExistingSshKey = hasKey,
                        authorName = s.authorName,
                        authorEmail = s.authorEmail,
                    )
                }
            }
        }
    }

    fun onRepoUrlChanged(v: String) = _uiState.update { it.copy(repoUrl = v, error = null) }
    fun onAuthTypeChanged(v: AuthType) = _uiState.update { it.copy(authType = v) }
    fun onPatTokenChanged(v: String) = _uiState.update { it.copy(patToken = v) }
    fun onPatUsernameChanged(v: String) = _uiState.update { it.copy(patUsername = v) }
    fun onAuthorNameChanged(v: String) = _uiState.update { it.copy(authorName = v) }
    fun onAuthorEmailChanged(v: String) = _uiState.update { it.copy(authorEmail = v) }
    fun toggleShowToken() = _uiState.update { it.copy(showToken = !it.showToken) }

    fun generateSshKey() {
        viewModelScope.launch {
            runCatching {
                val pair = sshKeyManager.generateKeyPair(sshDir)
                _uiState.update { it.copy(sshPublicKey = pair.publicKeyOpenSsh, hasExistingSshKey = true) }
                _events.emit(SetupEvent.ShowSnackbar("Ed25519 key generated"))
            }.onFailure { e ->
                _events.emit(SetupEvent.ShowSnackbar("Key generation failed: ${e.message}"))
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

        viewModelScope.launch {
            _uiState.update { it.copy(isCloning = true, error = null) }

            val localPath = File(context.filesDir, "repo").absolutePath
            // Auto-convert HTTPS URL to SSH format when SSH auth is selected
            val remoteUrl = if (state.authType == AuthType.SSH) toSshUrl(state.repoUrl) else state.repoUrl
            val auth = when (state.authType) {
                AuthType.PAT -> GitAuth.Pat(username = state.patUsername, token = state.patToken)
                AuthType.SSH -> GitAuth.SshKey(keyPath = File(sshDir, SshKeyManager.KEY_FILENAME).absolutePath)
            }
            Log.i("SetupVM", "cloneAndSetup: authType=${state.authType}  inputUrl=${state.repoUrl}  finalUrl=$remoteUrl  auth=${auth::class.simpleName}")

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
                    // Save settings
                    if (state.authType == AuthType.PAT) secureTokenStore.saveToken(state.patToken)
                    prefs.update {
                        copy(
                            repoUrl = remoteUrl, // store the final URL (SSH-converted if applicable)
                            localRepoPath = localPath,
                            authType = state.authType,
                            sshKeyPath = if (state.authType == AuthType.SSH) File(sshDir, SshKeyManager.KEY_FILENAME).absolutePath else "",
                            authorName = state.authorName,
                            authorEmail = state.authorEmail,
                            isRepoSetup = true,
                        )
                    }
                    _uiState.update { it.copy(isCloning = false, isComplete = true) }
                    _events.emit(SetupEvent.SetupComplete)
                }
                is GitResult.Failure -> {
                    _uiState.update {
                        it.copy(isCloning = false, error = result.error.message ?: "Clone failed")
                    }
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
                    IconButton(onClick = onNavigateUp) {
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
                    supportingText = if (uiState.authType == AuthType.SSH && uiState.repoUrl.trimStart().startsWith("http")) {
                        { Text(stringResource(R.string.ssh_auto_convert_note)) }
                    } else null,
                )
            }

            // Step 2: Auth
            SetupSection(title = stringResource(R.string.authentication)) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuthRadioRow(
                        selected = uiState.authType == AuthType.PAT,
                        label = stringResource(R.string.pat_auth_label),
                        onClick = { viewModel.onAuthTypeChanged(AuthType.PAT) },
                    )
                    AuthRadioRow(
                        selected = uiState.authType == AuthType.SSH,
                        label = stringResource(R.string.ssh_auth_label),
                        onClick = { viewModel.onAuthTypeChanged(AuthType.SSH) },
                    )
                }

                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(uiState.authType == AuthType.PAT) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.patUsername,
                            onValueChange = viewModel::onPatUsernameChanged,
                            label = { Text(stringResource(R.string.git_username)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = uiState.patToken,
                            onValueChange = viewModel::onPatTokenChanged,
                            label = { Text(stringResource(R.string.personal_access_token)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (uiState.showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleShowToken) {
                                    Icon(if (uiState.showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                                }
                            },
                        )
                    }
                }

                AnimatedVisibility(uiState.authType == AuthType.SSH) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            cm.setPrimaryClip(ClipData.newPlainText("ssh pub key", uiState.sshPublicKey))
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
}

@Composable
private fun SetupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

@Composable
private fun AuthRadioRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
