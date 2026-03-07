package com.soogoino.huga.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.net.Uri
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.soogoino.huga.R
import com.soogoino.huga.data.model.HugoPost
import com.soogoino.huga.data.prefs.MediaStrategy
import com.soogoino.huga.ui.theme.EditorTextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    filePath: String,
    onNavigateUp: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Font size for source editor (zoom in/out), 14sp default, 10–28sp range
    var editorFontSizeSp by rememberSaveable { mutableFloatStateOf(14f) }

    // TextFieldValue to track cursor in Content tab
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // Sync bodyText → textFieldValue when it changes externally (initial load / image insert).
    // Cursor goes to end on first load; stays put on subsequent external updates.
    LaunchedEffect(uiState.bodyText) {
        if (textFieldValue.text != uiState.bodyText) {
            val selection = if (textFieldValue.text.isEmpty()) {
                TextRange(uiState.bodyText.length)
            } else {
                textFieldValue.selection
            }
            textFieldValue = TextFieldValue(text = uiState.bodyText, selection = selection)
        }
    }

    // Load post on first composition
    LaunchedEffect(filePath) { viewModel.loadPost(filePath) }

    // Force-save on lifecycle stop
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // When leaving STARTED, force-save
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.forceSave() } }

    // Camera capture URI
    var captureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // Pending URI waiting for permission
    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            captureUri?.let { uri ->
                viewModel.handleImageUri(uri, uiState.mediaInsertPosition)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission just granted — launch camera with the prepared URI
            pendingCaptureUri?.let { uri ->
                captureUri = uri
                pendingCaptureUri = null
                takePictureLauncher.launch(uri)
            }
        } else {
            pendingCaptureUri = null
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.handleImageUri(it, uiState.mediaInsertPosition) }
    }

    // One-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditorEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditorEvent.NavigateUp -> onNavigateUp()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.frontMatter.title.ifBlank { stringResource(R.string.untitled) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.forceSave(); onNavigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Save state indicator
                    when (uiState.saveState) {
                        SaveState.UNSAVED -> Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = stringResource(R.string.unsaved_changes),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp).padding(4.dp)
                        )
                        SaveState.SAVING -> CircularProgressIndicator(Modifier.size(16.dp).padding(4.dp), strokeWidth = 2.dp)
                        SaveState.SAVED -> Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.saved),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom in / zoom out (Content + Preview tabs)
                    if (uiState.editorTab == EditorTab.CONTENT || uiState.editorTab == EditorTab.PREVIEW) {
                        IconButton(
                            onClick = { editorFontSizeSp = (editorFontSizeSp - 2f).coerceAtLeast(10f) },
                            enabled = editorFontSizeSp > 10f,
                        ) {
                            Icon(Icons.Outlined.ZoomOut, contentDescription = stringResource(R.string.zoom_out))
                        }
                        IconButton(
                            onClick = { editorFontSizeSp = (editorFontSizeSp + 2f).coerceAtMost(28f) },
                            enabled = editorFontSizeSp < 28f,
                        ) {
                            Icon(Icons.Outlined.ZoomIn, contentDescription = stringResource(R.string.zoom_in))
                        }
                    }

                    // Insert image (only shown when in Content tab)
                    if (uiState.editorTab == EditorTab.CONTENT) {
                        IconButton(onClick = {
                            viewModel.onCursorPositionChanged(textFieldValue.selection.start)
                            viewModel.showMediaSheet()
                        }) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(R.string.insert_image))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Content | Front Matter | Preview tab bar
            val tabTitles = listOf(
                stringResource(R.string.editor_tab_content),
                stringResource(R.string.editor_tab_front_matter),
                stringResource(R.string.editor_tab_preview),
            )
            val selectedTab = when (uiState.editorTab) {
                EditorTab.CONTENT -> 0
                EditorTab.FRONT_MATTER -> 1
                EditorTab.PREVIEW -> 2
            }
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            viewModel.setEditorTab(
                                when (index) {
                                    0 -> EditorTab.CONTENT
                                    1 -> EditorTab.FRONT_MATTER
                                    else -> EditorTab.PREVIEW
                                }
                            )
                        },
                        text = { Text(title) },
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.editorTab == EditorTab.CONTENT -> {
                        SourceEditor(
                            value = textFieldValue,
                            onValueChange = { newVal ->
                                textFieldValue = newVal
                                viewModel.onBodyChanged(newVal.text)
                                viewModel.onCursorPositionChanged(newVal.selection.start)
                            },
                            fontSize = editorFontSizeSp,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    uiState.editorTab == EditorTab.FRONT_MATTER -> {
                        FrontMatterTab(
                            frontMatter = uiState.frontMatter,
                            onChanged = viewModel::onFrontMatterChanged,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        val previewBody = remember(
                            uiState.bodyText,
                            uiState.post,
                            uiState.mediaStrategy,
                            uiState.localRepoPath,
                        ) {
                            resolvePreviewImagePaths(
                                bodyText = uiState.bodyText,
                                post = uiState.post,
                                mediaStrategy = uiState.mediaStrategy,
                                localRepoPath = uiState.localRepoPath,
                            )
                        }
                        PreviewPane(
                            markdown = previewBody,
                            fontSize = editorFontSizeSp,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // Markdown formatting toolbar — visible only in Content tab, floats above keyboard
            if (uiState.editorTab == EditorTab.CONTENT && !uiState.isLoading) {
                MarkdownToolbar(
                    onAction = { action ->
                        textFieldValue = applyMarkdownAction(action, textFieldValue)
                        viewModel.onBodyChanged(textFieldValue.text)
                    },
                )
            }
        }
    }

    // Image name dialog (shown after pick/capture)
    uiState.pendingImage?.let { pending ->
        ImageNameDialog(
            pendingImage = pending,
            onConfirm = viewModel::commitImageInsert,
            onDismiss = viewModel::dismissImageDialog,
        )
    }

    // Media insert bottom sheet
    if (uiState.showMediaSheet) {
        MediaInsertSheet(
            onDismiss = viewModel::hideMediaSheet,
            onTakePhoto = {
                viewModel.hideMediaSheet()
                val photoFile = File(context.cacheDir, "camera/photo_${System.currentTimeMillis()}.jpg").also {
                    it.parentFile?.mkdirs()
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                val cameraPermission = android.Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, cameraPermission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    captureUri = uri
                    takePictureLauncher.launch(uri)
                } else {
                    pendingCaptureUri = uri
                    cameraPermissionLauncher.launch(cameraPermission)
                }
            },
            onPickGallery = {
                viewModel.hideMediaSheet()
                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

/**
 * Rewrites relative/web-root image paths in [bodyText] to absolute `file://` URIs
 * so that Coil (used by RichText) can load locally-saved images in the preview.
 *
 * - Bare filename `image.jpg`  → `file://{bundleDir}/image.jpg`  (PAGE_BUNDLE)
 * - Web-root `/images/…`       → `file://{localRepoPath}/static/images/…`  (STATIC_FOLDER)
 * - Already-absolute paths (http/https/file) are left unchanged.
 */
private fun resolvePreviewImagePaths(
    bodyText: String,
    post: HugoPost?,
    mediaStrategy: MediaStrategy,
    localRepoPath: String,
): String {
    if (post == null) return bodyText
    return bodyText.replace(Regex("""!\[([^\]]*)\]\(([^)]+)\)""")) { match ->
        val alt = match.groupValues[1]
        val path = match.groupValues[2]
        val resolved = when {
            path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://") -> path
            path.startsWith("/") -> {
                // Web-root reference from STATIC_FOLDER strategy: /images/slug/file.jpg
                "file://$localRepoPath/static$path"
            }
            else -> {
                // Bare filename from PAGE_BUNDLE strategy: image.jpg
                "file://${post.bundleDir}/$path"
            }
        }
        "![$alt]($resolved)"
    }
}

@Composable
private fun SourceEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSize: Float = 14f,
    modifier: Modifier = Modifier,
) {
    val customSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            textStyle = EditorTextStyle.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6f).sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = remember { MarkdownVisualTransformation() },
            decorationBox = { innerTextField ->
                if (value.text.isEmpty()) {
                    Text(
                        stringResource(R.string.start_writing_placeholder),
                        style = EditorTextStyle.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.6f).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                innerTextField()
            },
        )
    }
}

// ── Markdown preview segment model ──────────────────────────────────────────

private sealed class MdSegment {
    data class Text(val content: String) : MdSegment()
    data class Image(val alt: String, val url: String) : MdSegment()
}

/**
 * Splits a markdown string into alternating [MdSegment.Text] and [MdSegment.Image] segments
 * so the preview can render images using Coil's AsyncImage instead of AnnotatedString
 * (which cannot embed bitmaps).
 */
private fun parseMarkdownSegments(markdown: String): List<MdSegment> {
    val imageRegex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
    val result = mutableListOf<MdSegment>()
    var cursor = 0
    for (match in imageRegex.findAll(markdown)) {
        val textBefore = markdown.substring(cursor, match.range.first)
        if (textBefore.isNotBlank()) result += MdSegment.Text(textBefore)
        result += MdSegment.Image(alt = match.groupValues[1], url = match.groupValues[2])
        cursor = match.range.last + 1
    }
    val tail = markdown.substring(cursor)
    if (tail.isNotBlank()) result += MdSegment.Text(tail)
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageNameDialog(
    pendingImage: PendingImageData,
    onConfirm: (filename: String, altText: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var filename by remember { mutableStateOf(pendingImage.suggestedFilename.removeSuffix(".jpg")) }
    var altText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_insert_image_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Thumbnail preview via Coil (supports ByteArray directly)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(pendingImage.bytes)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
                // File name
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text(stringResource(R.string.image_file_name)) },
                    suffix = { Text(".jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Alt text
                OutlinedTextField(
                    value = altText,
                    onValueChange = { altText = it },
                    label = { Text(stringResource(R.string.image_alt_text)) },
                    placeholder = { Text(stringResource(R.string.image_alt_text_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val safe = filename.trim().ifBlank { pendingImage.suggestedFilename.removeSuffix(".jpg") }
                onConfirm(safe, altText.trim())
            }) { Text(stringResource(R.string.insert)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PreviewPane(markdown: String, fontSize: Float = 14f, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // markdown is already body-only (no front matter), so render directly
    val segments = remember(markdown) { parseMarkdownSegments(markdown) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEach { segment ->
            key(segment) {
                when (segment) {
                    is MdSegment.Text -> {
                        val richState = rememberRichTextState()
                        LaunchedEffect(segment.content) { richState.setMarkdown(segment.content) }
                        RichText(
                            state = richState,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.6f).sp,
                            ),
                        )
                    }
                    is MdSegment.Image -> {
                        // file:// → java.io.File (more reliable in Coil 3 than Uri)
                        val coilModel = remember(segment.url) {
                            if (segment.url.startsWith("file://")) {
                                val path = Uri.parse(segment.url).path ?: segment.url
                                java.io.File(path)
                            } else {
                                segment.url
                            }
                        }
                        // Pre-check file existence to give immediate feedback
                        val fileExists = remember(coilModel) {
                            if (coilModel is java.io.File) coilModel.exists() else true
                        }
                        var loadError by remember(segment.url) { mutableStateOf(!fileExists) }
                        if (loadError) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small,
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = stringResource(R.string.image_not_found, segment.url),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(coilModel)
                                    .build(),
                                contentDescription = segment.alt.ifBlank { null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                contentScale = ContentScale.FillWidth,
                                onState = { state ->
                                    if (state is AsyncImagePainter.State.Error) {
                                        loadError = true
                                    }
                                },
                            )
                        }
                        if (segment.alt.isNotBlank() && !loadError) {
                            Text(
                                text = segment.alt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
