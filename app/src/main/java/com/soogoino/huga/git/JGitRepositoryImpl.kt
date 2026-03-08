package com.soogoino.huga.git

import android.util.Log
import com.soogoino.huga.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "JGitRepo"

@Singleton
class JGitRepositoryImpl @Inject constructor() : GitRepository {

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun transportConfigCallback(auth: GitAuth) =
        org.eclipse.jgit.api.TransportConfigCallback { transport ->
            Log.d(TAG, "transportConfigCallback: auth=${auth::class.simpleName} transport=${transport::class.qualifiedName}")
            when (auth) {
                is GitAuth.SshKey -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, "SSH key path=${auth.keyPath} exists=${File(auth.keyPath).exists()}")
                    val sshTransport = transport as? SshTransport
                    if (sshTransport == null) {
                        // Transport is NOT SSH — URL is likely still HTTPS; log clearly
                        Log.e(TAG, "⚠️ SSH auth requested but transport is ${transport::class.qualifiedName} (not SshTransport). URL may not have been converted to SSH format!")
                    } else {
                        val factory = JschSshSessionFactory(auth.keyPath)
                        sshTransport.sshSessionFactory = factory
                        Log.d(TAG, "JschSshSessionFactory installed on ${transport::class.simpleName}")
                    }
                }
                is GitAuth.Pat -> { /* PAT via CredentialsProvider */ }
            }
        }

    private fun credentialsProvider(auth: GitAuth) = when (auth) {
        is GitAuth.Pat -> UsernamePasswordCredentialsProvider(auth.username, auth.token)
        is GitAuth.SshKey -> null
    }

    private fun progressMonitor(onProgress: (Int, String) -> Unit) = object : ProgressMonitor {
        private var totalWork = 0
        private var done = 0
        private var taskTitle = ""

        override fun start(totalTasks: Int) {}
        override fun beginTask(title: String, totalWork: Int) {
            taskTitle = title
            this.totalWork = totalWork
            done = 0
        }
        override fun update(completed: Int) {
            done += completed
            val percent = if (totalWork > 0) (done * 100 / totalWork).coerceIn(0, 100) else 0
            onProgress(percent, taskTitle)
        }
        override fun endTask() { onProgress(100, taskTitle) }
        override fun isCancelled() = false
        override fun showDuration(enabled: Boolean) {}
    }

    // ─── GitRepository implementation ────────────────────────────────────────

    override suspend fun clone(
        remoteUrl: String,
        localPath: String,
        auth: GitAuth,
        onProgress: (Int, String) -> Unit,
    ): GitResult<Unit> = withContext(Dispatchers.IO) {
        retryOnNetworkError(tag = TAG) {
            runCatching {
                if (BuildConfig.DEBUG) Log.i(TAG, "clone  url=$remoteUrl  auth=${auth::class.simpleName}  localPath=$localPath")
                // Guard: SSH auth requires an SSH-style URL
                if (auth is GitAuth.SshKey && (remoteUrl.startsWith("https://") || remoteUrl.startsWith("http://"))) {
                    val msg = "SSH auth selected but URL is HTTP/HTTPS: $remoteUrl — convert to SSH URL (git@...) first"
                    Log.e(TAG, msg)
                    error(msg)
                }
                val dir = File(localPath)
                // If the directory already exists (e.g. previous clone / reconnect scenario),
                // wipe it so JGit doesn't throw "destination path already exists and is not an empty directory".
                if (dir.exists()) {
                    Log.i(TAG, "clone: target dir exists — deleting before re-clone: $localPath")
                    dir.deleteRecursively()
                }
                dir.mkdirs()
                val cmd = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(dir)
                    .setProgressMonitor(progressMonitor(onProgress))
                    .setTransportConfigCallback(transportConfigCallback(auth))
                credentialsProvider(auth)?.let { cmd.setCredentialsProvider(it) }
                cmd.call().close()
            }.fold(
                onSuccess = { GitResult.Success(Unit) },
                onFailure = { e ->
                    Log.e(TAG, "clone failed: ${e::class.simpleName} — ${e.message}", e)
                    GitResult.Failure(e)
                }
            )
        }
    }

    override suspend fun pull(
        localPath: String,
        auth: GitAuth,
        onProgress: (Int, String) -> Unit,
    ): GitResult<Unit> = withContext(Dispatchers.IO) {
        retryOnNetworkError(tag = TAG) {
            runCatching {
                Git.open(File(localPath)).use { git ->
                    val cmd = git.pull()
                        .setProgressMonitor(progressMonitor(onProgress))
                        .setTransportConfigCallback(transportConfigCallback(auth))
                    credentialsProvider(auth)?.let { cmd.setCredentialsProvider(it) }
                    cmd.call()
                }
            }.fold(
                onSuccess = { GitResult.Success(Unit) },
                onFailure = { e ->
                    Log.e(TAG, "pull failed", e)
                    GitResult.Failure(e)
                }
            )
        }
    }

    override suspend fun commitAndPush(
        localPath: String,
        message: String,
        authorName: String,
        authorEmail: String,
        auth: GitAuth,
        onProgress: (Int, String) -> Unit,
    ): GitResult<Unit> = withContext(Dispatchers.IO) {
        // ── Stage + Commit (local ops, not retried) ───────────────────────────────
        val commitResult = runCatching {
            Git.open(File(localPath)).use { git ->
                git.add().addFilepattern(".").call()
                git.add().setUpdate(true).addFilepattern(".").call()
                val personIdent = org.eclipse.jgit.lib.PersonIdent(authorName, authorEmail)
                git.commit()
                    .setMessage(message)
                    .setAuthor(personIdent)
                    .setCommitter(personIdent)
                    .call()
            }
        }
        if (commitResult.isFailure) {
            val e = commitResult.exceptionOrNull()!!
            Log.e(TAG, "commitAndPush: commit phase failed", e)
            return@withContext GitResult.Failure(e)
        }

        // ── Push (network op, retried up to 3×) ─────────────────────────────────
        retryOnNetworkError(tag = TAG) {
            runCatching {
                Git.open(File(localPath)).use { git ->
                    val pushCmd = git.push()
                        .setProgressMonitor(progressMonitor(onProgress))
                        .setTransportConfigCallback(transportConfigCallback(auth))
                    credentialsProvider(auth)?.let { pushCmd.setCredentialsProvider(it) }
                    val pushResults = pushCmd.call()
                    // Detect remote rejections (JGit encodes them in result, not as exceptions)
                    for (result in pushResults) {
                        for (update in result.remoteUpdates) {
                            val status = update.status
                            if (status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD ||
                                status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED ||
                                status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_OTHER_REASON ||
                                status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NODELETE) {
                                throw Exception("Push rejected by remote (${update.remoteName}): ${status.name}")
                            }
                        }
                    }
                }
            }.fold(
                onSuccess = { GitResult.Success(Unit) },
                onFailure = { e ->
                    Log.e(TAG, "commitAndPush: push phase failed", e)
                    GitResult.Failure(e)
                }
            )
        }
    }

    override suspend fun stageAll(localPath: String): GitResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(localPath)).use { git ->
                git.add().addFilepattern(".").call()
            }
        }.fold(
            onSuccess = { GitResult.Success(Unit) },
            onFailure = { GitResult.Failure(it) }
        )
    }

    override suspend fun hasLocalChanges(localPath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(localPath)).use { git ->
                val status = git.status().call()
                status.hasUncommittedChanges() || status.untracked.isNotEmpty()
            }
        }.getOrDefault(false)
    }

    override suspend fun aheadCount(localPath: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            Git.open(File(localPath)).use { git ->
                val repo = git.repository
                val head = repo.resolve("HEAD") ?: return@use 0
                // ROB-13: @{upstream} is null when no tracking branch is configured
                val tracking = repo.resolve("@{upstream}") ?: run {
                    Log.w(TAG, "aheadCount: no upstream tracking branch configured for $localPath")
                    return@use 0
                }
                val walk = org.eclipse.jgit.revwalk.RevWalk(repo)
                val count = walk.use {
                    it.markStart(it.parseCommit(head))
                    it.markUninteresting(it.parseCommit(tracking))
                    it.count()
                }
                count
            }
        }.getOrDefault(0)
    }

    override suspend fun log(localPath: String, limit: Int): GitResult<List<CommitEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Git.open(File(localPath)).use { git ->
                    git.log().setMaxCount(limit).call().map { commit ->
                        CommitEntry(
                            hash = commit.id.name,
                            shortHash = commit.id.abbreviate(7).name(),
                            message = commit.shortMessage,
                            authorName = commit.authorIdent.name,
                            authorEmail = commit.authorIdent.emailAddress,
                            time = commit.authorIdent.`when`.time / 1000,
                        )
                    }
                }
            }.fold(
                onSuccess = { GitResult.Success(it) },
                onFailure = { GitResult.Failure(it) }
            )
        }

    override suspend fun status(localPath: String): GitResult<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Git.open(File(localPath)).use { git ->
                    val s = git.status().call()
                    buildList {
                        s.modified.forEach { add("M  $it") }
                        s.added.forEach { add("A  $it") }
                        s.removed.forEach { add("D  $it") }
                        s.untracked.forEach { add("?? $it") }
                        s.conflicting.forEach { add("!! $it") }
                    }
                }
            }.fold(
                onSuccess = { GitResult.Success(it) },
                onFailure = { GitResult.Failure(it) }
            )
        }

    override fun isRepository(localPath: String): Boolean =
        runCatching {
            Git.open(File(localPath)).close()
            true
        }.getOrDefault(false)
}
