package com.davidsalama.lumiform.ui.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class SyncTimeFormatterTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val now = Instant.parse("2026-10-27T15:00:00Z")

    @Test
    fun `sync earlier today shows only the time, in the user's zone`() {
        // 08:42 UTC is 09:42 in Berlin (CET, after the October clock change).
        val text = formatSyncTime(Instant.parse("2026-10-27T08:42:00Z"), now, berlin, Locale.UK)

        assertEquals("09:42", text)
    }

    @Test
    fun `sync on an earlier day also shows the date`() {
        // Clocks went back on 25 Oct 2026, so 08:42 UTC is 09:42 in Berlin that day.
        val text = formatSyncTime(Instant.parse("2026-10-25T08:42:00Z"), now, berlin, Locale.UK)

        assertTrue(text, text.contains("25") && text.contains("Oct") && text.contains("09:42"))
    }

    @Test
    fun `today is decided in the user's zone, not UTC`() {
        // 23:30 UTC on the 26th is already 00:30 on the 27th in Berlin, so it's "today".
        val text = formatSyncTime(Instant.parse("2026-10-26T23:30:00Z"), now, berlin, Locale.UK)

        assertEquals("00:30", text)
    }
}
