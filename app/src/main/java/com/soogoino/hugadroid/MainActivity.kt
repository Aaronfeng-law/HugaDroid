package com.soogoino.hugadroid

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.soogoino.hugadroid.data.prefs.AppPreferences
import com.soogoino.hugadroid.data.prefs.AppSettings
import com.soogoino.hugadroid.data.prefs.ThemeMode
import com.soogoino.hugadroid.ui.navigation.HugaNavGraph
import com.soogoino.hugadroid.ui.navigation.Screen
import com.soogoino.hugadroid.ui.theme.HugaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: AppPreferences

    override fun attachBaseContext(newBase: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Read from plain SharedPreferences — synchronous and ANR-safe.
            // (DataStore is async; DataStore access via runBlocking risks ANR in early lifecycle.)
            val tag = newBase
                .getSharedPreferences("hugadroid_lang", android.content.Context.MODE_PRIVATE)
                .getString("app_language", "") ?: ""
            if (tag.isNotEmpty()) {
                val locale = Locale.forLanguageTag(tag)
                val config = Configuration(newBase.resources.configuration)
                config.setLocale(locale)
                super.attachBaseContext(newBase.createConfigurationContext(config))
                return
            }
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by prefs.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            HugaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Decide start destination: go to Setup if repo not configured
                    var startDest by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(Unit) {
                        val settings = prefs.settings.first()
                        startDest = if (settings.isRepoSetup) Screen.Home.route else Screen.Setup.route
                    }

                    startDest?.let { dest ->
                        HugaNavGraph(navController = navController, startDestination = dest)
                    }
                }
            }
        }
    }
}
