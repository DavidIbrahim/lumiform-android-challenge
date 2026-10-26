package com.davidsalama.lumiform.data.mapper

import com.davidsalama.lumiform.data.local.entity.ChoiceResponseEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeEntity
import com.davidsalama.lumiform.data.local.entity.FormNodeWithResponses
import com.davidsalama.lumiform.data.local.entity.NodeType
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.UnknownNode
import com.davidsalama.lumiform.testutil.sampleFormTree
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMapperTest {

    private val entities = sampleFormTree.toEntities()

    /** What Room would return: every node with the responses pointing at it. */
    private fun FormEntities.asRows(): List<FormNodeWithResponses> {
        val responsesByQuestion = responses.groupBy { it.questionId }
        return nodes.map { FormNodeWithResponses(it, responsesByQuestion[it.id].orEmpty()) }
    }

    @Test
    fun `nodes come out in pre-order so parents are inserted before children`() {
        // The sample form happens to be numbered in pre-order.
        assertEquals((1L..14L).toList(), entities.nodes.map { it.id })
    }

    @Test
    fun `each node records its parent and its position among siblings`() {
        val subsection = entities.nodes.single { it.id == 7L }
        val image = entities.nodes.single { it.id == 9L }
        val secondPage = entities.nodes.single { it.id == 10L }

        assertEquals(FormNodeEntity(7, parentId = 5, position = 1, NodeType.SECTION, title = "Subsection 1.1"), subsection)
        assertEquals(7L, image.parentId)
        assertEquals(1, image.position)
        assertEquals(null, secondPage.parentId)
        assertEquals(1, secondPage.position)
    }

    @Test
    fun `choice responses keep their question and order`() {
        assertEquals(
            listOf(
                ChoiceResponseEntity(1011, questionId = 13, position = 0, "Safety procedures", 1),
                ChoiceResponseEntity(1012, questionId = 13, position = 1, "Equipment checks", 2),
                ChoiceResponseEntity(1013, questionId = 13, position = 2, "Not applicable", null),
            ),
            entities.responses.filter { it.questionId == 13L },
        )
        assertEquals(6, entities.responses.size)
    }

    @Test
    fun `rows map back to the same tree`() {
        assertEquals(sampleFormTree, entities.asRows().toDomainTree())
    }

    @Test
    fun `tree rebuild does not depend on row order`() {
        val shuffledRows = entities.asRows()
            .shuffled(kotlin.random.Random(42))
            .map { it.copy(responses = it.responses.reversed()) }

        assertEquals(sampleFormTree, shuffledRows.toDomainTree())
    }

    @Test
    fun `unknown node keeps its raw type through the database`() {
        val form = listOf(Page(1, "P", listOf(UnknownNode(2, "signature"))))

        assertEquals(form, form.toEntities().asRows().toDomainTree())
    }
}
