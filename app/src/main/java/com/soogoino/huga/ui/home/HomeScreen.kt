package com.soogoino.huga.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.soogoino.huga.data.model.HugoPost
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.domain.ObservePostsUseCase
import com.soogoino.huga.domain.ScanPostsUseCase
import com.soogoino.huga.domain.SyncRepoUseCase
import com.soogoino.huga.git.GitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// ─── UiState ─────────────────────────────────────────────────────────────────

data class HomeUiState(
    val publishedCount: Int = 0,
    val draftCount: Int = 0,
    val writingDays: Int = 0,
    val uniqueTags: Int = 0,
    val uniqueCategories: Int = 0,
    val totalWords: Int = 0,
    val draftPosts: List<HugoPost> = emptyList(),
    val recentPosts: List<HugoPost> = emptyList(),
    val aheadCount: Int = 0,
    val isRepoSetup: Boolean = false,
    val isSyncing: Boolean = false,
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observePostsUseCase: ObservePostsUseCase,
    private val scanPostsUseCase: ScanPostsUseCase,
    private val syncRepoUseCase: SyncRepoUseCase,
    private val gitRepository: GitRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe setup status & ahead count
        viewModelScope.launch {
            prefs.settings.collect { s ->
                _uiState.update { it.copy(isRepoSetup = s.isRepoSetup) }
                if (s.isRepoSetup && s.localRepoPath.isNotBlank()) {
                    val ahead = runCatching { gitRepository.aheadCount(s.localRepoPath) }.getOrDefault(0)
                    _uiState.update { it.copy(aheadCount = ahead) }
                }
            }
        }
        // Observe Room post cache and compute stats
        viewModelScope.launch {
            observePostsUseCase().collect { posts ->
                val published = posts.filter { !it.frontMatter.draft }
                val drafts = posts.filter { it.frontMatter.draft }
                val writingDays = posts
                    .mapNotNull { runCatching { LocalDate.parse(it.frontMatter.date.take(10)) }.getOrNull() }
                    .toSet()
                    .size
                val tags = posts.flatMap { it.frontMatter.tags }.toSet().size
                val cats = posts.flatMap { it.frontMatter.categories }.toSet().size
                val totalWords = posts.sumOf { p -> p.wordCount }  // use pre-computed value
                val recentPosts = published
                    .sortedByDescending { p ->
                        runCatching { LocalDate.parse(p.frontMatter.date.take(10)) }.getOrNull()
                    }
                    .take(3)
                _uiState.update {
                    it.copy(
                        publishedCount = published.size,
                        draftCount = drafts.size,
                        writingDays = writingDays,
                        uniqueTags = tags,
                        uniqueCategories = cats,
                        totalWords = totalWords,
                        draftPosts = drafts,
                        recentPosts = recentPosts,
                    )
                }
            }
        }
        // Startup guard: if Room is empty but repo is set up, scan now.
        // Covers cold-start after a previous clone (without re-cloning).
        viewModelScope.launch {
            val s = prefs.settings.first()
            if (s.isRepoSetup && s.localRepoPath.isNotBlank()) {
                val hasCached = observePostsUseCase().first().isNotEmpty()
                if (!hasCached) runCatching { scanPostsUseCase() }
            }
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            syncRepoUseCase(onProgress = { _, _ -> })
            _uiState.update { it.copy(isSyncing = false) }
            val settings = prefs.settings.first()
            if (settings.localRepoPath.isNotBlank()) {
                val ahead = runCatching { gitRepository.aheadCount(settings.localRepoPath) }.getOrDefault(0)
                _uiState.update { it.copy(aheadCount = ahead) }
            }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onOpenPost: (filePath: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
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
    ) { paddingValues ->
        if (!uiState.isRepoSetup) {
            HomeSetupPlaceholder(
                onSetup = onNavigateToSetup,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Stats grid
                item {
                    Text(
                        stringResource(R.string.dashboard),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    StatGrid(uiState)
                }

                // Recent Posts section
                if (uiState.recentPosts.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recent_posts),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(uiState.recentPosts, key = { "recent_${it.filePath}" }) { post ->
                        RecentPostCard(post = post, onClick = { onOpenPost(post.filePath) })
                    }
                }

                // Working Papers header
                item {
                    Text(
                        stringResource(R.string.working_papers),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }

                if (uiState.draftPosts.isEmpty()) {
                    item {
                        NoDraftsCard()
                    }
                } else {
                    items(uiState.draftPosts, key = { it.filePath }) { post ->
                        DraftCard(post = post, onClick = { onOpenPost(post.filePath) })
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ─── Stat Grid ───────────────────────────────────────────────────────────────

@Composable
private fun StatGrid(uiState: HomeUiState) {
    val totalWordsLabel = when {
        uiState.totalWords >= 1_000_000 -> "%.1fM".format(uiState.totalWords / 1_000_000f)
        uiState.totalWords >= 1_000     -> "%.1fK".format(uiState.totalWords / 1_000f)
        else -> uiState.totalWords.toString()
    }
    // Single unified surface, divided into 4 equal cells by thin lines — no gap between cards.
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // ── Row 1: Posts  |  Tags & Categories ──────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                // Cell 1 — Published + Draft
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.posts),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.publishedCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.published),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.draftCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = stringResource(R.string.drafts_count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                // Cell 2 — Tags + Categories
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.taxonomy),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.uniqueTags.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.unique_tags),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.uniqueCategories.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = stringResource(R.string.unique_categories),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            // ── Row 2: Total Words  |  Writing Days ─────────────────────
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                // Cell 3 — Total Words
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                ) {
                    Text(
                        text = totalWordsLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.total_words),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                // Cell 4 — Writing Days
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                ) {
                    Text(
                        text = uiState.writingDays.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.writing_days),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Recent Post Card ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentPostCard(post: HugoPost, onClick: () -> Unit) {
    val minRead = remember(post.wordCount) { (post.wordCount / 200.0).coerceAtLeast(1.0).toInt() }
    val dateStr = post.frontMatter.date.take(10).takeIf { it.length == 10 }?.let {
        runCatching {
            val d = LocalDate.parse(it)
            d.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
        }.getOrDefault(it)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.frontMatter.title.ifBlank { post.slug },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (post.frontMatter.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = post.frontMatter.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dateStr != null) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Text(
                        text = stringResource(R.string.min_read, minRead),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            SuggestionChip(
                onClick = {},
                label = { Text(stringResource(R.string.published), style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = null,
            )
        }
    }
}

// ─── Draft Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftCard(post: HugoPost, onClick: () -> Unit) {
    val wordCount = post.wordCount
    val minRead = remember(post.wordCount) { (post.wordCount / 200.0).coerceAtLeast(1.0).toInt() }
    val dateStr = post.frontMatter.date.take(10).takeIf { it.length == 10 }?.let {
        runCatching {
            val d = LocalDate.parse(it)
            d.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
        }.getOrDefault(it)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = post.frontMatter.title.ifBlank { post.slug },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dateStr != null) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    text = stringResource(R.string.min_read, minRead),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// ─── Empty / Placeholder ─────────────────────────────────────────────────────

@Composable
private fun NoDraftsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.no_drafts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeSetupPlaceholder(onSetup: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
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
