package com.soogoino.huga.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.data.model.FrontMatterFormat
import com.soogoino.huga.data.model.FrontMatterParser
import com.soogoino.huga.data.model.HugoFrontMatter
import com.soogoino.huga.data.model.HugoPost
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.data.prefs.MediaStrategy
import com.soogoino.huga.domain.ReadPostUseCase
import com.soogoino.huga.domain.SavePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

enum class EditorTab { CONTENT, FRONT_MATTER, PREVIEW }
enum class SaveState { SAVED, UNSAVED, SAVING }

/** Holds a compressed image awaiting filename/alt confirmation from the user. */
data class PendingImageData(
    val bytes: ByteArray,
    val suggestedFilename: String,
)

data class EditorUiState(
    val post: HugoPost? = null,
    val frontMatter: HugoFrontMatter = HugoFrontMatter(),
    val frontMatterFormat: FrontMatterFormat = FrontMatterFormat.YAML,
    val bodyText: String = "",
    val editorTab: EditorTab = EditorTab.CONTENT,
    val saveState: SaveState = SaveState.SAVED,
    val isLoading: Boolean = true,
    val error: String? = null,
    val mediaInsertPosition: Int = 0,
    val mediaStrategy: MediaStrategy = MediaStrategy.PAGE_BUNDLE,
    val localRepoPath: String = "",
    val showMediaSheet: Boolean = false,
    val pendingImage: PendingImageData? = null,
)

sealed class EditorEvent {
    data class ShowSnackbar(val message: String) : EditorEvent()
    object NavigateUp : EditorEvent()
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readPostUseCase: ReadPostUseCase,
    private val savePostUseCase: SavePostUseCase,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    // Debounce auto-save
    private val _contentFlow = MutableStateFlow("")
    private var filePath: String = ""

    init {
        // Collect settings so PreviewPane can resolve local image paths
        viewModelScope.launch {
            prefs.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        mediaStrategy = settings.mediaStrategy,
                        localRepoPath = settings.localRepoPath,
                    )
                }
            }
        }

        viewModelScope.launch {
            _contentFlow
                .debounce(500L)
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collectLatest { _ ->
                    val s = _uiState.value
                    val orig = s.post ?: return@collectLatest
                    if (filePath.isNotBlank() && s.saveState == SaveState.UNSAVED) {
                        _uiState.update { it.copy(saveState = SaveState.SAVING) }
                        savePostUseCase(
                            orig.copy(
                                frontMatter = s.frontMatter,
                                frontMatterFormat = s.frontMatterFormat,
                                bodyMarkdown = s.bodyText,
                            )
                        )
                        _uiState.update { it.copy(saveState = SaveState.SAVED) }
                    }
                }
        }
    }

    fun loadPost(path: String) {
        filePath = path
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val post = readPostUseCase(path)
            if (post != null) {
                // Parse body verbatim from disk to avoid any SnakeYAML round-trip mutation.
                val rawBody = withContext(Dispatchers.IO) {
                    runCatching {
                        val raw = File(path).readText()
                        val bodyOffset = FrontMatterParser.bodyStartOffset(raw)
                        FrontMatterParser.stripLeadingFrontMatter(raw.substring(bodyOffset))
                    }.getOrElse { post.bodyMarkdown }
                }
                _uiState.update {
                    it.copy(
                        post = post,
                        frontMatter = post.frontMatter,
                        frontMatterFormat = post.frontMatterFormat,
                        bodyText = rawBody,
                        isLoading = false,
                        saveState = SaveState.SAVED,
                    )
                }
                _contentFlow.value = serialiseCurrent()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Could not load post") }
            }
        }
    }

    /** Called whenever the user edits the body text in the Content tab. */
    fun onBodyChanged(newBody: String) {
        _uiState.update { it.copy(bodyText = newBody, saveState = SaveState.UNSAVED) }
        _contentFlow.value = serialiseCurrent()
    }

    /** Called from FrontMatterTab on any field change. Triggers auto-save. */
    fun onFrontMatterChanged(fm: HugoFrontMatter) {
        _uiState.update { it.copy(frontMatter = fm, saveState = SaveState.UNSAVED) }
        _contentFlow.value = serialiseCurrent()
    }

    fun onCursorPositionChanged(pos: Int) {
        _uiState.update { it.copy(mediaInsertPosition = pos) }
    }

    fun setEditorTab(tab: EditorTab) {
        _uiState.update { it.copy(editorTab = tab) }
    }

    fun showMediaSheet() { _uiState.update { it.copy(showMediaSheet = true) } }
    fun hideMediaSheet() { _uiState.update { it.copy(showMediaSheet = false) } }

    /** Force-save immediately (called on pause/stop). */
    fun forceSave() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _uiState.value
            val orig = s.post ?: return@launch
            if (filePath.isNotBlank()) {
                savePostUseCase(
                    orig.copy(
                        frontMatter = s.frontMatter,
                        frontMatterFormat = s.frontMatterFormat,
                        bodyMarkdown = s.bodyText,
                    )
                )
                _uiState.update { it.copy(saveState = SaveState.SAVED) }
            }
        }
    }

    /** Insert image from URI into the post at cursor position.
     *  Compresses the image then shows a dialog for filename + alt text input. */
    fun handleImageUri(uri: Uri, insertAt: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val compressed = compressImage(uri)
                val suggested = "image_${System.currentTimeMillis()}.jpg"
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(
                        pendingImage = PendingImageData(compressed, suggested),
                        showMediaSheet = false,
                    ) }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    _events.emit(EditorEvent.ShowSnackbar("Image load failed: ${e.message}"))
                }
            }
        }
    }

    /** Called after user confirms filename + alt text in the dialog. */
    fun commitImageInsert(filename: String, altText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val settings = prefs.settings.first()
                val post = _uiState.value.post ?: return@runCatching
                val pending = _uiState.value.pendingImage ?: return@runCatching
                val insertAt = _uiState.value.mediaInsertPosition

                val safeFilename = filename.trim()
                    .ifBlank { pending.suggestedFilename.removeSuffix(".jpg") }
                    .let { if (it.endsWith(".jpg", ignoreCase = true)) it else "$it.jpg" }

                val destFile = when (settings.mediaStrategy) {
                    MediaStrategy.PAGE_BUNDLE -> {
                        val bundleDir = File(post.filePath).parentFile
                            ?: return@runCatching  // STB-02: graceful null guard
                        File(bundleDir, safeFilename)
                    }
                    MediaStrategy.STATIC_FOLDER -> {
                        val staticDir = File("${settings.localRepoPath}/static/images/${post.slug}").also { it.mkdirs() }
                        File(staticDir, safeFilename)
                    }
                }
                destFile.writeBytes(pending.bytes)

                val mdRef = when (settings.mediaStrategy) {
                    MediaStrategy.PAGE_BUNDLE -> "![$altText]($safeFilename)"
                    MediaStrategy.STATIC_FOLDER -> "![$altText](/images/${post.slug}/$safeFilename)"
                }

                // Insert directly into bodyText — no FM offset clamping needed
                val body = _uiState.value.bodyText
                val safeInsertAt = insertAt.coerceIn(0, body.length)
                val newBody = "${body.substring(0, safeInsertAt)}\n$mdRef\n${body.substring(safeInsertAt)}"

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(bodyText = newBody, pendingImage = null, saveState = SaveState.UNSAVED) }
                    _contentFlow.value = serialiseCurrent()
                    _events.emit(EditorEvent.ShowSnackbar("Image inserted: $safeFilename"))
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(pendingImage = null) }
                    _events.emit(EditorEvent.ShowSnackbar("Image insert failed: ${e.message}"))
                }
            }
        }
    }

    fun dismissImageDialog() {
        _uiState.update { it.copy(pendingImage = null) }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun serialiseCurrent(): String {
        val s = _uiState.value
        return FrontMatterParser.serialise(s.frontMatter, s.frontMatterFormat, s.bodyText)
    }

    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open image: URI is invalid or file was deleted")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val maxDim = 1920
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        return ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }.toByteArray()
    }

    override fun onCleared() {
        super.onCleared()
        // SEC-09: Clean up camera temp files accumulated in cacheDir/camera/
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            runCatching { context.cacheDir.resolve("camera").deleteRecursively() }
        }
        // viewModelScope is already cancelled at this point — use an independent scope
        // so the last edit is guaranteed to be flushed to disk.
        val saveScope = CoroutineScope(NonCancellable + Dispatchers.IO)
        val s = _uiState.value
        val orig = s.post ?: return
        if (filePath.isNotBlank()) {
            saveScope.launch {
                runCatching {
                    savePostUseCase(
                        orig.copy(
                            frontMatter = s.frontMatter,
                            frontMatterFormat = s.frontMatterFormat,
                            bodyMarkdown = s.bodyText,
                        )
                    )
                }
            }
        }
    }
}
