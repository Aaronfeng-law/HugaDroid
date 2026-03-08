package com.soogoino.huga.git

import android.util.Log
import kotlinx.coroutines.delay
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG_RETRY = "NetworkRetry"

/**
 * Returns true if [e] (or any cause in its chain) represents a transient
 * network connectivity error that is worth retrying:
 *   - No network / DNS failure
 *   - TCP connect timeout or reset
 *   - SSH session-level connection issues
 */
fun isNetworkError(e: Throwable): Boolean {
    var cause: Throwable? = e
    while (cause != null) {
        if (cause is SocketTimeoutException ||
            cause is UnknownHostException ||
            cause is ConnectException
        ) return true

        if (cause is java.io.IOException) {
            val msg = cause.message?.lowercase() ?: ""
            if (msg.contains("connection reset") ||
                msg.contains("broken pipe") ||
                msg.contains("connection timed out") ||
                msg.contains("timed out") ||
                msg.contains("eof") ||
                msg.contains("connection refused") ||
                msg.contains("no route to host")
            ) return true
        }

        // JSch SSH transport errors (avoid hard class-name dependency)
        if (cause.javaClass.name.endsWith("JSchException")) {
            val msg = cause.message?.lowercase() ?: ""
            if (msg.contains("timeout") ||
                msg.contains("connection is not established") ||
                msg.contains("socket") ||
                msg.contains("broken")
            ) return true
        }

        cause = cause.cause
    }
    return false
}

/**
 * Returns true if [e] looks like an SSH authentication / authorization failure
 * (key not accepted, wrong key, deploy key lacks write access, etc.).
 */
fun isAuthError(e: Throwable): Boolean {
    var cause: Throwable? = e
    while (cause != null) {
        val msg = cause.message?.lowercase() ?: ""
        if (msg.contains("auth") ||
            msg.contains("permission denied") ||
            msg.contains("publickey") ||
            msg.contains("repository not found") ||
            msg.contains("access denied")
        ) return true
        cause = cause.cause
    }
    return false
}

/**
 * Executes [block] up to [maxAttempts] times, retrying only when the result
 * is a [GitResult.Failure] caused by a transient network error.
 *
 * Delay between attempts uses linear backoff: attempt N waits [baseDelayMs] × N ms.
 * Non-network failures (auth errors, push rejections, etc.) are returned immediately.
 */
suspend fun <T> retryOnNetworkError(
    maxAttempts: Int = 3,
    baseDelayMs: Long = 2_000,
    tag: String = TAG_RETRY,
    block: suspend () -> GitResult<T>,
): GitResult<T> {
    var lastResult: GitResult<T> = GitResult.Failure(IllegalStateException("unreachable"))
    for (attempt in 1..maxAttempts) {
        lastResult = block()
        if (lastResult is GitResult.Success) return lastResult

        val error = (lastResult as GitResult.Failure).error
        if (!isNetworkError(error)) {
            Log.d(tag, "Non-network failure on attempt $attempt — not retrying: ${error::class.simpleName}: ${error.message}")
            return lastResult
        }
        if (attempt < maxAttempts) {
            val waitMs = baseDelayMs * attempt
            Log.w(tag, "Network error on attempt $attempt/$maxAttempts — retrying in ${waitMs}ms: ${error.message}")
            delay(waitMs)
        } else {
            Log.e(tag, "Network error on final attempt $attempt/$maxAttempts — giving up: ${error.message}")
        }
    }
    return lastResult
}
