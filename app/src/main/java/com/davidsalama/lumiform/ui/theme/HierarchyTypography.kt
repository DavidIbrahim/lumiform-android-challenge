package com.davidsalama.lumiform.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Font sizes that follow the form hierarchy: page > section (smaller the deeper it's nested)
 * > question. Sizes come from the Material type scale and are in sp, so the user's font
 * scale setting still applies.
 */
class HierarchyTypography(private val typography: Typography) {

    val page: TextStyle = typography.headlineMedium.copy(fontWeight = FontWeight.Bold)

    val question: TextStyle = typography.bodyLarge

    /**
     * Each nesting level is [SECTION_STEP_SP] smaller, but a section always stays at least
     * [MIN_GAP_TO_QUESTION_SP] bigger than a question. Past that point depth is still shown
     * by indentation and guide lines.
     */
    fun section(nestingLevel: Int): TextStyle {
        val base = typography.titleLarge
        val floor = question.fontSize.value + MIN_GAP_TO_QUESTION_SP
        val size = (base.fontSize.value - SECTION_STEP_SP * nestingLevel.coerceAtLeast(0))
            .coerceAtLeast(floor)
        return base.copy(
            fontSize = size.sp,
            lineHeight = (size + LINE_SPACING_SP).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }

    private companion object {
        const val SECTION_STEP_SP = 2f
        const val MIN_GAP_TO_QUESTION_SP = 2f
        const val LINE_SPACING_SP = 6f
    }
}
