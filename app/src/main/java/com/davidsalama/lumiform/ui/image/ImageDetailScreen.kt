package com.davidsalama.lumiform.ui.image

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidsalama.lumiform.R
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.ui.navigation.sharedImage
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun ImageDetailRoute(
    onBack: () -> Unit,
    viewModel: ImageDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImageDetailScreen(imageId = viewModel.imageId, state = state, onBack = onBack)
}

/**
 * An immersive, always-dark viewer: the image fills the screen, with a floating back button
 * and a caption that a tap hides or shows. Pinch or double tap to zoom, swipe down to close.
 */
@Composable
fun ImageDetailScreen(imageId: Long, state: ImageDetailUiState, onBack: () -> Unit) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    // While a swipe-to-close is in progress the controls fade with it.
    var dismissProgress by remember { mutableFloatStateOf(0f) }
    val showControls = controlsVisible || state !is ImageDetailUiState.Loaded

    ViewerSystemBars(visible = showControls)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (state) {
            ImageDetailUiState.Loading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            is ImageDetailUiState.Loaded -> ZoomableImage(
                imageId = imageId,
                url = state.image.src,
                contentDescription = state.image.title,
                controlsVisible = controlsVisible,
                onToggleControls = { controlsVisible = !controlsVisible },
                onDismissProgress = { dismissProgress = it },
                onDismiss = onBack,
            )
            ImageDetailUiState.NotFound -> Text(
                text = stringResource(R.string.image_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - dismissProgress },
        ) {
            Box(Modifier.fillMaxSize()) {
                BackButton(
                    onBack = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp),
                )
                if (state is ImageDetailUiState.Loaded) {
                    Caption(state.image, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White, modifier = modifier) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }
}

/** The title over a soft gradient, the way photo apps caption a picture. */
@Composable
private fun Caption(image: ImageQuestion, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 20.dp),
    ) {
        Text(
            text = image.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "#${image.id}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

private val CanvasShape = RoundedCornerShape(8.dp)

/**
 * The image on its light canvas, sized to the image's own shape. One finger drags it down to
 * close (when not zoomed), two fingers zoom; a tap toggles the controls, a double tap zooms.
 */
@Composable
private fun ZoomableImage(
    imageId: Long,
    url: String,
    contentDescription: String,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onDismissProgress: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var zoom by remember(url) { mutableStateOf(ZoomState()) }
    // Square until the image's real shape is known (from the thumbnail placeholder, then the image).
    var aspectRatio by remember(url) { mutableFloatStateOf(1f) }
    var height by remember { mutableFloatStateOf(0f) }
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(dragY) {
        snapshotFlow { SwipeToDismiss.progress(dragY.value, height) }.collect(onDismissProgress)
    }

    val toggleLabel = stringResource(if (controlsVisible) R.string.hide_controls else R.string.show_controls)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            // A zoomed image stays inside the screen instead of drawing over the system bars.
            .clipToBounds()
            .onSizeChanged { height = it.height.toFloat() }
            .semantics { onClick(label = toggleLabel) { onToggleControls(); true } }
            .pointerInput(url) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { tap -> zoom = zoom.toggle(tap, size.toSize()) },
                )
            }
            .pointerInput(url) {
                detectZoomOrDismiss(
                    isZoomed = { zoom.isZoomed },
                    onTransform = { centroid, pan, gestureZoom ->
                        zoom = zoom.transform(centroid, pan, gestureZoom, size.toSize())
                    },
                    onDismissDrag = { dy -> scope.launch { dragY.snapTo(dragY.value + dy) } },
                    onDismissEnd = { velocityY ->
                        if (SwipeToDismiss.shouldDismiss(dragY.value, velocityY, height)) {
                            onDismiss()
                        } else {
                            scope.launch { dragY.animateTo(0f) }
                        }
                    },
                )
            }
            .graphicsLayer {
                val dismissScale = SwipeToDismiss.scale(dragY.value, height)
                scaleX = zoom.scale * dismissScale
                scaleY = zoom.scale * dismissScale
                translationX = zoom.offset.x
                translationY = zoom.offset.y + dragY.value
            }
            .padding(16.dp),
    ) {
        FormImage(
            url = url,
            contentDescription = contentDescription,
            contentPadding = 16.dp,
            onImageSize = { size -> if (size.width > 0 && size.height > 0) aspectRatio = size.width / size.height },
            // The thumbnail is already in memory: show it straight away while the full image loads.
            placeholderMemoryCacheKey = thumbnailCacheKey(url),
            modifier = Modifier
                .sharedImage(imageId)
                .aspectRatio(aspectRatio)
                .clip(CanvasShape),
        )
    }
}

/**
 * Decides per gesture, once the finger has moved past touch slop: two fingers (or any drag of
 * a zoomed image) zoom and pan; a mostly vertical one-finger drag of an unzoomed image is a
 * swipe to close. Movements under the slop aren't consumed, so taps still reach the tap detector.
 */
private suspend fun PointerInputScope.detectZoomOrDismiss(
    isZoomed: () -> Boolean,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onDismissDrag: (dy: Float) -> Unit,
    onDismissEnd: (velocityY: Float) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var mode = GestureMode.Undecided
        var totalPan = Offset.Zero
        val velocity = VelocityTracker()
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) break
            val pan = event.calculatePan()
            val gestureZoom = event.calculateZoom()
            if (mode == GestureMode.Undecided) {
                totalPan += pan
                mode = when {
                    event.changes.size > 1 -> GestureMode.Transform
                    totalPan.getDistance() < viewConfiguration.touchSlop -> GestureMode.Undecided
                    !isZoomed() && totalPan.y.absoluteValue > totalPan.x.absoluteValue -> GestureMode.Dismiss
                    else -> GestureMode.Transform
                }
            }
            when (mode) {
                GestureMode.Undecided -> Unit
                GestureMode.Transform -> onTransform(event.calculateCentroid(), pan, gestureZoom)
                GestureMode.Dismiss -> {
                    val change = event.changes.first()
                    velocity.addPosition(change.uptimeMillis, change.position)
                    onDismissDrag(pan.y)
                }
            }
            if (mode != GestureMode.Undecided) {
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (mode == GestureMode.Dismiss) onDismissEnd(velocity.calculateVelocity().y)
    }
}

private enum class GestureMode { Undecided, Transform, Dismiss }

/**
 * Light icons on the black screen, and the system bars hide together with the controls
 * (a swipe from the edge brings them back). Everything is restored when the viewer closes.
 */
@Composable
private fun ViewerSystemBars(visible: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = remember(view) { view.context.findActivity()?.window } ?: return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    DisposableEffect(controller) {
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        // Edge-to-edge gives three-button navigation a light scrim; the viewer wants it clear.
        @Suppress("DEPRECATION")
        val navigationBarColor = window.navigationBarColor
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val contrastEnforced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && window.isNavigationBarContrastEnforced
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
            @Suppress("DEPRECATION")
            window.navigationBarColor = navigationBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = contrastEnforced
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(controller, visible) {
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
