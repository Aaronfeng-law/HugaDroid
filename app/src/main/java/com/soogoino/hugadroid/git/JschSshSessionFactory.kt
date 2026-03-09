package com.soogoino.hugadroid.git

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.soogoino.hugadroid.BuildConfig
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "JschSSH"

/**
 * Android-compatible SSH session factory backed by JSch (mwiede fork).
 *
 * Session caching: JSch [Session] objects (TCP + SSH handshake) are expensive to create,
 * especially on mobile networks (~1–3 s per handshake). This factory maintains a process-scoped
 * cache keyed by "keyPath|user@host:port" so that consecutive operations within the same sync
 * (pull → push) reuse the same underlying TCP connection instead of negotiating two separate
 * handshakes.
 *
 * MINA SSHD fails on Android because:
 *  - ClientBuilder's static initializer reads `user.home` (doesn't exist on Android)
 *  - KeyExchangeFactories can't initialize without proper JCE/BC setup
 *  - `KeyFactory.getInstance("Ed25519","BC")` fails due to Android's built-in BC collision
 *
 * JSch works natively on Android and supports Ed25519 PKCS#8 PEM keys.
 */
class JschSshSessionFactory(
    private val keyPath: String,
) : SshSessionFactory() {

    companion object {
        /** Cache of live JSch sessions keyed by "keyPath|user@host:port". */
        private val sessionCache = ConcurrentHashMap<String, Session>()

        private fun cacheKey(keyPath: String, user: String, host: String, port: Int) =
            "$keyPath|$user@$host:$port"

        /**
         * Evict and disconnect all cached sessions.
         * Call this after a full sync cycle completes so stale connections don't accumulate.
         */
        fun evictAll() {
            sessionCache.values.toList().forEach { runCatching { it.disconnect() } }
            sessionCache.clear()
            Log.d(TAG, "evictAll: all cached sessions released")
        }
    }

    override fun getSession(
        uri: URIish,
        credentialsProvider: org.eclipse.jgit.transport.CredentialsProvider?,
        fs: FS,
        tmsec: Int,
    ): RemoteSession {
        val port = if (uri.port > 0) uri.port else 22
        val user = uri.user ?: "git"
        val host = uri.host ?: run {
            Log.e(TAG, "No host in URI: $uri")
            throw TransportException(uri, "No host in URI: $uri")
        }
        val key = cacheKey(keyPath, user, host, port)

        // Reuse a cached, still-connected session — avoids a full TCP + SSH handshake.
        sessionCache[key]?.let { cached ->
            if (cached.isConnected) {
                if (BuildConfig.DEBUG) Log.d(TAG, "getSession: reusing cached session $user@$host:$port")
                return JschRemoteSession(cached, ownsSession = false)
            } else {
                Log.d(TAG, "getSession: cached session dead, reconnecting $user@$host:$port")
                sessionCache.remove(key)
            }
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "getSession: new connection $user@$host:$port  keyPath=$keyPath")

        try {
            val jsch = JSch()
            val keyFile = File(keyPath)
            if (keyFile.exists()) {
                if (BuildConfig.DEBUG) Log.d(TAG, "addIdentity: $keyPath (${keyFile.length()} bytes)")
                jsch.addIdentity(keyPath)
            } else {
                Log.e(TAG, "Key file not found: $keyPath")
                throw TransportException(uri, "SSH key not found at $keyPath")
            }

            // TOFU: auto-accept new host keys and persist to known_hosts;
            // reject if fingerprint changes on subsequent connections.
            val sshDir = keyFile.parentFile
            if (sshDir != null) {
                val knownHosts = File(sshDir, "known_hosts")
                if (!knownHosts.exists()) knownHosts.createNewFile()
                jsch.setKnownHosts(knownHosts.absolutePath)
            }

            val session: Session = jsch.getSession(user, host, port)
            session.setConfig("StrictHostKeyChecking", "accept-new")
            session.setConfig("PreferredAuthentications", "publickey")
            // Avoid server-alive issues on Android networking
            session.setConfig("ServerAliveInterval", "60")

            val timeout = if (tmsec > 0) tmsec else 30_000
            Log.d(TAG, "connecting with timeout=${timeout}ms …")
            session.connect(timeout)
            if (BuildConfig.DEBUG) Log.i(TAG, "Connected to $host:$port as $user (${session.serverVersion})")

            // Cache for reuse within this sync cycle.
            sessionCache[key] = session
            return JschRemoteSession(session, ownsSession = false)
        } catch (e: JSchException) {
            Log.e(TAG, "JSch auth/connect failure: ${e.message}", e)
            throw TransportException(uri, "SSH error: ${e.message}", e)
        }
    }

    override fun getType(): String = "jsch-android"
}

// ─── RemoteSession adapter ───────────────────────────────────────────────────

private class JschRemoteSession(
    private val session: Session,
    /**
     * When false, [disconnect] keeps the JSch session alive in the cache so it can be
     * reused by the next operation in the same sync cycle (pull → push).
     */
    private val ownsSession: Boolean,
) : RemoteSession {

    override fun exec(commandName: String, timeout: Int): Process {
        Log.d(TAG, "exec: $commandName")
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(commandName)

        val processIn  = channel.inputStream
        val processOut = channel.outputStream
        val processErr = channel.errStream

        val ms = if (timeout > 0) timeout else 30_000
        channel.connect(ms)

        return object : Process() {
            override fun getOutputStream(): OutputStream = processOut
            override fun getInputStream(): InputStream  = processIn
            override fun getErrorStream(): InputStream  = processErr

            override fun waitFor(): Int {
                while (!channel.isClosed) Thread.sleep(50)
                return channel.exitStatus
            }

            override fun exitValue(): Int {
                if (!channel.isClosed) throw IllegalThreadStateException("Process not finished")
                return channel.exitStatus
            }

            override fun destroy() {
                channel.disconnect()
            }
        }
    }

    override fun disconnect() {
        if (ownsSession) {
            Log.d(TAG, "disconnect: closing owned session")
            runCatching { session.disconnect() }
        } else {
            // Session is cached for reuse; actual TCP close happens via evictAll().
            Log.d(TAG, "disconnect: session cached, skipping TCP close")
        }
    }
}
