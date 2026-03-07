package com.soogoino.huga.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.data.local.CommitDao
import com.soogoino.huga.data.local.CommitEntity
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.domain.ScanPostsUseCase
import com.soogoino.huga.domain.SyncRepoUseCase
import com.soogoino.huga.git.CommitEntry
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.GitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SyncUiState(
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val syncTask: String = "",
    val commitLog: List<CommitEntry> = emptyList(),
    val pendingChanges: List<String> = emptyList(),
    val aheadCount: Int = 0,
    val error: String? = null,
    val isRepoSetup: Boolean = false,
)

sealed class SyncEvent {
    data class ShowSnackbar(val message: String) : SyncEvent()
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepoUseCase: SyncRepoUseCase,
    private val scanPostsUseCase: ScanPostsUseCase,
    private val gitRepository: GitRepository,
    private val commitDao: CommitDao,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>()
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { s -> _uiState.update { it.copy(isRepoSetup = s.isRepoSetup) } }
        }
        // Observe cached commit log
        viewModelScope.launch {
            commitDao.observeRecent().collect { entities ->
                _uiState.update { state ->
                    state.copy(commitLog = entities.map { it.toCommitEntry() })
                }
            }
        }
        refreshStatus()
    }

    private fun refreshStatus() {
        viewModelScope.launch {
            val settings = prefs.settings.first()
            if (!settings.isRepoSetup || settings.localRepoPath.isBlank()) return@launch
            val path = settings.localRepoPath
            val ahead = gitRepository.aheadCount(path)
            val statusResult = gitRepository.status(path)
            val changes = if (statusResult is GitResult.Success) statusResult.data else emptyList()
            _uiState.update { it.copy(aheadCount = ahead, pendingChanges = changes) }

            // Load commit log
            val logResult = gitRepository.log(path, 30)
            if (logResult is GitResult.Success) {
                val entities = logResult.data.map { it.toEntity() }
                commitDao.deleteAll()
                commitDao.upsertAll(entities)
            }
        }
    }

    fun pull() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val settings = prefs.settings.first()
            val auth = settings.toGitAuth() ?: run {
                _events.emit(SyncEvent.ShowSnackbar("Auth not configured"))
                _uiState.update { it.copy(isSyncing = false) }
                return@launch
            }
            val result = gitRepository.pull(settings.localRepoPath, auth) { pct, task ->
                _uiState.update { it.copy(syncProgress = pct, syncTask = task) }
            }
            val msg = if (result is GitResult.Success) "Pulled ✓" else "Pull failed: ${(result as GitResult.Failure).error.message}"
            _events.emit(SyncEvent.ShowSnackbar(msg))
            _uiState.update { it.copy(isSyncing = false, syncProgress = 0) }
            // Rescan posts so PostsScreen reflects files added/removed by pull
            if (result is GitResult.Success) {
                runCatching { scanPostsUseCase() }
            }
            refreshStatus()
        }
    }

    fun commitAndPush(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = syncRepoUseCase(commitMessage = message) { pct, task ->
                _uiState.update { it.copy(syncProgress = pct, syncTask = task) }
            }
            val msg = when {
                result.error != null -> "Push failed: ${result.error.message}"
                result.pushed -> "Committed & pushed ✓"
                else -> "Nothing to push"
            }
            _events.emit(SyncEvent.ShowSnackbar(msg))
            _uiState.update { it.copy(isSyncing = false, syncProgress = 0) }
            // Rescan posts so PostsScreen reflects any changes from push/pull
            if (result.error == null) {
                runCatching { scanPostsUseCase() }
            }
            refreshStatus()
        }
    }

    private fun CommitEntry.toEntity() = CommitEntity(hash, shortHash, message, authorName, authorEmail, time)
    private fun CommitEntity.toCommitEntry() = CommitEntry(hash, shortHash, message, authorName, authorEmail, time)
}
