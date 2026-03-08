package com.soogoino.hugadroid.git

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private const val SSHTAG = "SshKeyManager"

data class SshKeyPair(
    val privateKeyPath: String,
    val publicKeyOpenSsh: String,
)

@Singleton
class SshKeyManager @Inject constructor() {

    companion object {
        const val KEY_FILENAME = "id_ed25519"
        const val KEY_COMMENT  = "huga@android"
    }

    /**
     * Generate an Ed25519 key pair and persist to [sshDir].
     * Private key is written in OpenSSH PROTOCOL.key format
     * ("-----BEGIN OPENSSH PRIVATE KEY-----") which JSch reads natively.
     */
    suspend fun generateKeyPair(sshDir: File): SshKeyPair = withContext(Dispatchers.IO) {
        sshDir.mkdirs()
        val privFile = File(sshDir, KEY_FILENAME)
        val pubFile  = File(sshDir, "$KEY_FILENAME.pub")

        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()
        val privKey = keyPair.private as Ed25519PrivateKeyParameters
        val pubKey  = keyPair.public  as Ed25519PublicKeyParameters

        // OpenSSH PROTOCOL.key format — natively parsed by JSch mwiede fork
        val privBlob = OpenSSHPrivateKeyUtil.encodePrivateKey(privKey)
        val pem = buildString {
            append("-----BEGIN OPENSSH PRIVATE KEY-----\n")
            Base64.getEncoder().encodeToString(privBlob).chunked(64)
                .forEach { append(it).append('\n') }
            append("-----END OPENSSH PRIVATE KEY-----\n")
        }
        privFile.writeText(pem)
        privFile.setReadable(false, false)
        privFile.setReadable(true, true)

        val pubBlob   = OpenSSHPublicKeyUtil.encodePublicKey(pubKey)
        val pubKeyStr = "ssh-ed25519 ${
            Base64.getEncoder().encodeToString(pubBlob)
        } $KEY_COMMENT"
        pubFile.writeText(pubKeyStr)

        Log.d(SSHTAG, "Generated key pair at ${privFile.absolutePath}")
        Log.d(SSHTAG, "Public key fingerprint generated (${pubKeyStr.length} chars)")
        SshKeyPair(privFile.absolutePath, pubKeyStr)
    }

    fun hasKey(sshDir: File): Boolean = File(sshDir, KEY_FILENAME).exists()

    fun readPublicKey(sshDir: File): String? =
        File(sshDir, "$KEY_FILENAME.pub").takeIf { it.exists() }?.readText()?.trim()
}
