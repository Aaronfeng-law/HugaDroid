package com.soogoino.huga.ui.posts

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soogoino.huga.R
import com.soogoino.huga.data.model.HugoPost
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
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
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: PostsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            LargeTopAppBar(
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
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.Article, contentDescription = null) },
                    label = { Text(stringResource(R.string.posts)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToFiles,
                    icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
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
            ExtendedFloatingActionButton(
                onClick = { showNewPostDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_post)) },
            )
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
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.filterOnlyDraft,
                        onClick = viewModel::toggleDraftFilter,
                        label = { Text(stringResource(R.string.drafts_only)) },
                        leadingIcon = if (uiState.filterOnlyDraft) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(posts, key = { it.filePath }) { post ->
                            PostCard(
                                post = post,
                                onClick = { onOpenPost(post.filePath) },
                                onDelete = { showDeleteDialog = post },
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (post.frontMatter.draft)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.frontMatter.draft) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.draft_chip), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 8.dp).height(24.dp),
                        )
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

                // Tags
                if (post.frontMatter.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(post.frontMatter.tags.take(4)) { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Date
                if (post.frontMatter.date.isNotBlank()) {
                    Text(
                        text = formatDate(post.frontMatter.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = { showMenu = false; onClick() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    )
                }
            }
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
        }
    }
}

private fun formatDate(dateStr: String): String = runCatching {
    val instant = Instant.parse(dateStr)
    val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}.getOrDefault(dateStr)
