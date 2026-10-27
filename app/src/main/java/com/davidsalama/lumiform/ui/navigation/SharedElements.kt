package com.davidsalama.lumiform.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Set by the NavHost so a screen's image can fly between destinations. Null outside the
 * NavHost (previews, single-screen tests), where [sharedImage] simply does nothing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** The form's thumbnail and the viewer's image share this key, so one grows into the other. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedImage(imageId: Long): Modifier {
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(transitionScope) {
        this@sharedImage.sharedElement(
            sharedContentState = rememberSharedContentState(key = "image-$imageId"),
            animatedVisibilityScope = visibilityScope,
        )
    }
}
