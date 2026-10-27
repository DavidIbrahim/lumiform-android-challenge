package com.davidsalama.lumiform.ui.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomStateTest {

    private val size = Size(1000f, 2000f)
    private val centre = Offset(500f, 1000f)

    /** Where a content point ends up on screen under [state] (graphicsLayer scales around the centre). */
    private fun screenPosition(contentPoint: Offset, state: ZoomState): Offset =
        centre + (contentPoint - centre) * state.scale + state.offset

    @Test
    fun `pinching keeps the point under the fingers in place`() {
        val fingers = Offset(700f, 1200f)

        val zoomed = ZoomState().transform(centroid = fingers, pan = Offset.Zero, zoom = 2f, size = size)

        assertEquals(2f, zoomed.scale)
        assertEquals(fingers, screenPosition(fingers, zoomed))
    }

    @Test
    fun `scale stays between 1x and 5x`() {
        val zoomedIn = ZoomState().transform(centre, Offset.Zero, zoom = 20f, size = size)
        val zoomedOut = ZoomState(scale = 2f).transform(centre, Offset.Zero, zoom = 0.1f, size = size)

        assertEquals(ZoomState.MAX_SCALE, zoomedIn.scale)
        assertEquals(ZoomState.MIN_SCALE, zoomedOut.scale)
        assertEquals(Offset.Zero, zoomedOut.offset)
    }

    @Test
    fun `panning stops at the image's edges`() {
        // At 2x the image is 2000 wide on a 1000 wide screen: 500 of slack on each side.
        val panned = ZoomState(scale = 2f).transform(centre, pan = Offset(10_000f, -10_000f), zoom = 1f, size = size)

        assertEquals(Offset(500f, -1000f), panned.offset)
    }

    @Test
    fun `an image that isn't zoomed can't be dragged`() {
        val dragged = ZoomState().transform(centre, pan = Offset(300f, 300f), zoom = 1f, size = size)

        assertEquals(ZoomState(), dragged)
    }

    @Test
    fun `double tap zooms in on the tapped point, and a second double tap zooms back out`() {
        val tap = Offset(600f, 1100f)

        val zoomedIn = ZoomState().toggle(tap, size)
        assertTrue(zoomedIn.isZoomed)
        assertEquals(ZoomState.DOUBLE_TAP_SCALE, zoomedIn.scale)
        assertEquals(tap, screenPosition(tap, zoomedIn))

        val zoomedOut = zoomedIn.toggle(tap, size)
        assertFalse(zoomedOut.isZoomed)
        assertEquals(ZoomState(), zoomedOut)
    }
}
