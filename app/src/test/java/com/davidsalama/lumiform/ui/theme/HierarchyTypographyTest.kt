package com.davidsalama.lumiform.ui.theme

import androidx.compose.material3.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyTypographyTest {

    private val material = Typography()
    private val hierarchy = HierarchyTypography(material)

    private fun sectionSize(level: Int) = hierarchy.section(level).fontSize.value
    private val pageSize = hierarchy.page.fontSize.value
    private val questionSize = hierarchy.question.fontSize.value

    @Test
    fun `page is larger than every section, and every section larger than a question`() {
        for (level in 0..20) {
            assertTrue("level $level: page > section", pageSize > sectionSize(level))
            assertTrue("level $level: section > question", sectionSize(level) > questionSize)
        }
    }

    @Test
    fun `nested sections get smaller until the floor, and never larger`() {
        assertEquals(listOf(22f, 20f, 18f, 18f, 18f), (0..4).map(::sectionSize))
        (0 until 20).forEach { level -> assertTrue(sectionSize(level + 1) <= sectionSize(level)) }
    }

    @Test
    fun `sizes come from the Material type scale`() {
        assertEquals(material.headlineMedium.fontSize, hierarchy.page.fontSize)
        assertEquals(material.titleLarge.fontSize, hierarchy.section(0).fontSize)
        assertEquals(material.bodyLarge.fontSize, hierarchy.question.fontSize)
    }

    @Test
    fun `negative level is treated as top level`() {
        assertEquals(sectionSize(0), sectionSize(-1))
    }
}
