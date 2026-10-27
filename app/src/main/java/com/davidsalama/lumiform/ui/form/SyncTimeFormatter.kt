package com.davidsalama.lumiform.ui.form

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * "10:42" when the sync was today, otherwise date and time ("25 Oct 2026, 10:42"),
 * in the user's time zone and locale.
 */
fun formatSyncTime(syncedAt: Instant, now: Instant, zone: ZoneId, locale: Locale): String {
    val synced = syncedAt.atZone(zone)
    val formatter = if (synced.toLocalDate() == now.atZone(zone).toLocalDate()) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    return formatter.withLocale(locale).format(synced)
}
