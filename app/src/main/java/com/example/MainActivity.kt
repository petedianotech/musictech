package com.example

import android.Manifest
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.repository.SettingsRepository
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.data.db.MusicDatabase
import com.example.data.repository.MediaRepository
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.MusicViewModelFactory

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = this
            val settingsRepository = remember { SettingsRepository(context) }
            val themeMode by settingsRepository.themeMode.collectAsState(initial = 0)
            val showLockscreenArt by settingsRepository.showLockscreenArt.collectAsState(initial = true)

            LaunchedEffect(showLockscreenArt) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(showLockscreenArt)
                    setTurnScreenOn(showLockscreenArt)
                } else {
                    @Suppress("DEPRECATION")
                    if (showLockscreenArt) {
                        window.addFlags(
                            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    } else {
                        window.clearFlags(
                            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    }
                }
            }
            
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permissionState = rememberMultiplePermissionsState(permissions)

                LaunchedEffect(Unit) {
                    permissionState.launchMultiplePermissionRequest()
                }

                if (permissionState.allPermissionsGranted) {
                    val database = MusicDatabase.getDatabase(context)
                    val repository = MediaRepository(context)
                    
                    val musicViewModel: MusicViewModel = viewModel(
                        factory = MusicViewModelFactory(context, repository, database, settingsRepository)
                    )
                    
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        viewModel = musicViewModel,
                        settingsRepository = settingsRepository
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Permissions required to access music.")
                    }
                }
            }
        }
    }
}
