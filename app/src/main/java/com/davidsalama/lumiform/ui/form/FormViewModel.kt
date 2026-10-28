package com.davidsalama.lumiform.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidsalama.lumiform.domain.ConnectivityObserver
import com.davidsalama.lumiform.domain.DataError
import com.davidsalama.lumiform.domain.FormRepository
import com.davidsalama.lumiform.domain.RefreshResult
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    private val repository: FormRepository,
    private val connectivity: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private data class RefreshState(val isRefreshing: Boolean, val error: DataError?)

    private val refreshState = MutableStateFlow(RefreshState(isRefreshing = true, error = null))
    private var refreshJob: Job? = null

    /** Cached data and refresh status merged into one state; the cache shows as soon as it's read. */
    val uiState: StateFlow<FormUiState> = combine(
        repository.observeForm(),
        repository.observeSelections(),
        repository.observeLastSyncedAt(),
        refreshState,
    ) { form, selections, lastSyncedAt, refresh ->
        FormUiState(
            rows = flattenForm(form, selections),
            isRefreshing = refresh.isRefreshing,
            lastSyncedAt = lastSyncedAt,
            error = refresh.error,
        )
    }.stateIn(
        scope = viewModelScope,
        // Survives a rotation without restarting the upstream DB queries.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FormUiState(isRefreshing = true),
    )

    init {
        refresh()
        retryWhenBackOnline()
    }

    /** After a failed refresh, try again as soon as the device is back online, without a tap. */
    private fun retryWhenBackOnline() {
        viewModelScope.launch {
            connectivity.isOnline
                // The first value is the current state, not a change; only offline -> online counts.
                .drop(1)
                .filter { online -> online && refreshState.value.error != null }
                .collect { refresh() }
        }
    }

    /** Pull-to-refresh and Retry. A refresh already in flight isn't started twice. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshState.value = refreshState.value.copy(isRefreshing = true)
            val error: DataError? = (repository.refresh() as? RefreshResult.Failed)?.error
            refreshState.value = RefreshState(isRefreshing = false, error = error)
        }
    }

    /**
     * Called each time the screen comes to the foreground. The ViewModel outlives a trip to the
     * background, so `init` doesn't run again; this refreshes only if the cache is older than
     * [STALE_AFTER], so quick app switches cost nothing. On first start the refresh from `init`
     * is still running, and [refresh] ignores the duplicate.
     */
    fun onScreenStarted() {
        viewModelScope.launch {
            // Read from the database, not uiState: its upstream is stopped while in the background.
            val lastSyncedAt = repository.observeLastSyncedAt().first()
            if (lastSyncedAt == null || Duration.between(lastSyncedAt, clock.instant()) >= STALE_AFTER) {
                refresh()
            }
        }
    }

    fun onResponseTapped(question: ChoiceQuestion, responseId: Long) {
        viewModelScope.launch { repository.toggleResponse(question, responseId) }
    }

    companion object {
        /** How old the cache may get before coming back to the screen triggers a refresh. */
        val STALE_AFTER: Duration = Duration.ofMinutes(5)
    }
}
