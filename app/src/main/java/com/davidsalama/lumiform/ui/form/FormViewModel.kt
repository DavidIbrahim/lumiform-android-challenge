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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    private val repository: FormRepository,
    private val connectivity: ConnectivityObserver,
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
            val error = (repository.refresh() as? RefreshResult.Failed)?.error
            refreshState.value = RefreshState(isRefreshing = false, error = error)
        }
    }

    fun onResponseTapped(question: ChoiceQuestion, responseId: Long) {
        viewModelScope.launch { repository.toggleResponse(question, responseId) }
    }
}
