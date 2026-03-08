package com.soogoino.hugadroid.git

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.soogoino.hugadroid.BuildConfig
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "JschSSH"

/**
 * Android-compatible SSH session factory backed by JSch (mwiede fork).
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
        if (BuildConfig.DEBUG) Log.d(TAG, "getSession: $user@$host:$port  keyPath=$keyPath")

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
            return JschRemoteSession(session)
        } catch (e: JSchException) {
            Log.e(TAG, "JSch auth/connect failure: ${e.message}", e)
            throw TransportException(uri, "SSH error: ${e.message}", e)
        }
    }

    override fun getType(): String = "jsch-android"
}

// ─── RemoteSession adapter ───────────────────────────────────────────────────

private class JschRemoteSession(private val session: Session) : RemoteSession {

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
        Log.d(TAG, "disconnect")
        runCatching { session.disconnect() }
    }
}
