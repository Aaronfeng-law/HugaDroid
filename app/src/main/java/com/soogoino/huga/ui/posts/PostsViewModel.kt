package com.soogoino.huga.ui.posts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.R
import com.soogoino.huga.data.model.HugoPost
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.domain.*
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.isAuthError
import com.soogoino.huga.git.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class SortOrder { DATE_DESC, DATE_ASC, TITLE_ASC, TITLE_DESC, WORDS_DESC, WORDS_ASC }

data class PostsUiState(
    val posts: List<HugoPost> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val syncTask: String = "",
    val aheadCount: Int = 0,
    val error: String? = null,
    val isRepoSetup: Boolean = false,
    val showDrafts: Boolean = true,
    val filterOnlyDraft: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val availableSections: List<String> = listOf("posts"),
    val pinnedFilePaths: Set<String> = emptySet(),
)

sealed class PostsEvent {
    data class ShowSnackbar(val message: String) : PostsEvent()
    data class NavigateToEditor(val filePath: String) : PostsEvent()
}

/** Projection used to gate filteredPosts recomputation — excludes sync/loading fields. */
private data class FilterKey(
    val posts: List<HugoPost>,
    val filterOnlyDraft: Boolean,
    val searchQuery: String,
    val sortOrder: SortOrder,
    val pinnedFilePaths: Set<String>,
)

@HiltViewModel
class PostsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observePostsUseCase: ObservePostsUseCase,
    private val scanPostsUseCase: ScanPostsUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val syncRepoUseCase: SyncRepoUseCase,
    private val gitRepository: GitRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostsUiState())
    val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PostsEvent>()
    val events: SharedFlow<PostsEvent> = _events.asSharedFlow()

    init {
        // Observe pinned posts
        viewModelScope.launch {
            prefs.pinnedPosts.collect { pinned ->
                _uiState.update { it.copy(pinnedFilePaths = pinned) }
            }
        }
        // Observe setup status
        viewModelScope.launch {
            prefs.settings.collect { s ->
                _uiState.update { it.copy(isRepoSetup = s.isRepoSetup) }
                // Load available content sections from disk (IO-bound)
                if (s.isRepoSetup && s.localRepoPath.isNotBlank()) {
                    val sections = withContext(Dispatchers.IO) {
                        File(s.localRepoPath, "content")
                            .listFiles { f -> f.isDirectory }
                            ?.map { it.name }
                            ?.sorted()
                            ?.ifEmpty { listOf("posts") }
                            ?: listOf("posts")
                    }
                    _uiState.update { it.copy(availableSections = sections) }
                }
            }
        }
        // Observe cached posts
        viewModelScope.launch {
            observePostsUseCase().collect { posts ->
                _uiState.update { state ->
                    state.copy(posts = posts, isLoading = false)
                }
            }
        }
        // Initial scan — skip if Room already has cached data to avoid blocking startup
        viewModelScope.launch {
            val hasCached = observePostsUseCase().first().isNotEmpty()
            if (!hasCached) refreshPosts()
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { scanPostsUseCase() }
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun createPost(slug: String, section: String = "posts") {
        if (slug.isBlank()) return
        viewModelScope.launch {
            runCatching { createPostUseCase(slug, section = section) }
                .onSuccess { filePath ->
                    scanPostsUseCase()
                    _events.emit(PostsEvent.NavigateToEditor(filePath))
                }
                .onFailure { e ->
                    _events.emit(PostsEvent.ShowSnackbar(context.getString(R.string.post_create_failed, e.message ?: "")))
                }
        }
    }

    fun deletePost(post: HugoPost) {
        viewModelScope.launch {
            runCatching { deletePostUseCase(post.filePath) }
                .onSuccess { _events.emit(PostsEvent.ShowSnackbar(context.getString(R.string.post_deleted, post.frontMatter.title))) }
                .onFailure { e -> _events.emit(PostsEvent.ShowSnackbar(context.getString(R.string.post_delete_failed, e.message ?: ""))) }
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncProgress = 0) }
            val result = syncRepoUseCase(
                onProgress = { pct, task ->
                    _uiState.update { it.copy(syncProgress = pct, syncTask = task) }
                }
            )
            val msg = when {
                result.error != null -> {
                    val e = result.error
                    when {
                        isNetworkError(e) -> context.getString(R.string.error_network)
                        isAuthError(e) -> context.getString(R.string.error_auth_ssh)
                        else -> e.message ?: context.getString(R.string.sync_failed)
                    }
                }
                result.pushed -> context.getString(R.string.push_success)
                result.pulled -> context.getString(R.string.pull_success)
                else -> context.getString(R.string.sync_complete)
            }
            _uiState.update { it.copy(isSyncing = false, syncProgress = 0, syncTask = "") }
            _events.emit(PostsEvent.ShowSnackbar(msg))
            // Rescan so list reflects any remote changes pulled in
            refreshPosts()
            // Refresh ahead count
            val settings = prefs.settings.first()
            if (settings.localRepoPath.isNotBlank()) {
                val ahead = gitRepository.aheadCount(settings.localRepoPath)
                _uiState.update { it.copy(aheadCount = ahead) }
            }
        }
    }

    fun togglePin(filePath: String) {
        viewModelScope.launch { prefs.togglePinPost(filePath) }
    }

    fun toggleDraftFilter() {
        _uiState.update { it.copy(filterOnlyDraft = !it.filterOnlyDraft) }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    val filteredPosts: StateFlow<List<HugoPost>> =
        _uiState
            // Only recompute when filter-relevant fields actually change.
            // This prevents syncProgress / isSyncing / aheadCount updates from
            // triggering an expensive list rebuild on the Default dispatcher.
            .map { s -> FilterKey(s.posts, s.filterOnlyDraft, s.searchQuery, s.sortOrder, s.pinnedFilePaths) }
            .distinctUntilChanged()
            .map { key ->
                var list = if (key.filterOnlyDraft) key.posts.filter { it.frontMatter.draft }
                           else key.posts
                if (key.searchQuery.isNotBlank()) {
                    val q = key.searchQuery.lowercase()
                    list = list.filter { post ->
                        post.frontMatter.title.lowercase().contains(q) ||
                        post.frontMatter.description.lowercase().contains(q) ||
                        post.frontMatter.tags.any { it.lowercase().contains(q) }
                    }
                }
                val sorted = when (key.sortOrder) {
                    SortOrder.DATE_DESC  -> list.sortedByDescending { it.frontMatter.date }
                    SortOrder.DATE_ASC   -> list.sortedBy { it.frontMatter.date }
                    SortOrder.TITLE_ASC  -> list.sortedBy { it.frontMatter.title.lowercase() }
                    SortOrder.TITLE_DESC -> list.sortedByDescending { it.frontMatter.title.lowercase() }
                    SortOrder.WORDS_DESC -> list.sortedByDescending { it.wordCount }
                    SortOrder.WORDS_ASC  -> list.sortedBy { it.wordCount }
                }
                val (pinned, unpinned) = sorted.partition { it.filePath in key.pinnedFilePaths }
                pinned + unpinned
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
