package com.soogoino.huga.ui.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soogoino.huga.R
import com.soogoino.huga.data.local.CommitDao
import com.soogoino.huga.data.local.CommitEntity
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.domain.ScanPostsUseCase
import com.soogoino.huga.domain.SyncRepoUseCase
import com.soogoino.huga.git.CommitEntry
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.GitResult
import com.soogoino.huga.git.isAuthError
import com.soogoino.huga.git.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SyncUiState(
    val isSyncing: Boolean = false,
    // true while git status/log is being scanned in background (starts true so the
    // UI immediately shows "Checking…" rather than a misleading empty/gray state)
    val isLoadingStatus: Boolean = true,
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
    @ApplicationContext private val context: Context,
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

    /** Tracks the active pull/push coroutine so it can be cancelled by the user. */
    private var syncJob: Job? = null

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
            _uiState.update { it.copy(isLoadingStatus = true) }
            val settings = prefs.settings.first()
            if (!settings.isRepoSetup || settings.localRepoPath.isBlank()) {
                _uiState.update { it.copy(isLoadingStatus = false) }
                return@launch
            }
            val path = settings.localRepoPath
            val ahead = gitRepository.aheadCount(path)
            val statusResult = gitRepository.status(path)
            val changes = if (statusResult is GitResult.Success) statusResult.data else emptyList()
            _uiState.update { it.copy(aheadCount = ahead, pendingChanges = changes, isLoadingStatus = false) }

            // Load commit log (non-blocking — isLoadingStatus already cleared above)
            val logResult = gitRepository.log(path, 30)
            if (logResult is GitResult.Success) {
                val entities = logResult.data.map { it.toEntity() }
                commitDao.deleteAll()
                commitDao.upsertAll(entities)
            }
        }
    }

    fun pull() {
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val settings = prefs.settings.first()
            val auth = settings.toGitAuth() ?: run {
                _events.emit(SyncEvent.ShowSnackbar(context.getString(R.string.error_auth_not_configured)))
                _uiState.update { it.copy(isSyncing = false) }
                return@launch
            }
            val result = gitRepository.pull(settings.localRepoPath, auth) { pct, task ->
                _uiState.update { it.copy(syncProgress = pct, syncTask = task) }
            }
            val msg = if (result is GitResult.Success)
                context.getString(R.string.pull_success)
            else
                gitErrorMessage((result as GitResult.Failure).error)
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
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = syncRepoUseCase(commitMessage = message) { pct, task ->
                _uiState.update { it.copy(syncProgress = pct, syncTask = task) }
            }
            val msg = when {
                result.error != null -> gitErrorMessage(result.error)
                result.pushed -> context.getString(R.string.push_success)
                else -> context.getString(R.string.nothing_to_push)
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

    /**
     * Cancels an in-flight pull or push, and resets the UI to idle.
     * Safe to call from the UI when the user confirms they want to leave during a sync.
     */
    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        _uiState.update { it.copy(isSyncing = false, syncProgress = 0, syncTask = "") }
    }

    private fun CommitEntry.toEntity() = CommitEntity(hash, shortHash, message, authorName, authorEmail, time)
    private fun CommitEntity.toCommitEntry() = CommitEntry(hash, shortHash, message, authorName, authorEmail, time)

    /** Translates a git Throwable into a user-friendly, localised message. */
    private fun gitErrorMessage(e: Throwable): String = when {
        isNetworkError(e) -> context.getString(R.string.error_network)
        isAuthError(e) -> context.getString(R.string.error_auth_ssh)
        e.message?.startsWith("Push rejected") == true -> context.getString(R.string.error_push_rejected)
        else -> e.message ?: context.getString(R.string.sync_failed)
    }
}
