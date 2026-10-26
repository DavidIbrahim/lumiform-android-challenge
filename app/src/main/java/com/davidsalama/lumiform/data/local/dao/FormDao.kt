package com.davidsalama.lumiform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.davidsalama.lumiform.data.local.entity.ChoiceResponseEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeWithResponses
import com.davidsalama.lumiform.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {

    /** The whole form in one read; the tree (and sibling order) is rebuilt in memory. */
    @Transaction
    @Query("SELECT * FROM form_node")
    fun observeForm(): Flow<List<FormNodeWithResponses>>

    @Query("SELECT last_synced_at FROM sync_metadata WHERE `key` = :key")
    fun observeLastSyncedAt(key: String = SyncMetadataEntity.FORM_KEY): Flow<Long?>

    /**
     * Replaces the cached form with a freshly fetched one, atomically: observers see either the
     * old form or the new one, and a failure (e.g. a duplicate id) leaves the old form untouched.
     *
     * Rows are deleted explicitly and inserted with a plain `@Insert`. `REPLACE` is avoided on
     * purpose: it deletes the old row first, which fires `ON DELETE CASCADE` on its children.
     *
     * [nodes] must list every parent before its children, because of the parent foreign key.
     */
    @Transaction
    suspend fun replaceForm(
        nodes: List<FormNodeEntity>,
        responses: List<ChoiceResponseEntity>,
        syncedAtMillis: Long,
    ) {
        deleteAllResponses()
        deleteAllNodes()
        insertNodes(nodes)
        insertResponses(responses)
        deleteOrphanSelections()
        upsertSyncMetadata(SyncMetadataEntity(SyncMetadataEntity.FORM_KEY, syncedAtMillis))
    }

    @Query("DELETE FROM choice_response")
    suspend fun deleteAllResponses()

    @Query("DELETE FROM form_node")
    suspend fun deleteAllNodes()

    @Insert
    suspend fun insertNodes(nodes: List<FormNodeEntity>)

    @Insert
    suspend fun insertResponses(responses: List<ChoiceResponseEntity>)

    /** Drops answers whose response is gone, or no longer belongs to the same question. */
    @Query(
        """
        DELETE FROM selected_response
        WHERE NOT EXISTS (
            SELECT 1 FROM choice_response c
            WHERE c.id = selected_response.response_id
              AND c.question_id = selected_response.question_id
        )
        """,
    )
    suspend fun deleteOrphanSelections()

    // No child rows reference this table, so an upsert can't trigger any cascade.
    @Upsert
    suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)
}
