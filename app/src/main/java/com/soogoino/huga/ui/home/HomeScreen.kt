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
import com.soogoino.huga.domain.SyncRepoUseCase
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.ui.components.HugaNavigationBar
import com.soogoino.huga.ui.components.HugaTab
import com.soogoino.huga.ui.util.countWords
import com.soogoino.huga.ui.util.estimatedMinRead
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
    val aheadCount: Int = 0,
    val isRepoSetup: Boolean = false,
    val isSyncing: Boolean = false,
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observePostsUseCase: ObservePostsUseCase,
    private val syncRepoUseCase: SyncRepoUseCase,
    private val gitRepository: GitRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { s ->
                _uiState.update { it.copy(isRepoSetup = s.isRepoSetup) }
                if (s.isRepoSetup && s.localRepoPath.isNotBlank()) {
                    val ahead = runCatching { gitRepository.aheadCount(s.localRepoPath) }.getOrDefault(0)
                    _uiState.update { it.copy(aheadCount = ahead) }
                }
            }
        }
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
                val totalWords = posts.sumOf { p -> countWords(p.bodyMarkdown) }
                _uiState.update {
                    it.copy(
                        publishedCount = published.size,
                        draftCount = drafts.size,
                        writingDays = writingDays,
                        uniqueTags = tags,
                        uniqueCategories = cats,
                        totalWords = totalWords,
                        draftPosts = drafts,
                    )
                }
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
    onNavigateToPosts: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onOpenPost: (filePath: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
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
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        },
        bottomBar = {
            HugaNavigationBar(
                selected = HugaTab.HOME,
                onHome = {},
                onPosts = onNavigateToPosts,
                onFiles = onNavigateToFiles,
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
        uiState.totalWords >= 1_000 -> "%.1fK".format(uiState.totalWords / 1_000f)
        else -> uiState.totalWords.toString()
    }
    val stats = listOf(
        R.string.published to uiState.publishedCount.toString(),
        R.string.drafts_count to uiState.draftCount.toString(),
        R.string.writing_days to uiState.writingDays.toString(),
        R.string.unique_tags to uiState.uniqueTags.toString(),
        R.string.unique_categories to uiState.uniqueCategories.toString(),
        R.string.total_words to totalWordsLabel,
    )

    val rows = stats.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (labelRes, value) ->
                    StatCard(
                        label = stringResource(labelRes),
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill remaining space if row is not full
                if (row.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Draft Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftCard(post: HugoPost, onClick: () -> Unit) {
    val wordCount = countWords(post.bodyMarkdown)
    val minRead = estimatedMinRead(post.bodyMarkdown)
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
