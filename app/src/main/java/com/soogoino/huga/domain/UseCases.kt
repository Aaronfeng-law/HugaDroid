package com.soogoino.huga.domain

import com.soogoino.huga.data.model.FrontMatterFormat
import com.soogoino.huga.data.model.HugoPost
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.data.prefs.AppSettings
import com.soogoino.huga.data.repository.PostRepository
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.GitResult
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
        if (pullResult is GitResult.Failure) {
            return SyncResult(false, false, pullResult.error)
        }

        // Commit + push if there are local changes
        var pushed = false
        if (gitRepository.hasLocalChanges(repoPath)) {
            val msg = commitMessage ?: "Auto-sync: ${java.time.Instant.now()}"
            val pushResult = gitRepository.commitAndPush(
                repoPath, msg,
                settings.authorName.ifBlank { "Huga" },
                settings.authorEmail.ifBlank { "huga@mobile" },
                auth, onProgress,
            )
            pushed = pushResult is GitResult.Success
        }

        // Refresh post list
        postRepository.scanAndRefresh(repoPath)

        return SyncResult(pulled = true, pushed = pushed)
    }
}
