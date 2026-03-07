package com.soogoino.huga.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.R
import com.soogoino.huga.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ─── Model ───────────────────────────────────────────────────────────────────

data class FileEntry(
    val file: File,
    val isDirectory: Boolean,
    val name: String,
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class FilesUiState(
    val isRepoSetup: Boolean = false,
    val repoRoot: File? = null,
    val currentDir: File? = null,
    val entries: List<FileEntry> = emptyList(),
    val isAtRoot: Boolean = true,
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { s ->
                if (s.isRepoSetup && s.localRepoPath.isNotBlank()) {
                    val root = File(s.localRepoPath)
                    _uiState.update { state ->
                        val dir = state.currentDir?.takeIf { it.exists() } ?: root
                        state.copy(
                            isRepoSetup = true,
                            repoRoot = root,
                            currentDir = dir,
                            entries = loadEntries(dir),
                            isAtRoot = isSameDir(dir, root),
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isRepoSetup = false, repoRoot = null, currentDir = null, entries = emptyList())
                    }
                }
            }
        }
    }

    private fun loadEntries(dir: File): List<FileEntry> =
        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.map { FileEntry(it, it.isDirectory, it.name) }
            ?: emptyList()

    private fun isSameDir(a: File, b: File): Boolean =
        runCatching { a.canonicalPath == b.canonicalPath }.getOrDefault(a == b)

    fun navigateInto(dir: File) {
        val root = _uiState.value.repoRoot ?: return
        _uiState.update {
            it.copy(
                currentDir = dir,
                entries = loadEntries(dir),
                isAtRoot = isSameDir(dir, root),
            )
        }
    }

    /** Returns true if navigated up, false if already at root. */
    fun navigateUp(): Boolean {
        val current = _uiState.value.currentDir ?: return false
        val root = _uiState.value.repoRoot ?: return false
        if (isSameDir(current, root)) return false
        val parent = current.parentFile ?: return false
        navigateInto(parent)
        return true
    }

    fun createFolder(name: String) {
        val dir = _uiState.value.currentDir ?: return
        viewModelScope.launch {
            File(dir, name.trim()).mkdirs()
            refresh()
        }
    }

    fun createMarkdownFile(name: String) {
        val dir = _uiState.value.currentDir ?: return
        viewModelScope.launch {
            val fileName = if (name.trim().endsWith(".md")) name.trim() else "${name.trim()}.md"
            File(dir, fileName).createNewFile()
            refresh()
        }
    }

    fun rename(entry: FileEntry, newName: String) {
        viewModelScope.launch {
            entry.file.renameTo(File(entry.file.parentFile, newName.trim()))
            refresh()
        }
    }

    fun delete(entry: FileEntry) {
        viewModelScope.launch {
            entry.file.deleteRecursively()
            refresh()
        }
    }

    private fun refresh() {
        val dir = _uiState.value.currentDir ?: return
        val root = _uiState.value.repoRoot ?: return
        _uiState.update {
            it.copy(
                entries = loadEntries(dir),
                isAtRoot = isSameDir(dir, root),
            )
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onNavigateToPosts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onOpenFile: (filePath: String) -> Unit,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Intercept system back: navigate up in directory tree before leaving screen
    BackHandler(enabled = !uiState.isAtRoot) {
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.currentDir?.name ?: stringResource(R.string.files),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (!uiState.isAtRoot) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPosts,
                    icon = { Icon(Icons.Outlined.Article, contentDescription = null) },
                    label = { Text(stringResource(R.string.posts)) },
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    label = { Text(stringResource(R.string.files)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                )
            }
        },
        floatingActionButton = {
            if (uiState.isRepoSetup) {
                CreateFab(
                    onCreateFolder = viewModel::createFolder,
                    onCreateMarkdown = viewModel::createMarkdownFile,
                )
            }
        },
    ) { padding ->
        when {
            !uiState.isRepoSetup -> {
                EmptyRepoPlaceholder(
                    onSetup = onNavigateToSetup,
                    modifier = Modifier.padding(padding),
                )
            }
            uiState.entries.isEmpty() -> {
                EmptyFolderPlaceholder(modifier = Modifier.padding(padding))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(uiState.entries, key = { it.file.absolutePath }) { entry ->
                        FileEntryRow(
                            entry = entry,
                            onOpen = {
                                if (entry.isDirectory) {
                                    viewModel.navigateInto(entry.file)
                                } else if (entry.name.endsWith(".md", ignoreCase = true)) {
                                    onOpenFile(entry.file.absolutePath)
                                }
                            },
                            onRename = { newName -> viewModel.rename(entry, newName) },
                            onDelete = { viewModel.delete(entry) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 56.dp),
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Entry Row ───────────────────────────────────────────────────────────────

private val plainTextExtensions = setOf("toml", "yaml", "yml", "html", "htm", "json", "xml", "txt", "ini", "conf")

@Composable
private fun FileEntryRow(
    entry: FileEntry,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTextViewer by remember { mutableStateOf(false) }

    val ext = entry.name.substringAfterLast('.', "").lowercase()
    val isMarkdown = ext == "md"
    val isPlainText = ext in plainTextExtensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    entry.isDirectory -> onOpen()
                    isMarkdown -> onOpen()
                    isPlainText -> showTextViewer = true
                    else -> {}
                }
            }
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when {
                entry.isDirectory -> Icons.Outlined.Folder
                isMarkdown -> Icons.Outlined.Article
                isPlainText -> Icons.Outlined.Description
                else -> Icons.Outlined.Image
            },
            contentDescription = null,
            tint = if (entry.isDirectory)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!entry.isDirectory) {
                Text(
                    entry.file.length().toHumanSize(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename)) },
                    onClick = { showMenu = false; showRenameDialog = true },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; showDeleteDialog = true },
                    leadingIcon = {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                    },
                )
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = entry.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName -> showRenameDialog = false; onRename(newName) },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_file_confirmation, entry.name)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showTextViewer) {
        val content = remember(entry.file.absolutePath) {
            runCatching { entry.file.readText() }.getOrDefault("(unable to read file)")
        }
        AlertDialog(
            onDismissRequest = { showTextViewer = false },
            title = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Text(
                    content,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            },
            confirmButton = {
                TextButton(onClick = { showTextViewer = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

// ─── FAB ─────────────────────────────────────────────────────────────────────

@Composable
private fun CreateFab(
    onCreateFolder: (String) -> Unit,
    onCreateMarkdown: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showFileDialog by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_folder)) },
                onClick = { expanded = false; showFolderDialog = true },
                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_file_md)) },
                onClick = { expanded = false; showFileDialog = true },
                leadingIcon = { Icon(Icons.Outlined.Article, null) },
            )
        }
    }

    if (showFolderDialog) {
        InputNameDialog(
            title = stringResource(R.string.new_folder),
            label = stringResource(R.string.folder_name),
            onDismiss = { showFolderDialog = false },
            onConfirm = { name -> showFolderDialog = false; onCreateFolder(name) },
        )
    }

    if (showFileDialog) {
        InputNameDialog(
            title = stringResource(R.string.new_file_md),
            label = stringResource(R.string.file_name_md),
            placeholder = "my-new-post",
            onDismiss = { showFileDialog = false },
            onConfirm = { name -> showFileDialog = false; onCreateMarkdown(name) },
        )
    }
}

// ─── Dialogs ─────────────────────────────────────────────────────────────────

@Composable
private fun InputNameDialog(
    title: String,
    label: String,
    placeholder: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                placeholder = if (placeholder.isNotBlank()) {
                    { Text(placeholder) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.rename_to)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank() && text.trim() != currentName,
            ) {
                Text(stringResource(R.string.rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ─── Placeholders ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyRepoPlaceholder(onSetup: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.CloudOff, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.no_repo_configured), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.connect_repo_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSetup) { Text(stringResource(R.string.set_up_repository)) }
        }
    }
}

@Composable
private fun EmptyFolderPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.FolderOpen, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.empty_folder), style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ─── Util ─────────────────────────────────────────────────────────────────────

private fun Long.toHumanSize(): String = when {
    this < 1_024L -> "$this B"
    this < 1_048_576L -> "${this / 1_024} KB"
    else -> "${this / 1_048_576} MB"
}
