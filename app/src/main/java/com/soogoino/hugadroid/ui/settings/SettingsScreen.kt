package com.soogoino.hugadroid.ui.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soogoino.hugadroid.R
import com.soogoino.hugadroid.data.prefs.AppPreferences
import com.soogoino.hugadroid.data.prefs.AppSettings
import com.soogoino.hugadroid.data.prefs.MediaStrategy
import com.soogoino.hugadroid.data.prefs.ThemeMode
import com.soogoino.hugadroid.data.repository.SecureTokenStore
import com.soogoino.hugadroid.worker.GitSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AppPreferences,
    private val secureTokenStore: SecureTokenStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAuthorName(v: String) = viewModelScope.launch { prefs.update { copy(authorName = v) } }
    fun setAuthorEmail(v: String) = viewModelScope.launch { prefs.update { copy(authorEmail = v) } }
    fun setMediaStrategy(v: MediaStrategy) = viewModelScope.launch { prefs.update { copy(mediaStrategy = v) } }
    fun setAutoSync(enabled: Boolean) = viewModelScope.launch {
        prefs.update { copy(autoSyncEnabled = enabled) }
        if (enabled) {
            // THR-02: read fresh settings to avoid stale StateFlow snapshot
            val interval = prefs.settings.first().autoSyncIntervalMinutes.toLong()
            GitSyncWorker.schedule(context, interval)
        } else {
            GitSyncWorker.cancel(context)
        }
    }
    fun setAutoSyncInterval(minutes: Int) = viewModelScope.launch {
        prefs.update { copy(autoSyncIntervalMinutes = minutes) }
        val isEnabled = prefs.settings.first().autoSyncEnabled
        if (isEnabled) GitSyncWorker.schedule(context, minutes.toLong())
    }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.update { copy(themeMode = mode) } }
    fun setLanguage(tag: String) = viewModelScope.launch {
        prefs.update { copy(appLanguage = tag) }
        // Also persist to plain SharedPreferences so attachBaseContext() can read it
        // synchronously (DataStore is async and unsafe to block on in early lifecycle).
        context.getSharedPreferences("huga_lang", android.content.Context.MODE_PRIVATE)
            .edit().putString("app_language", tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = if (tag.isEmpty()) android.os.LocaleList.getEmptyLocaleList()
                          else android.os.LocaleList.forLanguageTags(tag)
            context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = locales
        }
    }
    fun resetSetup() = viewModelScope.launch {
        // Wipe credentials first
        withContext(Dispatchers.IO) {
            secureTokenStore.clearToken()
            // Delete SSH keys
            val sshDir = context.filesDir.resolve(".ssh")
            sshDir.resolve("id_ed25519").delete()
            sshDir.resolve("id_ed25519.pub").delete()
            // Delete cloned repo
            context.filesDir.resolve("repo").deleteRecursively()
        }
        prefs.update { copy(isRepoSetup = false, repoUrl = "", localRepoPath = "", sshKeyPath = "") }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var authorName by remember(settings.authorName) { mutableStateOf(settings.authorName) }
    var authorEmail by remember(settings.authorEmail) { mutableStateOf(settings.authorEmail) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    // Notification permission launcher (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.notify_silent_warning))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Author info
            SettingsSection(title = stringResource(R.string.author), icon = Icons.Outlined.Person) {
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it; viewModel.setAuthorName(it) },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = authorEmail,
                    onValueChange = { authorEmail = it; viewModel.setAuthorEmail(it) },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Appearance
            SettingsSection(title = stringResource(R.string.appearance), icon = Icons.Outlined.Palette) {
                Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                val themeModes = listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeModes.forEachIndexed { index, (mode, labelRes) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = themeModes.size),
                            icon = { SegmentedButtonDefaults.ActiveIcon() },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }

            // Language
            SettingsSection(title = stringResource(R.string.language), icon = Icons.Outlined.Language) {
                val langOptions = listOf(
                    "" to R.string.language_system,
                    "en" to R.string.language_en,
                    "zh-TW" to R.string.language_zh_tw,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    langOptions.forEachIndexed { index, (tag, labelRes) ->
                        SegmentedButton(
                            selected = settings.appLanguage == tag,
                            onClick = { viewModel.setLanguage(tag) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = langOptions.size),
                            icon = { SegmentedButtonDefaults.ActiveIcon() },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }

            // Media strategy
            SettingsSection(title = stringResource(R.string.media_storage), icon = Icons.Outlined.Image) {
                Text(stringResource(R.string.media_storage_desc),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MediaStrategyRow(
                        selected = settings.mediaStrategy == MediaStrategy.PAGE_BUNDLE,
                        label = stringResource(R.string.page_bundle_label),
                        subtitle = stringResource(R.string.page_bundle_subtitle),
                        onClick = { viewModel.setMediaStrategy(MediaStrategy.PAGE_BUNDLE) },
                    )
                    MediaStrategyRow(
                        selected = settings.mediaStrategy == MediaStrategy.STATIC_FOLDER,
                        label = stringResource(R.string.static_folder_label),
                        subtitle = stringResource(R.string.static_folder_subtitle),
                        onClick = { viewModel.setMediaStrategy(MediaStrategy.STATIC_FOLDER) },
                    )
                }
            }

            // Auto sync
            SettingsSection(title = stringResource(R.string.background_sync), icon = Icons.Outlined.Sync) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.auto_sync), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.auto_sync_subtitle), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.autoSyncEnabled,
                        onCheckedChange = { enabled ->
                            // On Android 13+, request POST_NOTIFICATIONS when user enables auto-sync
                            if (enabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.setAutoSync(enabled)
                        },
                    )
                }

                if (settings.autoSyncEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.sync_interval, settings.autoSyncIntervalMinutes),
                        style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.autoSyncIntervalMinutes.toFloat(),
                        onValueChange = { viewModel.setAutoSyncInterval(it.toInt()) },
                        valueRange = 15f..120f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.min_interval), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.max_interval), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Repo info (URL + local path only)
            if (settings.isRepoSetup) {
                SettingsSection(title = stringResource(R.string.repository), icon = Icons.Outlined.FolderOpen) {
                    Text(stringResource(R.string.repo_url_display, settings.repoUrl), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.repo_local_display, settings.localRepoPath), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Disconnect — standalone at the very bottom
            if (settings.isRepoSetup) {
                OutlinedButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.LinkOff, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.disconnect_repository))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            icon = { Icon(Icons.Outlined.LinkOff, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.disconnect_confirm_title)) },
            text = { Text(stringResource(R.string.disconnect_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = { showDisconnectDialog = false; viewModel.resetSetup() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.disconnect_repository)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

@Composable
private fun MediaStrategyRow(selected: Boolean, label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
