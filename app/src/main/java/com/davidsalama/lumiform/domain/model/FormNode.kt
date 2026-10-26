package com.davidsalama.lumiform.domain.model

/**
 * One item of a form: a page, a section or a question.
 *
 * Ids are assumed to be unique across the whole form, whatever the type, so they can be used
 * as database keys and list keys. They are [Long] because SQLite integers are 64-bit.
 */
sealed interface FormNode {
    val id: Long
}

/** A node that holds other nodes, in display order. */
sealed interface ContainerNode : FormNode {
    val title: String
    val children: List<FormNode>
}

data class Page(
    override val id: Long,
    override val title: String,
    override val children: List<FormNode>,
) : ContainerNode

data class Section(
    override val id: Long,
    override val title: String,
    override val children: List<FormNode>,
) : ContainerNode

sealed interface Question : FormNode

data class TextQuestion(
    override val id: Long,
    val content: String,
) : Question

data class ImageQuestion(
    override val id: Long,
    val src: String,
    val title: String,
) : Question

data class ChoiceQuestion(
    override val id: Long,
    val content: String,
    val responseSet: ResponseSet,
) : Question

/**
 * A node whose type this app version doesn't know (e.g. a type the backend added later).
 * Kept instead of dropped, so one new type can't break parsing of the whole form.
 */
data class UnknownNode(
    override val id: Long,
    val rawType: String,
) : FormNode
