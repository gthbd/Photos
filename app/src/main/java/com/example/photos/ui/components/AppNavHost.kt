package com.example.photos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.photos.ui.screens.MainScreen
import kotlinx.serialization.Serializable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photos.ui.screens.ViewerScreen

@Serializable
data object GridRoute

// Truyền id thay vì vị trí vì refresh() có thể chèn mục mới vào đầu danh sách làm lệch vị trí
@Serializable
data class ViewerRoute(val mediaId: Long)

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: MediaViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    NavHost(navController = navController, startDestination = GridRoute) {
        composable<GridRoute> { MainScreen(
            uiState = uiState,
            onMediaClick = { mediaId ->
                navController.navigate(ViewerRoute(mediaId)) { launchSingleTop = true }
            },
            modifier = modifier,
        ) }
        composable<ViewerRoute> { backStackEntry ->
            ViewerScreen(
                uiState = uiState,
                mediaId = backStackEntry.toRoute<ViewerRoute>().mediaId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}