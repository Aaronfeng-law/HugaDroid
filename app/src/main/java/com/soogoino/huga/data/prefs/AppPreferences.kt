package com.soogoino.huga.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.soogoino.huga.data.repository.SecureTokenStore
import com.soogoino.huga.git.GitAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "huga_prefs")

enum class AuthType { PAT, SSH }
enum class MediaStrategy { PAGE_BUNDLE, STATIC_FOLDER }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val repoUrl: String = "",
    val localRepoPath: String = "",
    val authType: AuthType = AuthType.PAT,
    val patToken: String = "",      // stored encrypted via EncryptedSharedPreferences separately
    val sshKeyPath: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val mediaStrategy: MediaStrategy = MediaStrategy.PAGE_BUNDLE,
    val autoSyncEnabled: Boolean = true,
    val autoSyncIntervalMinutes: Int = 30,
    val isRepoSetup: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: String = "",   // "" = system, "en", "zh-TW", ...
) {
    /** Build a [GitAuth] from current settings. Returns null if credentials are missing. */
    fun toGitAuth(): GitAuth? = when (authType) {
        AuthType.PAT -> if (patToken.isNotBlank()) GitAuth.Pat(token = patToken) else null
        AuthType.SSH -> if (sshKeyPath.isNotBlank()) GitAuth.SshKey(keyPath = sshKeyPath) else null
    }
}

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureTokenStore: SecureTokenStore,
) {
    private object Keys {
        val REPO_URL = stringPreferencesKey("repo_url")
        val LOCAL_REPO_PATH = stringPreferencesKey("local_repo_path")
        val AUTH_TYPE = stringPreferencesKey("auth_type")
        val SSH_KEY_PATH = stringPreferencesKey("ssh_key_path")
        val AUTHOR_NAME = stringPreferencesKey("author_name")
        val AUTHOR_EMAIL = stringPreferencesKey("author_email")
        val MEDIA_STRATEGY = stringPreferencesKey("media_strategy")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val AUTO_SYNC_INTERVAL = intPreferencesKey("auto_sync_interval_minutes")
        val IS_REPO_SETUP = booleanPreferencesKey("is_repo_setup")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PINNED_POSTS = stringSetPreferencesKey("pinned_posts")
        val LANGUAGE = stringPreferencesKey("app_language")
    }

    val pinnedPosts: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.PINNED_POSTS] ?: emptySet()
    }

    suspend fun togglePinPost(filePath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PINNED_POSTS] ?: emptySet()
            prefs[Keys.PINNED_POSTS] = if (filePath in current) current - filePath else current + filePath
        }
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val authType = runCatching { AuthType.valueOf(prefs[Keys.AUTH_TYPE] ?: "") }.getOrDefault(AuthType.PAT)
        AppSettings(
            repoUrl = prefs[Keys.REPO_URL] ?: "",
            localRepoPath = prefs[Keys.LOCAL_REPO_PATH] ?: "",
            authType = authType,
            // PAT is stored encrypted in EncryptedSharedPreferences, not DataStore
            patToken = if (authType == AuthType.PAT) secureTokenStore.getToken() else "",
            sshKeyPath = prefs[Keys.SSH_KEY_PATH] ?: "",
            authorName = prefs[Keys.AUTHOR_NAME] ?: "",
            authorEmail = prefs[Keys.AUTHOR_EMAIL] ?: "",
            mediaStrategy = runCatching { MediaStrategy.valueOf(prefs[Keys.MEDIA_STRATEGY] ?: "") }.getOrDefault(MediaStrategy.PAGE_BUNDLE),
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            autoSyncIntervalMinutes = prefs[Keys.AUTO_SYNC_INTERVAL] ?: 30,
            isRepoSetup = prefs[Keys.IS_REPO_SETUP] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            appLanguage = prefs[Keys.LANGUAGE] ?: "",
        )
    }

    suspend fun update(block: AppSettings.() -> AppSettings) {
        // Read current, apply block, write back
        context.dataStore.edit { prefs ->
            // Re-read via current prefs snapshot
            val authType = runCatching { AuthType.valueOf(prefs[Keys.AUTH_TYPE] ?: "") }.getOrDefault(AuthType.PAT)
            val current = AppSettings(
                repoUrl = prefs[Keys.REPO_URL] ?: "",
                localRepoPath = prefs[Keys.LOCAL_REPO_PATH] ?: "",
                authType = authType,
                patToken = if (authType == AuthType.PAT) secureTokenStore.getToken() else "",
                sshKeyPath = prefs[Keys.SSH_KEY_PATH] ?: "",
                authorName = prefs[Keys.AUTHOR_NAME] ?: "",
                authorEmail = prefs[Keys.AUTHOR_EMAIL] ?: "",
                mediaStrategy = runCatching { MediaStrategy.valueOf(prefs[Keys.MEDIA_STRATEGY] ?: "") }.getOrDefault(MediaStrategy.PAGE_BUNDLE),
                autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
                autoSyncIntervalMinutes = prefs[Keys.AUTO_SYNC_INTERVAL] ?: 30,
                isRepoSetup = prefs[Keys.IS_REPO_SETUP] ?: false,
                themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
                appLanguage = prefs[Keys.LANGUAGE] ?: "",
            )
            val updated = current.block()
            prefs[Keys.REPO_URL] = updated.repoUrl
            prefs[Keys.LOCAL_REPO_PATH] = updated.localRepoPath
            prefs[Keys.AUTH_TYPE] = updated.authType.name
            prefs[Keys.SSH_KEY_PATH] = updated.sshKeyPath
            prefs[Keys.AUTHOR_NAME] = updated.authorName
            prefs[Keys.AUTHOR_EMAIL] = updated.authorEmail
            prefs[Keys.MEDIA_STRATEGY] = updated.mediaStrategy.name
            prefs[Keys.AUTO_SYNC_ENABLED] = updated.autoSyncEnabled
            prefs[Keys.AUTO_SYNC_INTERVAL] = updated.autoSyncIntervalMinutes
            prefs[Keys.IS_REPO_SETUP] = updated.isRepoSetup
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.LANGUAGE] = updated.appLanguage
        }
    }
}
