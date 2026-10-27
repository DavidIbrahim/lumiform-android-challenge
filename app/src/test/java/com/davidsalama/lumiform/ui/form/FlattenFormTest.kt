package com.davidsalama.lumiform.ui.form

import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.domain.model.UnknownNode
import com.davidsalama.lumiform.testutil.sampleFormTree
import org.junit.Assert.assertEquals
import org.junit.Test

class FlattenFormTest {

    private val rows = flattenForm(sampleFormTree)

    @Test
    fun `rows follow reading order - every container right before its children`() {
        assertEquals((1L..14L).toList(), rows.map { it.id })
    }

    @Test
    fun `depth counts the containers above each item`() {
        //            1  2  3  4  5  6  7  8  9 10 11 12 13 14
        val depths = listOf(0, 1, 2, 2, 1, 2, 2, 3, 3, 0, 1, 2, 2, 2)
        assertEquals(depths, rows.map { it.depth })
    }

    @Test
    fun `each node becomes the matching row type`() {
        assertEquals(PageRow(10, 0, "Second Page"), rows[9])
        assertEquals(SectionRow(7, 2, "Subsection 1.1", nestingLevel = 1), rows[6])
        assertEquals(TextRow(8, 3, "This is a subsection under Chapter 1."), rows[7])
        assertEquals(
            ImageRow(9, 3, "Chapter 1 Image", "https://raw.githubusercontent.com/github/explore/main/topics/ios/ios.png"),
            rows[8],
        )
        assertEquals(13L, (rows[12] as ChoiceRow).question.id)
    }

    @Test
    fun `section nesting level counts only section ancestors`() {
        val levels = rows.filterIsInstance<SectionRow>().associate { it.id to it.nestingLevel }
        assertEquals(mapOf(2L to 0, 5L to 0, 7L to 1, 11L to 0), levels)
    }

    @Test
    fun `choice rows carry their selection, empty when nothing is selected`() {
        val withSelection = flattenForm(sampleFormTree, selections = mapOf(14L to setOf(1021L, 1023L)))
            .filterIsInstance<ChoiceRow>()
            .associate { it.id to it.selectedIds }

        assertEquals(mapOf(13L to emptySet(), 14L to setOf(1021L, 1023L)), withSelection)
    }

    @Test
    fun `questions directly in a page sit one level below it`() {
        val form = listOf(Page(1, "P", listOf(TextQuestion(2, "Direct question"))))

        assertEquals(listOf(PageRow(1, 0, "P"), TextRow(2, 1, "Direct question")), flattenForm(form))
    }

    @Test
    fun `deep nesting keeps increasing depth and nesting level`() {
        val form = listOf(Page(1, "P", listOf(Section(2, "S1", listOf(Section(3, "S2", listOf(Section(4, "S3", emptyList()))))))))

        val sections = flattenForm(form).filterIsInstance<SectionRow>()

        assertEquals(listOf(1, 2, 3), sections.map { it.depth })
        assertEquals(listOf(0, 1, 2), sections.map { it.nestingLevel })
    }

    @Test
    fun `unknown nodes are kept as rows so the UI can show a placeholder`() {
        val form = listOf(Page(1, "P", listOf(UnknownNode(2, "signature"))))

        assertEquals(UnknownRow(2, 1, "signature"), flattenForm(form)[1])
    }

    @Test
    fun `empty form gives no rows`() {
        assertEquals(emptyList<FormRow>(), flattenForm(emptyList()))
    }
}
