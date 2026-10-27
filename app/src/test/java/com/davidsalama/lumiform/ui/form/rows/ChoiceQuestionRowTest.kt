package com.davidsalama.lumiform.ui.form.rows

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.ContainerNode
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.ResponseSet
import com.davidsalama.lumiform.testutil.sampleFormTree
import com.davidsalama.lumiform.ui.form.ChoiceRow
import com.davidsalama.lumiform.ui.theme.HierarchyTypography
import com.davidsalama.lumiform.ui.theme.LumiformTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders the real choice row on the JVM (Robolectric) and checks what users and screen readers get. */
@RunWith(AndroidJUnit4::class)
class ChoiceQuestionRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val single = findChoice(13)
    private val multi = findChoice(14)
    private val taps = mutableListOf<Pair<Long, Long>>()

    private fun findChoice(id: Long): ChoiceQuestion {
        fun find(nodes: List<FormNode>): ChoiceQuestion? = nodes.firstNotNullOfOrNull { node ->
            when (node) {
                is ChoiceQuestion -> node.takeIf { it.id == id }
                is ContainerNode -> find(node.children)
                else -> null
            }
        }
        return checkNotNull(find(sampleFormTree))
    }

    private fun show(question: ChoiceQuestion, selectedIds: Set<Long> = emptySet()) {
        composeRule.setContent {
            LumiformTheme {
                ChoiceQuestionRow(
                    row = ChoiceRow(question.id, depth = 2, question = question, selectedIds = selectedIds),
                    typography = HierarchyTypography(MaterialTheme.typography),
                    onResponseTapped = { q, responseId -> taps += q.id to responseId },
                )
            }
        }
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun `tapping a response reports the question and response`() {
        show(single)

        composeRule.onNodeWithText("Equipment checks").performClick()

        assertEquals(listOf(13L to 1012L), taps)
    }

    @Test
    fun `selected responses are marked selected, the others not`() {
        show(multi, selectedIds = setOf(1021, 1023))

        composeRule.onNodeWithText("Entrance").assertIsSelected()
        composeRule.onNodeWithText("Storage").assertIsNotSelected()
        composeRule.onNodeWithText("Loading bay").assertIsSelected()
        composeRule.onNodeWithText("2 selected · multiple choice").assertExists()
    }

    @Test
    fun `status line says when nothing is selected and which mode applies`() {
        show(single)

        composeRule.onNodeWithText("None selected · single choice").assertExists()
    }

    @Test
    fun `single choice chips are announced as radio buttons`() {
        show(single)

        composeRule.onNodeWithText("Safety procedures").assert(hasRole(Role.RadioButton))
    }

    @Test
    fun `multiple choice chips are announced as checkboxes`() {
        show(multi)

        composeRule.onNodeWithText("Storage").assert(hasRole(Role.Checkbox))
    }

    @Test
    fun `scores are shown with a spoken description`() {
        show(single)

        composeRule.onNodeWithContentDescription("score 1", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("score 2", useUnmergedTree = true).assertExists()
        // "Not applicable" has a null score, so there is no third badge.
        assertEquals(
            2,
            composeRule.onAllNodes(
                SemanticsMatcher("is a score badge") { node ->
                    node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { it.startsWith("score") }
                },
                useUnmergedTree = true,
            ).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `question without responses explains there is nothing to choose`() {
        show(single.copy(responseSet = ResponseSet(id = 1, multipleSelection = false, responses = emptyList())))

        composeRule.onNodeWithText("No options to choose from.").assertExists()
    }
}
