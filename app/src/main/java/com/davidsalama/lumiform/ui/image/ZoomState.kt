package com.davidsalama.lumiform.ui.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Zoom and pan of the full-screen image, applied with `graphicsLayer` around the centre.
 * Pure, so the gesture maths is unit tested without touch events.
 */
@Immutable
data class ZoomState(val scale: Float = MIN_SCALE, val offset: Offset = Offset.Zero) {

    val isZoomed: Boolean get() = scale > MIN_SCALE

    /**
     * Applies one pinch/pan step. The content point under [centroid] stays under the fingers
     * (plus [pan]), and the image can't be dragged past its own edges.
     */
    fun transform(centroid: Offset, pan: Offset, zoom: Float, size: Size): ZoomState {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val appliedZoom = newScale / scale
        val fromCentre = centroid - size.center
        val newOffset = fromCentre * (1 - appliedZoom) + offset * appliedZoom + pan
        return ZoomState(newScale, newOffset.clampedTo(size, newScale))
    }

    /** Double tap: zoom in on the tapped point, or back out if already zoomed. */
    fun toggle(tap: Offset, size: Size): ZoomState =
        if (isZoomed) ZoomState() else ZoomState().transform(tap, Offset.Zero, DOUBLE_TAP_SCALE, size)

    private fun Offset.clampedTo(size: Size, scale: Float): Offset {
        val maxX = size.width * (scale - 1) / 2
        val maxY = size.height * (scale - 1) / 2
        return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2.5f
    }
}

private val Size.center: Offset get() = Offset(width / 2, height / 2)
