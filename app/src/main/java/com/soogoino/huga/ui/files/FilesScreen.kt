package com.soogoino.huga.ui.files

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.R
import com.soogoino.huga.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    val clipboardCount: Int = 0,
)

data class ConvertBatchResult(
    val convertedOriginals: List<File> = emptyList(),
    val skippedCount: Int = 0,
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()
    private val clipboardPaths = mutableListOf<String>()

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
            ?.filter { !(it.isDirectory && it.name.startsWith('.')) }
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

    fun copyToClipboard(files: List<File>) {
        clipboardPaths.clear()
        clipboardPaths.addAll(files.map { it.absolutePath }.distinct())
        _uiState.update { it.copy(clipboardCount = clipboardPaths.size) }
    }

    fun clearClipboard() {
        clipboardPaths.clear()
        _uiState.update { it.copy(clipboardCount = 0) }
    }

    suspend fun pasteClipboard() = withContext(Dispatchers.IO) {
        val targetDir = _uiState.value.currentDir ?: return@withContext
        if (clipboardPaths.isEmpty()) return@withContext

        clipboardPaths.forEach { sourcePath ->
            val source = File(sourcePath)
            if (!source.exists()) return@forEach

            val target = nonConflictTarget(targetDir, source.name)
            runCatching {
                if (source.isDirectory) {
                    source.copyRecursively(target, overwrite = false)
                } else {
                    source.copyTo(target, overwrite = false)
                }
            }
        }

        withContext(Dispatchers.Main) { refresh() }
    }

    suspend fun convertImagesToWebp(files: List<File>): ConvertBatchResult = withContext(Dispatchers.IO) {
        val convertedOriginals = mutableListOf<File>()
        var skippedCount = 0

        files.distinctBy { it.absolutePath }.forEach { source ->
            if (!source.exists() || source.isDirectory || !isConvertibleImageFile(source)) {
                skippedCount++
                return@forEach
            }

            val target = File(source.parentFile, "${source.nameWithoutExtension}.webp")
            if (target.exists()) {
                skippedCount++
                return@forEach
            }

            val bitmap = runCatching { BitmapFactory.decodeFile(source.absolutePath) }.getOrNull()
            if (bitmap == null) {
                skippedCount++
                return@forEach
            }

            val success = runCatching {
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                }
            }.getOrDefault(false)

            bitmap.recycle()

            if (success) {
                convertedOriginals += source
            } else {
                target.delete()
                skippedCount++
            }
        }

        withContext(Dispatchers.Main) { refresh() }
        ConvertBatchResult(convertedOriginals = convertedOriginals, skippedCount = skippedCount)
    }

    suspend fun deleteFiles(files: List<File>) = withContext(Dispatchers.IO) {
        files.distinctBy { it.absolutePath }.forEach { file ->
            runCatching { file.deleteRecursively() }
        }
        withContext(Dispatchers.Main) { refresh() }
    }

    private fun refresh() {
        val dir = _uiState.value.currentDir ?: return
        val root = _uiState.value.repoRoot ?: return
        _uiState.update {
            it.copy(
                entries = loadEntries(dir),
                isAtRoot = isSameDir(dir, root),
                clipboardCount = clipboardPaths.size,
            )
        }
    }

    private fun nonConflictTarget(parent: File, name: String): File {
        var candidate = File(parent, name)
        if (!candidate.exists()) return candidate

        val dot = name.lastIndexOf('.')
        val hasExt = dot > 0 && dot < name.length - 1
        val base = if (hasExt) name.substring(0, dot) else name
        val ext = if (hasExt) name.substring(dot) else ""

        var index = 1
        while (candidate.exists()) {
            candidate = File(parent, "$base ($index)$ext")
            index++
        }
        return candidate
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onOpenFile: (filePath: String) -> Unit,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedPaths = remember { mutableStateListOf<String>() }
    var showConvertConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showDeleteOriginalsDialog by remember { mutableStateOf(false) }
    var lastConvertedOriginals by remember { mutableStateOf<List<File>>(emptyList()) }
    var lastSkippedCount by remember { mutableIntStateOf(0) }
    var showNormalActionsMenu by remember { mutableStateOf(false) }
    var showSelectionActionsMenu by remember { mutableStateOf(false) }

    val selectedEntries = remember(uiState.entries, selectedPaths.toList()) {
        uiState.entries.filter { selectedPaths.contains(it.file.absolutePath) }
    }

    val selectedConvertibleEntries = remember(selectedEntries) {
        selectedEntries.filter { isConvertibleImageFile(it.file) }
    }

    val allSelected = remember(uiState.entries, selectedPaths.toList()) {
        uiState.entries.isNotEmpty() && selectedPaths.size == uiState.entries.size
    }

    LaunchedEffect(uiState.entries) {
        val valid = uiState.entries.mapTo(mutableSetOf()) { it.file.absolutePath }
        selectedPaths.removeAll { it !in valid }
        if (selectedPaths.isEmpty() && selectionMode) {
            selectionMode = false
        }
    }

    fun toggleSelection(entry: FileEntry) {
        val path = entry.file.absolutePath
        if (selectedPaths.contains(path)) {
            selectedPaths.remove(path)
        } else {
            selectedPaths.add(path)
        }
    }

    fun enterSelectionWith(entry: FileEntry) {
        if (!selectionMode) {
            selectionMode = true
            selectedPaths.clear()
        }
        toggleSelection(entry)
    }

    // Intercept system back: navigate up in directory tree before leaving screen
    BackHandler(enabled = selectionMode || !uiState.isAtRoot) {
        if (selectionMode) {
            selectionMode = false
            selectedPaths.clear()
        } else {
            viewModel.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) {
                            stringResource(R.string.selected_count, selectedPaths.size)
                        } else {
                            uiState.currentDir?.name ?: stringResource(R.string.files)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedPaths.clear()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    } else if (!uiState.isAtRoot) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        Box {
                            IconButton(onClick = { showSelectionActionsMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            DropdownMenu(
                                expanded = showSelectionActionsMenu,
                                onDismissRequest = { showSelectionActionsMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (allSelected) {
                                                stringResource(R.string.clear_selection)
                                            } else {
                                                stringResource(R.string.select_all)
                                            }
                                        )
                                    },
                                    onClick = {
                                        showSelectionActionsMenu = false
                                        if (allSelected) {
                                            selectedPaths.clear()
                                        } else {
                                            selectedPaths.clear()
                                            selectedPaths.addAll(uiState.entries.map { it.file.absolutePath })
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.DoneAll, null) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy)) },
                                    onClick = {
                                        showSelectionActionsMenu = false
                                        viewModel.copyToClipboard(selectedEntries.map { it.file })
                                        selectionMode = false
                                        selectedPaths.clear()
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                    enabled = selectedEntries.isNotEmpty(),
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.delete),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showSelectionActionsMenu = false
                                        showDeleteSelectedDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    enabled = selectedEntries.isNotEmpty(),
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.convert_to_webp)) },
                                    onClick = {
                                        showSelectionActionsMenu = false
                                        showConvertConfirmDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Image, null) },
                                    enabled = selectedConvertibleEntries.isNotEmpty(),
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                        }
                        Box {
                            IconButton(onClick = { showNormalActionsMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            DropdownMenu(
                                expanded = showNormalActionsMenu,
                                onDismissRequest = { showNormalActionsMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.select)) },
                                    onClick = {
                                        showNormalActionsMenu = false
                                        selectionMode = true
                                        selectedPaths.clear()
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Checklist, null) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.paste)) },
                                    onClick = {
                                        showNormalActionsMenu = false
                                        scope.launch { viewModel.pasteClipboard() }
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.ContentPaste, null) },
                                    enabled = uiState.clipboardCount > 0,
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.isRepoSetup && !selectionMode) {
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
                            selectionMode = selectionMode,
                            isSelected = selectedPaths.contains(entry.file.absolutePath),
                            onToggleSelection = { toggleSelection(entry) },
                            onLongPressSelect = { enterSelectionWith(entry) },
                            onConvertToWebP = {
                                selectedPaths.clear()
                                selectedPaths.add(entry.file.absolutePath)
                                showConvertConfirmDialog = true
                            },
                            onOpen = {
                                if (selectionMode) {
                                    toggleSelection(entry)
                                    return@FileEntryRow
                                }
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

    if (showConvertConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConvertConfirmDialog = false },
            title = { Text(stringResource(R.string.convert_to_webp)) },
            text = {
                Text(
                    stringResource(
                        R.string.convert_selected_confirmation,
                        selectedConvertibleEntries.size,
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConvertConfirmDialog = false
                        scope.launch {
                            val result = viewModel.convertImagesToWebp(selectedConvertibleEntries.map { it.file })
                            lastConvertedOriginals = result.convertedOriginals
                            lastSkippedCount = result.skippedCount
                            if (result.convertedOriginals.isNotEmpty()) {
                                showDeleteOriginalsDialog = true
                            } else {
                                selectionMode = false
                                selectedPaths.clear()
                            }
                        }
                    },
                    enabled = selectedConvertibleEntries.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.convert_to_webp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_selected_confirmation,
                        selectedEntries.size,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSelectedDialog = false
                    scope.launch {
                        viewModel.deleteFiles(selectedEntries.map { it.file })
                        selectionMode = false
                        selectedPaths.clear()
                    }
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteOriginalsDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteOriginalsDialog = false
                selectionMode = false
                selectedPaths.clear()
            },
            title = { Text(stringResource(R.string.delete_original_title)) },
            text = {
                Text(
                    if (lastSkippedCount > 0) {
                        stringResource(
                            R.string.delete_selected_originals_and_skipped,
                            lastConvertedOriginals.size,
                            lastSkippedCount,
                        )
                    } else {
                        stringResource(
                            R.string.delete_selected_originals_confirmation,
                            lastConvertedOriginals.size,
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteOriginalsDialog = false
                    scope.launch {
                        viewModel.deleteFiles(lastConvertedOriginals)
                        selectionMode = false
                        selectedPaths.clear()
                    }
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteOriginalsDialog = false
                    selectionMode = false
                    selectedPaths.clear()
                }) {
                    Text(stringResource(R.string.keep))
                }
            },
        )
    }
}

// ─── Entry Row ───────────────────────────────────────────────────────────────

private val plainTextExtensions = setOf("toml", "yaml", "yml", "html", "htm", "json", "xml", "txt", "ini", "conf", "svg")
private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private val webpConvertibleExtensions = setOf("jpg", "jpeg", "png", "bmp")

private fun isConvertibleImageFile(file: File): Boolean {
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return ext in webpConvertibleExtensions
}

@Composable
private fun FileEntryRow(
    entry: FileEntry,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPressSelect: () -> Unit,
    onConvertToWebP: () -> Unit,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTextViewer by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val ext = entry.name.substringAfterLast('.', "").lowercase()
    val isMarkdown = ext == "md"
    val isPlainText = ext in plainTextExtensions
    val isImage = ext in imageExtensions
    val isConvertibleImage = isConvertibleImageFile(entry.file)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelection()
                        return@combinedClickable
                    }
                    when {
                        entry.isDirectory -> onOpen()
                        isMarkdown -> onOpen()
                        isPlainText -> showTextViewer = true
                        isImage -> showImagePreview = true
                        else -> openWithExternalApp(context, entry.file)
                    }
                },
                onLongClick = {
                    if (!selectionMode) onLongPressSelect()
                }
            )
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when {
                entry.isDirectory -> Icons.Outlined.Folder
                isMarkdown -> Icons.Outlined.Article
                isPlainText -> Icons.Outlined.Description
                isImage -> Icons.Outlined.Image
                else -> Icons.Outlined.AttachFile
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

        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!entry.isDirectory && isConvertibleImage) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.convert_to_webp)) },
                        onClick = { showMenu = false; onConvertToWebP() },
                        leadingIcon = { Icon(Icons.Outlined.Image, null) },
                    )
                }
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
                Box(
                    Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTextViewer = false
                    openWithExternalApp(context, entry.file)
                }) {
                    Text(stringResource(R.string.open_with))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextViewer = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showImagePreview) {
        ImagePreviewDialog(
            file = entry.file,
            onDismiss = { showImagePreview = false },
            onOpenWith = {
                showImagePreview = false
                openWithExternalApp(context, entry.file)
            },
        )
    }
}

// ─── Image Preview Dialog ────────────────────────────────────────────────────

@Composable
private fun ImagePreviewDialog(
    file: File,
    onDismiss: () -> Unit,
    onOpenWith: () -> Unit,
) {
    val bitmap = remember(file.absolutePath) {
        runCatching {
            // Two-pass decode: measure first, then subsample to avoid OOM
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            opts.inSampleSize = calculateInSampleSize(opts, 1024, 1024)
            opts.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = file.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text("Unable to load image: ${file.name}")
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenWith) {
                Text(stringResource(R.string.open_with))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

// ─── External App Launcher ───────────────────────────────────────────────────

private fun openWithExternalApp(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = getMimeType(file.name)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

private fun getMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
}

private fun calculateInSampleSize(opts: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
    val (h, w) = opts.outHeight to opts.outWidth
    var inSampleSize = 1
    if (h > reqH || w > reqW) {
        val halfH = h / 2; val halfW = w / 2
        while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) inSampleSize *= 2
    }
    return inSampleSize
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
    this < 1_048_576L -> "%.1f KB".format(this / 1_024.0)
    else -> "%.1f MB".format(this / 1_048_576.0)
}
