package com.soogoino.huga

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
import com.soogoino.huga.data.prefs.AppPreferences
import com.soogoino.huga.data.prefs.AppSettings
import com.soogoino.huga.data.prefs.ThemeMode
import com.soogoino.huga.ui.navigation.HugaNavGraph
import com.soogoino.huga.ui.navigation.Screen
import com.soogoino.huga.ui.theme.HugaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: AppPreferences

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
                        startDest = if (settings.isRepoSetup) Screen.Posts.route else Screen.Setup.route
                    }

                    startDest?.let { dest ->
                        HugaNavGraph(navController = navController, startDestination = dest)
                    }
                }
            }
        }
    }
}
