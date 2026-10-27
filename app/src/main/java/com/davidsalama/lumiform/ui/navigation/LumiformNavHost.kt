package com.davidsalama.lumiform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.davidsalama.lumiform.ui.form.FormRoute
import com.davidsalama.lumiform.ui.image.ImageDetailRoute
import kotlinx.serialization.Serializable

@Serializable
data object FormDestination

@Serializable
data class ImageDetailDestination(val imageId: Long)

@Composable
fun LumiformNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = FormDestination) {
        composable<FormDestination> { entry ->
            FormRoute(
                onImageClick = { imageId ->
                    // Ignores a second tap that lands while the transition is already running.
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(ImageDetailDestination(imageId))
                    }
                },
            )
        }
        composable<ImageDetailDestination> {
            ImageDetailRoute(onBack = navController::navigateUp)
        }
    }
}
