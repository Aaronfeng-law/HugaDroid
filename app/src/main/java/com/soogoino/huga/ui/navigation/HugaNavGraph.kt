package com.soogoino.huga.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soogoino.huga.ui.editor.EditorScreen
import com.soogoino.huga.ui.posts.PostsScreen
import com.soogoino.huga.ui.settings.SettingsScreen
import com.soogoino.huga.ui.sync.SetupScreen
import com.soogoino.huga.ui.sync.SyncScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Posts : Screen("posts")
    object Editor : Screen("editor/{filePath}") {
        fun createRoute(filePath: String): String =
            "editor/${URLEncoder.encode(filePath, "UTF-8")}"
    }
    object Sync : Screen("sync")
    object Setup : Screen("setup")
    object Settings : Screen("settings")
}

@Composable
fun HugaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Posts.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
    ) {
        composable(Screen.Posts.route) {
            PostsScreen(
                onOpenPost = { filePath ->
                    navController.navigate(Screen.Editor.createRoute(filePath))
                },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType }),
        ) { backStack ->
            val encoded = backStack.arguments?.getString("filePath") ?: ""
            val filePath = URLDecoder.decode(encoded, "UTF-8")
            EditorScreen(
                filePath = filePath,
                onNavigateUp = { navController.popBackStack() },
            )
        }

        composable(Screen.Sync.route) {
            SyncScreen(
                onNavigateUp = { navController.popBackStack() },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Posts.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
                onNavigateUp = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateUp = { navController.popBackStack() })
        }
    }
}
