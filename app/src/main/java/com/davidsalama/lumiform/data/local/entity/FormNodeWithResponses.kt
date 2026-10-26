package com.davidsalama.lumiform.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A node with its choice responses (empty for non-choice nodes).
 *
 * Loading both through one `@Transaction` query means a refresh can never be observed
 * half-way, e.g. new nodes paired with the old responses.
 */
data class FormNodeWithResponses(
    @Embedded val node: FormNodeEntity,
    @Relation(parentColumn = "id", entityColumn = "question_id")
    val responses: List<ChoiceResponseEntity>,
)
