package com.davidsalama.lumiform.domain

import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Offline-first access to the form.
 *
 * The local database is the single source of truth: the observe functions only ever read
 * the cache, and [refresh] is the only thing that talks to the network, by writing into it.
 */
interface FormRepository {

    /** The cached form; empty until a first refresh succeeds. */
    fun observeForm(): Flow<List<FormNode>>

    /** When the cached form was last fetched, or null if it never was. */
    fun observeLastSyncedAt(): Flow<Instant?>

    /** Selected response ids, by question id. Questions without a selection are absent. */
    fun observeSelections(): Flow<Map<Long, Set<Long>>>

    /** Fetches the form and replaces the cache. On failure the cache is left as it was. */
    suspend fun refresh(): RefreshResult

    /** Applies a tap on [responseId], respecting the question's multiple-selection rule. */
    suspend fun toggleResponse(question: ChoiceQuestion, responseId: Long)
}

sealed interface RefreshResult {
    data object Success : RefreshResult
    data class Failed(val error: DataError) : RefreshResult
}

/** Why a refresh failed, in terms the UI can show. Raw exceptions never leave the data layer. */
sealed interface DataError {
    /** No route to the server: offline, DNS failure, connection refused or reset. */
    data object NoConnection : DataError
    data object Timeout : DataError
    data class Server(val code: Int) : DataError
    /** The response arrived but couldn't be understood. Retrying won't help. */
    data object InvalidData : DataError
    data object Unknown : DataError
}
