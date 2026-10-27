package com.davidsalama.lumiform.ui.image

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidsalama.lumiform.R

@Composable
fun ImageDetailRoute(
    onBack: () -> Unit,
    viewModel: ImageDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImageDetailScreen(state = state, onBack = onBack)
}

/** Always dark, like a photo viewer, whatever the app theme: the image is the only thing to look at. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(state: ImageDetailUiState, onBack: () -> Unit) {
    LightSystemBarIcons()
    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    if (state is ImageDetailUiState.Loaded) {
                        Text(state.image.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (state) {
            ImageDetailUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            is ImageDetailUiState.Loaded -> ZoomableImage(
                url = state.image.src,
                contentDescription = state.image.title,
                modifier = contentModifier,
            )
            ImageDetailUiState.NotFound -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.image_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val CanvasShape = RoundedCornerShape(4.dp)

/**
 * The image on its light canvas, sized to the image's own shape so the canvas doesn't fill
 * the black screen. Pinch to zoom (up to 5x), drag to pan, double tap to zoom in or out.
 */
@Composable
private fun ZoomableImage(url: String, contentDescription: String, modifier: Modifier = Modifier) {
    var zoom by remember(url) { mutableStateOf(ZoomState()) }
    // Square until the image has loaded and its real shape is known.
    var aspectRatio by remember(url) { mutableFloatStateOf(1f) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // A zoomed image stays inside its area instead of drawing over the bars.
            .clipToBounds()
            .pointerInput(url) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    zoom = zoom.transform(centroid, pan, gestureZoom, size.toSize())
                }
            }
            .pointerInput(url) {
                detectTapGestures(onDoubleTap = { tap -> zoom = zoom.toggle(tap, size.toSize()) })
            }
            .graphicsLayer {
                scaleX = zoom.scale
                scaleY = zoom.scale
                translationX = zoom.offset.x
                translationY = zoom.offset.y
            }
            .padding(16.dp),
    ) {
        FormImage(
            url = url,
            contentDescription = contentDescription,
            contentPadding = 16.dp,
            onImageSize = { size -> if (size.width > 0 && size.height > 0) aspectRatio = size.width / size.height },
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .clip(CanvasShape),
        )
    }
}

/** Light status and navigation bar icons on the black screen; the previous style comes back on leave. */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
