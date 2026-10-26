package com.davidsalama.lumiform.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

// Ids and labels mirror question 13 (single) and 14 (multi) of the real form.
class ResponseSetToggleTest {

    private val single = ResponseSet(
        id = 101,
        multipleSelection = false,
        responses = listOf(
            Response(1011, "Safety procedures", score = 1),
            Response(1012, "Equipment checks", score = 2),
            Response(1013, "Not applicable", score = null),
        ),
    )

    private val multi = ResponseSet(
        id = 102,
        multipleSelection = true,
        responses = listOf(
            Response(1021, "Entrance", score = null),
            Response(1022, "Storage", score = null),
            Response(1023, "Loading bay", score = null),
        ),
    )

    @Test
    fun `single - tapping with nothing selected selects it`() {
        assertEquals(setOf(1011L), single.toggle(emptySet(), 1011))
    }

    @Test
    fun `single - tapping another response replaces the selection`() {
        assertEquals(setOf(1012L), single.toggle(setOf(1011), 1012))
    }

    @Test
    fun `single - tapping the selected response clears it`() {
        assertEquals(emptySet<Long>(), single.toggle(setOf(1011), 1011))
    }

    @Test
    fun `multi - tapping adds to the selection`() {
        assertEquals(setOf(1021L, 1022L), multi.toggle(setOf(1021), 1022))
    }

    @Test
    fun `multi - tapping a selected response removes only that one`() {
        assertEquals(setOf(1023L), multi.toggle(setOf(1021, 1023), 1021))
    }

    @Test
    fun `multi - all responses can be selected at once`() {
        val all = listOf(1021L, 1022L, 1023L).fold(emptySet<Long>(), multi::toggle)
        assertEquals(setOf(1021L, 1022L, 1023L), all)
    }

    @Test
    fun `an id from another response set leaves the selection unchanged`() {
        assertEquals(setOf(1011L), single.toggle(setOf(1011), 1021))
        assertEquals(setOf(1021L), multi.toggle(setOf(1021), 1011))
    }
}
