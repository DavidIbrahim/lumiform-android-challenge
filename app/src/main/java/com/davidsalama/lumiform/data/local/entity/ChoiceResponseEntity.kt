package com.davidsalama.lumiform.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "choice_response",
    foreignKeys = [
        ForeignKey(
            entity = FormNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("question_id")],
)
data class ChoiceResponseEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "question_id") val questionId: Long,
    val position: Int,
    val label: String,
    val score: Int?,
)
