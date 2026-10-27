package com.davidsalama.lumiform.ui.form.rows

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.davidsalama.lumiform.R
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.ui.form.ChoiceRow
import com.davidsalama.lumiform.ui.form.FormRow
import com.davidsalama.lumiform.ui.form.ImageRow
import com.davidsalama.lumiform.ui.form.PageRow
import com.davidsalama.lumiform.ui.form.SectionRow
import com.davidsalama.lumiform.ui.form.TextRow
import com.davidsalama.lumiform.ui.form.UnknownRow
import com.davidsalama.lumiform.ui.theme.HierarchyTypography

@Composable
fun FormRowItem(
    row: FormRow,
    /** The top of the list: no gap above it, since nothing comes before. */
    isFirst: Boolean,
    typography: HierarchyTypography,
    onResponseTapped: (ChoiceQuestion, Long) -> Unit,
    onImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (row) {
        is PageRow -> PageHeader(row, isFirst, typography, modifier)
        is SectionRow -> SectionHeader(row, typography, modifier)
        is TextRow -> TextQuestionRow(row, typography, modifier)
        is ImageRow -> ImageQuestionRow(row, typography, onImageClick, modifier)
        is ChoiceRow -> ChoiceQuestionRow(row, typography, onResponseTapped, modifier)
        is UnknownRow -> UnsupportedRow(row, typography, modifier)
    }
}

@Composable
private fun PageHeader(row: PageRow, isFirst: Boolean, typography: HierarchyTypography, modifier: Modifier) {
    HierarchyRowFrame(depth = row.depth, modifier = modifier, top = if (isFirst) 12.dp else 32.dp, bottom = 8.dp) {
        TitleWithId(
            text = row.title,
            id = row.id,
            style = typography.page,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun SectionHeader(row: SectionRow, typography: HierarchyTypography, modifier: Modifier) {
    HierarchyRowFrame(depth = row.depth, modifier = modifier, top = 16.dp, bottom = 6.dp) {
        TitleWithId(
            text = row.title,
            id = row.id,
            style = typography.section(row.nestingLevel),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun TextQuestionRow(row: TextRow, typography: HierarchyTypography, modifier: Modifier) {
    QuestionText(row.depth, row.content, row.id, typography, modifier)
}

@Composable
private fun QuestionText(depth: Int, text: String, id: Long, typography: HierarchyTypography, modifier: Modifier) {
    HierarchyRowFrame(depth = depth, modifier = modifier) {
        QuestionCard {
            TitleWithId(
                text = text,
                id = id,
                style = typography.question,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun UnsupportedRow(row: UnknownRow, typography: HierarchyTypography, modifier: Modifier) {
    HierarchyRowFrame(depth = row.depth, modifier = modifier) {
        QuestionCard {
            Text(
                text = stringResource(R.string.unsupported_item, row.rawType),
                style = typography.question.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
