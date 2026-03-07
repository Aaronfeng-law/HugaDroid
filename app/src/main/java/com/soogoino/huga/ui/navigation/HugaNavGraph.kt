package com.soogoino.huga.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soogoino.huga.ui.editor.EditorScreen
import com.soogoino.huga.ui.files.FilesScreen
import com.soogoino.huga.ui.home.HomeScreen
import com.soogoino.huga.ui.posts.PostsScreen
import com.soogoino.huga.ui.settings.SettingsScreen
import com.soogoino.huga.ui.sync.SetupScreen
import com.soogoino.huga.ui.sync.SyncScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Posts : Screen("posts")
    object Editor : Screen("editor/{filePath}") {
        fun createRoute(filePath: String): String =
            "editor/${URLEncoder.encode(filePath, "UTF-8")}"
    }
    object Files : Screen("files")
    object Sync : Screen("sync")
    object Setup : Screen("setup")
    object Settings : Screen("settings")
}

/** Tab 的左右順序，用來判斷滑動方向 */
private val TAB_ORDER = mapOf(
    Screen.Home.route to 0,
    Screen.Posts.route to 1,
    Screen.Files.route to 2,
)

private fun tabIndexOf(route: String?) = TAB_ORDER[route] ?: -1

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(Screen.Home.route) { saveState = true }
    }
}

@Composable
fun HugaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // 非 Tab 畫面的全域預設（push/pop 風格）
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
    ) {
        composable(
            route = Screen.Home.route,
            enterTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (from >= 0 && to >= 0) {
                    // Tab 互切：依索引決定方向
                    slideInHorizontally(initialOffsetX = { if (to > from) it else -it }) + fadeIn()
                } else {
                    // 從非 Tab 畫面 pop 回來
                    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
                }
            },
            exitTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (from >= 0 && to >= 0) {
                    slideOutHorizontally(targetOffsetX = { if (to > from) -it / 3 else it / 3 }) + fadeOut()
                } else {
                    // push 到非 Tab 畫面
                    slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                }
            },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
        ) {
            HomeScreen(
                onNavigateToPosts = { navController.navigateTab(Screen.Posts.route) },
                onNavigateToFiles = { navController.navigateTab(Screen.Files.route) },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
                onOpenPost = { filePath -> navController.navigate(Screen.Editor.createRoute(filePath)) },
            )
        }

        composable(
            route = Screen.Posts.route,
            enterTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (from >= 0 && to >= 0) {
                    slideInHorizontally(initialOffsetX = { if (to > from) it else -it }) + fadeIn()
                } else {
                    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
                }
            },
            exitTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (from >= 0 && to >= 0) {
                    slideOutHorizontally(targetOffsetX = { if (to > from) -it / 3 else it / 3 }) + fadeOut()
                } else {
                    slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                }
            },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
        ) {
            PostsScreen(
                onOpenPost = { filePath ->
                    navController.navigate(Screen.Editor.createRoute(filePath))
                },
                onNavigateToHome = { navController.navigateTab(Screen.Home.route) },
                onNavigateToSync = { navController.navigate(Screen.Sync.route) },
                onNavigateToFiles = { navController.navigateTab(Screen.Files.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
            )
        }

        composable(
            route = Screen.Files.route,
            enterTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (from >= 0 && to >= 0) {
                    slideInHorizontally(initialOffsetX = { if (to > from) it else -it }) + fadeIn()
                } else {
                    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
                }
            },
            exitTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (from >= 0 && to >= 0) {
                    slideOutHorizontally(targetOffsetX = { if (to > from) -it / 3 else it / 3 }) + fadeOut()
                } else {
                    slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                }
            },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
        ) {
            FilesScreen(
                onNavigateToHome = { navController.navigateTab(Screen.Home.route) },
                onNavigateToPosts = { navController.navigateTab(Screen.Posts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
                onOpenFile = { filePath ->
                    navController.navigate(Screen.Editor.createRoute(filePath))
                },
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
                    navController.navigate(Screen.Home.route) {
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
