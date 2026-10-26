package com.davidsalama.lumiform.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A response the user picked. This is user state, not server data.
 *
 * There is deliberately no foreign key to [ChoiceResponseEntity]: a refresh replaces the whole
 * server tree, and a cascade would wipe every answer. Selections whose response no longer
 * exists are removed explicitly at the end of the refresh instead.
 */
@Entity(
    tableName = "selected_response",
    primaryKeys = ["question_id", "response_id"],
)
data class SelectedResponseEntity(
    @ColumnInfo(name = "question_id") val questionId: Long,
    @ColumnInfo(name = "response_id") val responseId: Long,
)
