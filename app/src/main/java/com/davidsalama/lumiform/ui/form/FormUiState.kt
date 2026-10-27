package com.davidsalama.lumiform.ui.form

import com.davidsalama.lumiform.domain.DataError
import java.time.Instant

data class FormUiState(
    val rows: List<FormRow> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastSyncedAt: Instant? = null,
    /** The error of the last refresh, or null if it succeeded or is still running. */
    val error: DataError? = null,
) {
    /** Nothing cached yet and the first fetch is still running. */
    val isInitialLoading: Boolean get() = rows.isEmpty() && isRefreshing

    /** Nothing cached and the fetch failed: the error takes the whole screen. */
    val isBlockingError: Boolean get() = rows.isEmpty() && !isRefreshing && error != null
}
