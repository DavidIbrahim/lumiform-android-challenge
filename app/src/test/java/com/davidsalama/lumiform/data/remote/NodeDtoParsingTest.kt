package com.davidsalama.lumiform.data.remote

import com.davidsalama.lumiform.data.remote.dto.ChoiceDto
import com.davidsalama.lumiform.data.remote.dto.ImageDto
import com.davidsalama.lumiform.data.remote.dto.NodeDto
import com.davidsalama.lumiform.data.remote.dto.PageDto
import com.davidsalama.lumiform.data.remote.dto.SectionDto
import com.davidsalama.lumiform.data.remote.dto.TextDto
import com.davidsalama.lumiform.data.remote.dto.UnknownNodeDto
import com.davidsalama.lumiform.testutil.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeDtoParsingTest {

    private fun parse(json: String): List<NodeDto> = FormApi.json.decodeFromString(json)

    private val form by lazy { parse(Fixtures.sampleForm) }

    private fun NodeDto.items(): List<NodeDto> = when (this) {
        is PageDto -> items
        is SectionDto -> items
        else -> emptyList()
    }

    private fun NodeDto.allNodes(): List<NodeDto> = listOf(this) + items().flatMap { it.allNodes() }

    @Test
    fun `real response parses into two pages`() {
        assertEquals(listOf(1L, 10L), form.map { it.id })
        assertTrue(form.all { it is PageDto })
    }

    @Test
    fun `every node of the real response gets its known type`() {
        val nodes = form.flatMap { it.allNodes() }
        assertEquals((1L..14L).toList(), nodes.map { it.id })
        assertTrue(nodes.none { it is UnknownNodeDto })
    }

    @Test
    fun `nested sections keep their children in order`() {
        val chapter1 = form[0].items()[1] as SectionDto
        val subsection = chapter1.items[1] as SectionDto

        assertEquals("Subsection 1.1", subsection.title)
        assertEquals(listOf(8L, 9L), subsection.items.map { it.id })
        assertEquals("Chapter 1 Image", (subsection.items[1] as ImageDto).title)
    }

    @Test
    fun `score can be a number, null, or missing`() {
        val chapter2 = form[1].items()[0] as SectionDto
        val single = chapter2.items[1] as ChoiceDto
        val multi = chapter2.items[2] as ChoiceDto

        assertFalse(single.responseSet.multipleSelection)
        assertEquals(listOf(1, 2, null), single.responseSet.responses.map { it.score })
        assertTrue(multi.responseSet.multipleSelection)
        assertEquals(listOf(null, null, null), multi.responseSet.responses.map { it.score })
    }

    @Test
    fun `unknown type becomes UnknownNodeDto instead of failing the whole form`() {
        val nodes = parse(
            """
            [{"id": 1, "type": "page", "title": "P", "items": [
              {"id": 2, "type": "signature", "label": "Sign here"},
              {"id": 3, "type": "text", "content": "Still parsed"}
            ]}]
            """,
        )

        val (unknown, text) = (nodes.single() as PageDto).items
        assertEquals(UnknownNodeDto(id = 2, type = "signature"), unknown)
        assertEquals(TextDto(id = 3, content = "Still parsed"), text)
    }

    @Test
    fun `missing or null items become an empty list`() {
        val nodes = parse(
            """
            [{"id": 1, "type": "page", "title": "P", "items": [
              {"id": 2, "type": "section", "title": "No items"},
              {"id": 3, "type": "section", "title": "Null items", "items": null}
            ]}]
            """,
        )

        val sections = (nodes.single() as PageDto).items.map { it as SectionDto }
        assertTrue(sections.all { it.items.isEmpty() })
    }

    @Test
    fun `node without a type becomes UnknownNodeDto`() {
        assertEquals(UnknownNodeDto(id = 7, type = ""), parse("""[{"id": 7}]""").single())
    }
}
