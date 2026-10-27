package com.davidsalama.lumiform.data

import com.davidsalama.lumiform.data.local.dao.FormDao
import com.davidsalama.lumiform.data.local.dao.SelectionDao
import com.davidsalama.lumiform.data.mapper.toDomainTree
import com.davidsalama.lumiform.data.mapper.toEntities
import com.davidsalama.lumiform.data.mapper.toImageQuestionOrNull
import com.davidsalama.lumiform.data.remote.FormRemoteDataSource
import com.davidsalama.lumiform.domain.FormRepository
import com.davidsalama.lumiform.domain.RefreshResult
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.ImageQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstFormRepository @Inject constructor(
    private val remote: FormRemoteDataSource,
    private val formDao: FormDao,
    private val selectionDao: SelectionDao,
    private val clock: Clock,
) : FormRepository {

    // A toggle is read-modify-write; two fast taps must not both read the same old selection.
    private val selectionMutex = Mutex()

    override fun observeForm(): Flow<List<FormNode>> =
        formDao.observeForm().map { rows -> rows.toDomainTree() }

    override fun observeImage(id: Long): Flow<ImageQuestion?> =
        formDao.observeNode(id).map { it?.toImageQuestionOrNull() }

    override fun observeLastSyncedAt(): Flow<Instant?> =
        formDao.observeLastSyncedAt().map { millis -> millis?.let(Instant::ofEpochMilli) }

    override fun observeSelections(): Flow<Map<Long, Set<Long>>> =
        selectionDao.observeSelections().map { rows ->
            rows.groupBy({ it.questionId }, { it.responseId }).mapValues { (_, ids) -> ids.toSet() }
        }

    override suspend fun refresh(): RefreshResult = try {
        val entities = remote.fetchForm().toEntities()
        formDao.replaceForm(entities.nodes, entities.responses, syncedAtMillis = clock.millis())
        RefreshResult.Success
    } catch (e: CancellationException) {
        // The caller went away (e.g. the screen closed); that's not a failed refresh.
        throw e
    } catch (e: Exception) {
        RefreshResult.Failed(e.toDataError())
    }

    override suspend fun toggleResponse(question: ChoiceQuestion, responseId: Long) {
        selectionMutex.withLock {
            val current = selectionDao.getSelection(question.id).toSet()
            val updated = question.responseSet.toggle(current, responseId)
            if (updated != current) selectionDao.replaceSelection(question.id, updated)
        }
    }
}
