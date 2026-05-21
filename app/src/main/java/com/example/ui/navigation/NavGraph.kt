package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.viewmodel.MusicViewModel
import com.example.data.repository.SettingsRepository

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: MusicViewModel,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "library",
        modifier = modifier
    ) {
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateToPlaylist = { id -> navController.navigate("playlist/$id") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToVideoPlayer = { uri -> 
                    navController.navigate("video_player/${java.net.URLEncoder.encode(uri, "UTF-8")}") 
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("video_player/{videoUri}") { backStackEntry ->
            val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            VideoPlayerScreen(
                videoUri = videoUri,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "player",
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300))
            }
        ) {
            PlayerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEqualizer = { navController.navigate("equalizer") }
            )
        }
        composable("equalizer") {
            EqualizerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("playlist/{playlistId}") { backStackEntry ->
            val playlistIdStr = backStackEntry.arguments?.getString("playlistId")
            val playlistId = playlistIdStr?.toIntOrNull() ?: 0
            PlaylistDetailScreen(
                playlistId = playlistId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") }
            )
        }
    }
}
