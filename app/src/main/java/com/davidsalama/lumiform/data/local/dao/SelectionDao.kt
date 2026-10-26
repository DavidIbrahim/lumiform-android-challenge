package com.davidsalama.lumiform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.davidsalama.lumiform.data.local.entity.SelectedResponseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectionDao {

    @Query("SELECT * FROM selected_response")
    fun observeSelections(): Flow<List<SelectedResponseEntity>>

    @Query("SELECT response_id FROM selected_response WHERE question_id = :questionId")
    suspend fun getSelection(questionId: Long): List<Long>

    /** Sets the full selection of one question; other questions are untouched. */
    @Transaction
    suspend fun replaceSelection(questionId: Long, responseIds: Set<Long>) {
        clearSelection(questionId)
        insertSelections(responseIds.map { SelectedResponseEntity(questionId, it) })
    }

    @Query("DELETE FROM selected_response WHERE question_id = :questionId")
    suspend fun clearSelection(questionId: Long)

    @Insert
    suspend fun insertSelections(selections: List<SelectedResponseEntity>)
}
