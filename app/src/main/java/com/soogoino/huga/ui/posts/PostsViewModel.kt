package com.soogoino.huga.ui.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.data.model.HugoPost
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.domain.*
import com.soogoino.huga.git.GitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class SortOrder { DATE_DESC, DATE_ASC, TITLE_ASC }

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
)

sealed class PostsEvent {
    data class ShowSnackbar(val message: String) : PostsEvent()
    data class NavigateToEditor(val filePath: String) : PostsEvent()
}

@HiltViewModel
class PostsViewModel @Inject constructor(
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
        // Observe setup status
        viewModelScope.launch {
            prefs.settings.collect { s ->
                _uiState.update { it.copy(isRepoSetup = s.isRepoSetup) }
                // Load available content sections from disk
                if (s.isRepoSetup && s.localRepoPath.isNotBlank()) {
                    val sections = File(s.localRepoPath, "content")
                        .listFiles { f -> f.isDirectory }
                        ?.map { it.name }
                        ?.sorted()
                        ?.ifEmpty { listOf("posts") }
                        ?: listOf("posts")
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
        // Initial scan
        refreshPosts()
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { scanPostsUseCase() }
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
                    _events.emit(PostsEvent.ShowSnackbar("Failed: ${e.message}"))
                }
        }
    }

    fun deletePost(post: HugoPost) {
        viewModelScope.launch {
            runCatching { deletePostUseCase(post.filePath) }
                .onSuccess { _events.emit(PostsEvent.ShowSnackbar("Deleted: ${post.frontMatter.title}")) }
                .onFailure { e -> _events.emit(PostsEvent.ShowSnackbar("Delete failed: ${e.message}")) }
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
                result.error != null -> "Sync failed: ${result.error.message}"
                result.pushed -> "Pushed ✓"
                result.pulled -> "Already up to date"
                else -> "Sync complete"
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
        _uiState.map { state ->
            var list = if (state.filterOnlyDraft) state.posts.filter { it.frontMatter.draft }
                       else state.posts
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.lowercase()
                list = list.filter { post ->
                    post.frontMatter.title.lowercase().contains(q) ||
                    post.frontMatter.description.lowercase().contains(q) ||
                    post.frontMatter.tags.any { it.lowercase().contains(q) }
                }
            }
            when (state.sortOrder) {
                SortOrder.DATE_DESC -> list.sortedByDescending { it.frontMatter.date }
                SortOrder.DATE_ASC  -> list.sortedBy { it.frontMatter.date }
                SortOrder.TITLE_ASC -> list.sortedBy { it.frontMatter.title.lowercase() }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
