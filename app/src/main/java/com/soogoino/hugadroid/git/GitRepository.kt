package com.soogoino.hugadroid.git

import kotlinx.coroutines.flow.Flow

/** Authenticated transport configuration. */
sealed class GitAuth {
    /** HTTPS using a Personal Access Token */
    data class Pat(val username: String = "oauth2", val token: String) : GitAuth()
    /** SSH key stored at [keyPath]; protected by optional [passphrase] */
    data class SshKey(val keyPath: String, val passphrase: String = "") : GitAuth()
}

/** One entry in the commit log. */
data class CommitEntry(
    val hash: String,
    val shortHash: String,
    val message: String,
    val authorName: String,
    val authorEmail: String,
    val time: Long, // epoch seconds
)

/** Result wrapper for Git operations. */
sealed class GitResult<out T> {
    data class Success<T>(val data: T) : GitResult<T>()
    data class Failure(val error: Throwable) : GitResult<Nothing>()
}

/** High-level Git operations needed by the app. */
interface GitRepository {

    /** Clone [remoteUrl] into [localPath]. Emits progress 0..100. */
    suspend fun clone(
        remoteUrl: String,
        localPath: String,
        auth: GitAuth,
        onProgress: (percent: Int, task: String) -> Unit = { _, _ -> },
    ): GitResult<Unit>

    /** Pull (fast-forward or merge) from origin/main. */
    suspend fun pull(
        localPath: String,
        auth: GitAuth,
        onProgress: (percent: Int, task: String) -> Unit = { _, _ -> },
    ): GitResult<Unit>

    /** Stage all changes, commit, and push to origin. */
    suspend fun commitAndPush(
        localPath: String,
        message: String,
        authorName: String,
        authorEmail: String,
        auth: GitAuth,
        onProgress: (percent: Int, task: String) -> Unit = { _, _ -> },
    ): GitResult<Unit>

    /** Stage all modified/new/deleted files. */
    suspend fun stageAll(localPath: String): GitResult<Unit>

    /** True if there are uncommitted local changes. */
    suspend fun hasLocalChanges(localPath: String): Boolean

    /** Number of commits ahead of remote (need push). */
    suspend fun aheadCount(localPath: String): Int

    /** Recent commit log (newest first). */
    suspend fun log(localPath: String, limit: Int = 20): GitResult<List<CommitEntry>>

    /** List of files currently modified/untracked/deleted. */
    suspend fun status(localPath: String): GitResult<List<String>>

    /** Check whether [localPath] is an initialised Git repository. */
    fun isRepository(localPath: String): Boolean
}
