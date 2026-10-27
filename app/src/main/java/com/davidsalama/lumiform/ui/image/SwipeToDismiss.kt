package com.davidsalama.lumiform.ui.image

import kotlin.math.absoluteValue

/**
 * Swipe-down-to-close rules for the image viewer, kept pure so they're unit tested.
 * Dragging either way works; a long enough drag or a quick flick closes the viewer.
 */
object SwipeToDismiss {

    /** Share of the screen height to drag before letting go closes the viewer. */
    const val DISTANCE_FRACTION = 0.18f

    /** A flick at least this fast (px/s) closes it even when the drag was short. */
    const val FLING_VELOCITY = 1_800f

    /** How much the image shrinks at the full drag distance, a hint that letting go will close it. */
    const val MAX_SHRINK = 0.15f

    fun shouldDismiss(dragY: Float, velocityY: Float, height: Float): Boolean {
        if (height <= 0f) return false
        val farEnough = dragY.absoluteValue >= height * DISTANCE_FRACTION
        // A flick only counts when it goes the same way as the drag, not when flicking back.
        val flicked = velocityY.absoluteValue >= FLING_VELOCITY && dragY * velocityY > 0
        return farEnough || flicked
    }

    /** 0 at rest, 1 once the drag reaches the dismiss distance. */
    fun progress(dragY: Float, height: Float): Float =
        if (height <= 0f) 0f else (dragY.absoluteValue / (height * DISTANCE_FRACTION)).coerceIn(0f, 1f)

    fun scale(dragY: Float, height: Float): Float = 1f - MAX_SHRINK * progress(dragY, height)
}
