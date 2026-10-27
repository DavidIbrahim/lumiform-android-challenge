package com.davidsalama.lumiform.ui.form

import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.domain.model.UnknownNode

/**
 * Turns the form tree into display rows, in reading order (depth-first, pre-order):
 * every container comes right before its children.
 *
 * [selections] maps a choice question id to its selected response ids.
 */
fun flattenForm(
    nodes: List<FormNode>,
    selections: Map<Long, Set<Long>> = emptyMap(),
): List<FormRow> {
    val rows = mutableListOf<FormRow>()

    fun visit(node: FormNode, depth: Int, sectionNesting: Int) {
        when (node) {
            is Page -> {
                rows += PageRow(node.id, depth, node.title)
                node.children.forEach { visit(it, depth + 1, sectionNesting = 0) }
            }
            is Section -> {
                rows += SectionRow(node.id, depth, node.title, nestingLevel = sectionNesting)
                node.children.forEach { visit(it, depth + 1, sectionNesting + 1) }
            }
            is TextQuestion -> rows += TextRow(node.id, depth, node.content)
            is ImageQuestion -> rows += ImageRow(node.id, depth, node.title, node.src)
            is ChoiceQuestion -> rows += ChoiceRow(node.id, depth, node, selections[node.id].orEmpty())
            is UnknownNode -> rows += UnknownRow(node.id, depth, node.rawType)
        }
    }

    nodes.forEach { visit(it, depth = 0, sectionNesting = 0) }
    return rows
}
