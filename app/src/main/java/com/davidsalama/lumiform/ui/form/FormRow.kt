package com.davidsalama.lumiform.ui.form

import com.davidsalama.lumiform.domain.model.ChoiceQuestion

/**
 * One line of the form screen. The tree is flattened into these so a single LazyColumn can
 * render it lazily, with [depth] carrying the hierarchy (indentation, guide lines).
 *
 * [id] is the server id. It's safe as a LazyColumn key because it's the database primary key,
 * so duplicates can never reach the UI.
 */
sealed interface FormRow {
    val id: Long

    /** 0 for a top-level page, +1 for every container above the item. */
    val depth: Int
}

data class PageRow(
    override val id: Long,
    override val depth: Int,
    val title: String,
) : FormRow

data class SectionRow(
    override val id: Long,
    override val depth: Int,
    val title: String,
    /** How many sections this one is nested in (0 for a section directly in a page). */
    val nestingLevel: Int,
) : FormRow

data class TextRow(
    override val id: Long,
    override val depth: Int,
    val content: String,
) : FormRow

data class ImageRow(
    override val id: Long,
    override val depth: Int,
    val title: String,
    val imageUrl: String,
) : FormRow

data class ChoiceRow(
    override val id: Long,
    override val depth: Int,
    val question: ChoiceQuestion,
    val selectedIds: Set<Long>,
) : FormRow

data class UnknownRow(
    override val id: Long,
    override val depth: Int,
    val rawType: String,
) : FormRow
