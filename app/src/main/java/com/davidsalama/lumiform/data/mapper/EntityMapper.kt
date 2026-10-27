package com.davidsalama.lumiform.data.mapper

import com.davidsalama.lumiform.data.local.entity.ChoiceResponseEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeWithResponses
import com.davidsalama.lumiform.data.local.entity.NodeType
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.ContainerNode
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.ImageQuestion
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Response
import com.davidsalama.lumiform.domain.model.ResponseSet
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.domain.model.UnknownNode

data class FormEntities(
    val nodes: List<FormNodeEntity>,
    val responses: List<ChoiceResponseEntity>,
)

/**
 * Flattens the tree into rows. Nodes come out in pre-order (every parent before its children),
 * which the parent foreign key requires on insert.
 */
fun List<FormNode>.toEntities(): FormEntities {
    val nodes = mutableListOf<FormNodeEntity>()
    val responses = mutableListOf<ChoiceResponseEntity>()

    fun visit(node: FormNode, parentId: Long?, position: Int) {
        nodes += node.toEntity(parentId, position)
        when (node) {
            is ContainerNode -> node.children.forEachIndexed { index, child -> visit(child, node.id, index) }
            is ChoiceQuestion -> node.responseSet.responses.forEachIndexed { index, response ->
                responses += ChoiceResponseEntity(response.id, node.id, index, response.label, response.score)
            }
            else -> Unit
        }
    }

    forEachIndexed { index, root -> visit(root, parentId = null, position = index) }
    return FormEntities(nodes, responses)
}

private fun FormNode.toEntity(parentId: Long?, position: Int): FormNodeEntity = when (this) {
    is Page -> FormNodeEntity(id, parentId, position, NodeType.PAGE, title = title)
    is Section -> FormNodeEntity(id, parentId, position, NodeType.SECTION, title = title)
    is TextQuestion -> FormNodeEntity(id, parentId, position, NodeType.TEXT, content = content)
    is ImageQuestion -> FormNodeEntity(id, parentId, position, NodeType.IMAGE, title = title, imageUrl = src)
    is ChoiceQuestion -> FormNodeEntity(
        id, parentId, position, NodeType.CHOICE,
        content = content,
        responseSetId = responseSet.id,
        multipleSelection = responseSet.multipleSelection,
    )
    is UnknownNode -> FormNodeEntity(id, parentId, position, NodeType.UNKNOWN, rawType = rawType)
}

fun FormNodeEntity.toImageQuestionOrNull(): ImageQuestion? =
    if (type == NodeType.IMAGE) ImageQuestion(id, imageUrl.orEmpty(), title.orEmpty()) else null

/**
 * Rebuilds the tree from all rows in O(n): one pass groups rows by parent, then each node
 * looks up its children in that map. No recursive SQL, no query per level.
 */
fun List<FormNodeWithResponses>.toDomainTree(): List<FormNode> {
    val childrenByParent = groupBy { it.node.parentId }
        .mapValues { (_, rows) -> rows.sortedBy { it.node.position } }

    fun build(row: FormNodeWithResponses): FormNode {
        val node = row.node
        fun children() = childrenByParent[node.id].orEmpty().map(::build)
        return when (node.type) {
            NodeType.PAGE -> Page(node.id, node.title.orEmpty(), children())
            NodeType.SECTION -> Section(node.id, node.title.orEmpty(), children())
            NodeType.TEXT -> TextQuestion(node.id, node.content.orEmpty())
            NodeType.IMAGE -> ImageQuestion(node.id, node.imageUrl.orEmpty(), node.title.orEmpty())
            NodeType.CHOICE -> ChoiceQuestion(
                id = node.id,
                content = node.content.orEmpty(),
                responseSet = ResponseSet(
                    id = node.responseSetId ?: 0,
                    multipleSelection = node.multipleSelection ?: false,
                    responses = row.responses
                        .sortedBy { it.position }
                        .map { Response(it.id, it.label, it.score) },
                ),
            )
            NodeType.UNKNOWN -> UnknownNode(node.id, node.rawType.orEmpty())
        }
    }

    return childrenByParent[null].orEmpty().map(::build)
}
