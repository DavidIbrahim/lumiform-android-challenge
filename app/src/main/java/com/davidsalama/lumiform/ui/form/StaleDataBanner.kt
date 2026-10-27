package com.davidsalama.lumiform.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.davidsalama.lumiform.R
import com.davidsalama.lumiform.domain.DataError
import java.time.Instant
import java.time.ZoneId

/**
 * Shown above cached content when the last refresh failed: why, and how old the data is.
 * The content stays usable underneath; this never blocks.
 */
@Composable
fun StaleDataBanner(
    error: DataError,
    lastSyncedAt: Instant?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val syncedText = lastSyncedAt?.let { syncedAt ->
        val time = remember(syncedAt, locale) { formatSyncTime(syncedAt, Instant.now(), ZoneId.systemDefault(), locale) }
        stringResource(R.string.banner_showing_since, time)
    }
    val reason = error.bannerReason()
    val message = if (syncedText == null) reason else stringResource(R.string.banner_combined, reason, syncedText)

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        // Read out by TalkBack when it appears, without stealing focus.
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun DataError.bannerReason(): String = when (this) {
    DataError.NoConnection -> stringResource(R.string.banner_no_connection)
    DataError.Timeout -> stringResource(R.string.banner_timeout)
    is DataError.Server -> stringResource(R.string.banner_server, code)
    DataError.InvalidData -> stringResource(R.string.banner_invalid_data)
    DataError.Unknown -> stringResource(R.string.banner_unknown)
}
