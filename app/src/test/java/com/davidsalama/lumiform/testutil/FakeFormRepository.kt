package com.davidsalama.lumiform.testutil

import com.davidsalama.lumiform.domain.FormRepository
import com.davidsalama.lumiform.domain.RefreshResult
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/** In-memory repository: a successful refresh "downloads" [remoteForm] into the cache. */
class FakeFormRepository(
    cachedForm: List<FormNode> = emptyList(),
    var remoteForm: List<FormNode> = sampleFormTree,
    var refreshResult: RefreshResult = RefreshResult.Success,
) : FormRepository {

    val form = MutableStateFlow(cachedForm)
    val lastSyncedAt = MutableStateFlow<Instant?>(null)
    val selections = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())

    var refreshCount = 0
        private set

    /** While set, refresh() suspends until [releaseRefresh] is called. */
    private var gate: CompletableDeferred<Unit>? = null

    fun holdRefresh() {
        gate = CompletableDeferred()
    }

    fun releaseRefresh() {
        gate?.complete(Unit)
        gate = null
    }

    override fun observeForm(): Flow<List<FormNode>> = form
    override fun observeLastSyncedAt(): Flow<Instant?> = lastSyncedAt
    override fun observeSelections(): Flow<Map<Long, Set<Long>>> = selections

    override suspend fun refresh(): RefreshResult {
        refreshCount++
        gate?.await()
        if (refreshResult == RefreshResult.Success) form.value = remoteForm
        return refreshResult
    }

    override suspend fun toggleResponse(question: ChoiceQuestion, responseId: Long) {
        selections.update { all ->
            val updated = question.responseSet.toggle(all[question.id].orEmpty(), responseId)
            if (updated.isEmpty()) all - question.id else all + (question.id to updated)
        }
    }
}
