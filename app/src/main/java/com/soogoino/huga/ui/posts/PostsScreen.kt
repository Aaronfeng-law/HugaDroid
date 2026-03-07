package com.soogoino.huga.ui.posts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soogoino.huga.R
import com.soogoino.huga.data.model.HugoPost
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostsScreen(
    onOpenPost: (filePath: String) -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: PostsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    // Isolate pinnedFilePaths so that changes to unrelated uiState fields
    // (aheadCount, isSyncing, …) don't cause all PostCards to recompose.
    val pinnedFilePaths by remember { derivedStateOf { uiState.pinnedFilePaths } }

    var showNewPostDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<HugoPost?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PostsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is PostsEvent.NavigateToEditor -> onOpenPost(event.filePath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.sort))
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.newest_first)) },
                                onClick = { viewModel.setSortOrder(SortOrder.DATE_DESC); showSortMenu = false },
                                leadingIcon = if (uiState.sortOrder == SortOrder.DATE_DESC) { { Icon(Icons.Filled.Check, null) } } else null,
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.oldest_first)) },
                                onClick = { viewModel.setSortOrder(SortOrder.DATE_ASC); showSortMenu = false },
                                leadingIcon = if (uiState.sortOrder == SortOrder.DATE_ASC) { { Icon(Icons.Filled.Check, null) } } else null,
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_a_z)) },
                                onClick = { viewModel.setSortOrder(SortOrder.TITLE_ASC); showSortMenu = false },
                                leadingIcon = if (uiState.sortOrder == SortOrder.TITLE_ASC) { { Icon(Icons.Filled.Check, null) } } else null,
                            )
                        }
                    }
                    // Search toggle
                    IconButton(onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            if (searchExpanded) Icons.Filled.Close else Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    // Sync status badge
                    BadgedBox(
                        badge = {
                            if (uiState.aheadCount > 0) {
                                Badge { Text(uiState.aheadCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = {
                            if (!uiState.isRepoSetup) onNavigateToSetup() else onNavigateToSync()
                        }) {
                            Icon(Icons.Outlined.Sync, contentDescription = stringResource(R.string.sync))
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    SmallFloatingActionButton(
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.scroll_to_top))
                    }
                }
                FloatingActionButton(
                    onClick = { showNewPostDialog = true },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_post))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            AnimatedVisibility(visible = searchExpanded) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text(stringResource(R.string.search_posts_placeholder)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Filled.Close, null) } }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // Sync progress bar
            AnimatedVisibility(visible = uiState.isSyncing) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    LinearProgressIndicator(
                        progress = { if (uiState.syncProgress > 0) uiState.syncProgress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.syncTask.isNotBlank()) {
                        Text(
                            uiState.syncTask,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterOnlyDraft,
                    onClick = viewModel::toggleDraftFilter,
                    label = { Text(stringResource(R.string.drafts_only)) },
                    leadingIcon = if (uiState.filterOnlyDraft) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }

            // Post list
            when {
                uiState.isLoading && posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                !uiState.isRepoSetup -> {
                    EmptySetupPlaceholder(onSetup = onNavigateToSetup)
                }
                posts.isEmpty() -> {
                    EmptyPostsPlaceholder(onCreate = { showNewPostDialog = true })
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = posts,
                            key = { it.filePath },
                            contentType = { "post" },
                        ) { post ->
                            PostCard(
                                post = post,
                                isPinned = post.filePath in pinnedFilePaths,
                                onClick = { onOpenPost(post.filePath) },
                                onDelete = { showDeleteDialog = post },
                                onTogglePin = { viewModel.togglePin(post.filePath) },
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                    }
                }
            }
        }
    }

    // New post dialog
    if (showNewPostDialog) {
        NewPostDialog(
            availableSections = uiState.availableSections,
            onDismiss = { showNewPostDialog = false },
            onConfirm = { slug, section ->
                showNewPostDialog = false
                viewModel.createPost(slug, section)
            }
        )
    }

    // Delete confirm dialog
    showDeleteDialog?.let { post ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_confirmation, post.frontMatter.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    viewModel.deletePost(post)
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCard(
    post: HugoPost,
    isPinned: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    // Compute card appearance values (composable fns handle their own memoization)
    val isDraft = post.frontMatter.draft
    val cardColors = CardDefaults.cardColors(
        containerColor = if (isDraft) MaterialTheme.colorScheme.surfaceVariant
                         else MaterialTheme.colorScheme.surface
    )
    val cardElevation = CardDefaults.cardElevation(
        defaultElevation = if (isPinned) 3.dp else 1.dp
    )
    // Only allocate border & color object when actually pinned
    val cardBorder = if (isPinned) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        elevation = cardElevation,
        border = cardBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(end = 2.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (post.frontMatter.draft) {
                        // Lightweight badge — avoids SuggestionChip's deep composable tree
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.draft_chip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    Text(
                        text = post.frontMatter.title.ifBlank { post.slug },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (post.frontMatter.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = post.frontMatter.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Tags — lightweight Box+Text badges (AssistChip has ~12 composable layers each)
                if (post.frontMatter.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        post.frontMatter.tags.take(4).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Date + reading time
                if (post.frontMatter.date.isNotBlank() || post.wordCount > 0) {
                    val minRead = remember(post.wordCount) {
                        (post.wordCount / 200.0).coerceAtLeast(1.0).toInt()
                    }
                    val dateFormatted = remember(post.frontMatter.date) { formatDate(post.frontMatter.date) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (post.frontMatter.date.isNotBlank()) {
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        if (post.wordCount > 0) {
                            Text(
                                text = stringResource(R.string.min_read, minRead),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            PostCardMenu(
                isPinned = isPinned,
                onEdit = onClick,
                onTogglePin = onTogglePin,
                onDelete = onDelete,
            )
        }
    }
}

/** Isolated composable so opening/closing the menu only recomposes this subtree, not the full card. */
@Composable
private fun PostCardMenu(
    isPinned: Boolean,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
            )
            DropdownMenuItem(
                text = { Text(if (isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin)) },
                onClick = { showMenu = false; onTogglePin() },
                leadingIcon = {
                    Icon(
                        if (isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        null,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPostDialog(
    availableSections: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (slug: String, section: String) -> Unit,
) {
    var slug by remember { mutableStateOf("") }
    val sections = availableSections.ifEmpty { listOf("posts") }
    var selectedSection by remember(sections) { mutableStateOf(sections.first()) }
    var sectionMenuExpanded by remember { mutableStateOf(false) }
    val isValid = slug.matches(Regex("[a-z0-9][a-z0-9-]*"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_post)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.new_post_slug_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "") },
                    label = { Text(stringResource(R.string.slug)) },
                    singleLine = true,
                    isError = slug.isNotEmpty() && !isValid,
                    supportingText = if (slug.isNotEmpty() && !isValid) {
                        { Text(stringResource(R.string.use_lowercase)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (sections.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = sectionMenuExpanded,
                        onExpandedChange = { sectionMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedSection,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.section)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = sectionMenuExpanded,
                            onDismissRequest = { sectionMenuExpanded = false },
                        ) {
                            sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(section) },
                                    onClick = { selectedSection = section; sectionMenuExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(slug, selectedSection) }, enabled = isValid) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EmptySetupPlaceholder(onSetup: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.CloudOff, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.no_repo_configured), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.connect_repo_body), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSetup) { Text(stringResource(R.string.set_up_repository)) }
        }
    }
}

@Composable
private fun EmptyPostsPlaceholder(onCreate: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.Article, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.no_posts_yet), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.create_first_post), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onCreate) { Text(stringResource(R.string.new_post)) }
        }
    }
}

private fun formatDate(dateStr: String): String = runCatching {
    val instant = Instant.parse(dateStr)
    val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}.getOrDefault(dateStr)
