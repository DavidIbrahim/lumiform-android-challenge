package com.davidsalama.lumiform.ui.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeToDismissTest {

    private val height = 2000f
    private val threshold = height * SwipeToDismiss.DISTANCE_FRACTION // 360px

    @Test
    fun `a short slow drag springs back`() {
        assertFalse(SwipeToDismiss.shouldDismiss(dragY = threshold - 1, velocityY = 100f, height = height))
    }

    @Test
    fun `dragging past the threshold closes, up or down`() {
        assertTrue(SwipeToDismiss.shouldDismiss(dragY = threshold, velocityY = 0f, height = height))
        assertTrue(SwipeToDismiss.shouldDismiss(dragY = -threshold, velocityY = 0f, height = height))
    }

    @Test
    fun `a quick flick closes even when short`() {
        assertTrue(SwipeToDismiss.shouldDismiss(dragY = 50f, velocityY = 2_500f, height = height))
    }

    @Test
    fun `flicking back towards the centre doesn't close`() {
        assertFalse(SwipeToDismiss.shouldDismiss(dragY = 50f, velocityY = -2_500f, height = height))
    }

    @Test
    fun `progress and shrink follow the drag and stop at the threshold`() {
        assertEquals(0f, SwipeToDismiss.progress(0f, height))
        assertEquals(0.5f, SwipeToDismiss.progress(threshold / 2, height))
        assertEquals(1f, SwipeToDismiss.progress(threshold * 3, height))

        assertEquals(1f, SwipeToDismiss.scale(0f, height))
        assertEquals(1f - SwipeToDismiss.MAX_SHRINK, SwipeToDismiss.scale(threshold * 3, height))
    }

    @Test
    fun `before the screen is measured nothing closes`() {
        assertFalse(SwipeToDismiss.shouldDismiss(dragY = 500f, velocityY = 5_000f, height = 0f))
        assertEquals(0f, SwipeToDismiss.progress(500f, 0f))
    }
}
