package com.soogoino.huga.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores sensitive tokens (PAT) encrypted via the Android Keystore.
 * Requires: androidx.security:security-crypto
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "huga_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_PAT, token).apply()
    }

    fun getToken(): String = prefs.getString(KEY_PAT, "") ?: ""

    fun clearToken() {
        prefs.edit().remove(KEY_PAT).apply()
    }

    companion object {
        private const val KEY_PAT = "pat_token"
    }
}
