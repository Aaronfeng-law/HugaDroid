package com.soogoino.hugadroid.domain

import com.soogoino.hugadroid.data.model.FrontMatterFormat
import com.soogoino.hugadroid.data.model.HugoPost
import com.soogoino.hugadroid.data.prefs.AppPreferences
import com.soogoino.hugadroid.data.prefs.AppSettings
import com.soogoino.hugadroid.data.repository.PostRepository
import com.soogoino.hugadroid.git.GitAuth
import com.soogoino.hugadroid.git.GitRepository
import com.soogoino.hugadroid.git.GitResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Observe cached post list from Room, ordered by date desc. */
class ObservePostsUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    operator fun invoke(): Flow<List<HugoPost>> = postRepository.observePosts()
}

/** Scan file system and refresh Room post cache. */
class ScanPostsUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val prefs: AppPreferences,
) {
    suspend operator fun invoke() {
        val settings = prefs.settings.first()
        if (settings.localRepoPath.isNotBlank()) {
            postRepository.scanAndRefresh(settings.localRepoPath)
        }
    }
}

/** Read a specific post from disk. */
class ReadPostUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    suspend operator fun invoke(filePath: String): HugoPost? =
        postRepository.readPost(filePath)
}

/** Save (write to disk + update cache). */
class SavePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    suspend operator fun invoke(post: HugoPost) = postRepository.savePost(post)
}

/** Create a new page-bundle post scaffold. Returns filePath. */
class CreatePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val prefs: AppPreferences,
) {
    suspend operator fun invoke(slug: String, format: FrontMatterFormat = FrontMatterFormat.YAML, section: String = "posts"): String {
        val settings = prefs.settings.first()
        return postRepository.createPost(settings.localRepoPath, slug, format, section)
    }
}

/** Delete a post (file + Room cache). */
class DeletePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    suspend operator fun invoke(filePath: String) = postRepository.deletePost(filePath)
}

/** Auto-save raw content debounce write to disk. */
class AutoSaveUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    suspend operator fun invoke(filePath: String, content: String) =
        postRepository.saveRawToDisk(filePath, content)
}

/** Pull + (optionally commit & push) then re-scan posts. */
class SyncRepoUseCase @Inject constructor(
    private val gitRepository: GitRepository,
    private val postRepository: PostRepository,
    private val prefs: AppPreferences,
) {
    data class SyncResult(val pulled: Boolean, val pushed: Boolean, val error: Throwable? = null)

    suspend operator fun invoke(
        commitMessage: String? = null,
        onProgress: (percent: Int, task: String) -> Unit = { _, _ -> },
    ): SyncResult {
        val settings = prefs.settings.first()
        val auth = settings.toGitAuth() ?: return SyncResult(false, false, IllegalStateException("No auth configured"))
        val repoPath = settings.localRepoPath

        // Pull
        val pullResult = gitRepository.pull(repoPath, auth, onProgress)
        when (pullResult) {
            is GitResult.Failure -> return SyncResult(false, false, pullResult.error)
            is GitResult.Conflict -> return SyncResult(
                false, false,
                Exception("merge_conflict:${pullResult.files.joinToString(",")}"),
            )
            is GitResult.Success -> { /* continue to commit+push */ }
        }

        // Commit + push (commitAndPush handles the "nothing to commit" case internally,
        // returning Success(false) when the working tree is already clean after staging).
        val msg = commitMessage ?: "Auto-sync: ${java.time.Instant.now()}"
        // Author info is required — caller must ensure it is configured before syncing
        val authorName = settings.authorName.ifBlank { return SyncResult(false, false, IllegalStateException("author_name_missing")) }
        val authorEmail = settings.authorEmail.ifBlank { return SyncResult(false, false, IllegalStateException("author_email_missing")) }
        val pushResult = gitRepository.commitAndPush(
            repoPath, msg,
            authorName,
            authorEmail,
            auth, onProgress,
        )
        val pushed = pushResult is GitResult.Success && (pushResult as GitResult.Success).data

        // Refresh post list
        postRepository.scanAndRefresh(repoPath)

        return SyncResult(pulled = true, pushed = pushed)
    }
}

/** Hard-reset working tree to HEAD — discards all local uncommitted changes. */
class DiscardLocalChangesUseCase @Inject constructor(
    private val gitRepository: GitRepository,
    private val prefs: AppPreferences,
) {
    suspend operator fun invoke(): GitResult<Unit> {
        val settings = prefs.settings.first()
        if (settings.localRepoPath.isBlank()) {
            return GitResult.Failure(IllegalStateException("No repo path configured"))
        }
        return gitRepository.discardLocalChanges(settings.localRepoPath)
    }
}

/** Fetch remote and hard-reset to `origin/<branch>` — accepts remote as source of truth. */
class ForceResetToRemoteUseCase @Inject constructor(
    private val gitRepository: GitRepository,
    private val prefs: AppPreferences,
) {
    suspend operator fun invoke(
        onProgress: (percent: Int, task: String) -> Unit = { _, _ -> },
    ): GitResult<Unit> {
        val settings = prefs.settings.first()
        val auth = settings.toGitAuth()
            ?: return GitResult.Failure(IllegalStateException("No auth configured"))
        if (settings.localRepoPath.isBlank()) {
            return GitResult.Failure(IllegalStateException("No repo path configured"))
        }
        return gitRepository.forceResetToRemote(settings.localRepoPath, auth, onProgress)
    }
}
