package com.davidsalama.lumiform.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LumiformNavHost(navController: NavHostController = rememberNavController()) {
    // Lets the tapped thumbnail grow into the full-screen image, and shrink back on close.
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = FormDestination) {
                composable<FormDestination> { entry ->
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        FormRoute(
                            onImageClick = { imageId ->
                                // Ignores a second tap that lands while the transition is already running.
                                if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                                    navController.navigate(ImageDetailDestination(imageId))
                                }
                            },
                        )
                    }
                }
                composable<ImageDetailDestination> {
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        ImageDetailRoute(onBack = navController::navigateUp)
                    }
                }
            }
        }
    }
}
