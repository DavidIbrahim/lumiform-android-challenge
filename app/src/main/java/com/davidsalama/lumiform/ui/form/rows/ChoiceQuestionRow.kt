package com.davidsalama.lumiform.ui.form.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.davidsalama.lumiform.R
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.Response
import com.davidsalama.lumiform.ui.form.ChoiceRow
import com.davidsalama.lumiform.ui.theme.HierarchyTypography

/**
 * The question text, one chip per response, and a status line ("None selected · single choice").
 * Taps go to the ViewModel; the selection shown always comes back from the database.
 */
@Composable
fun ChoiceQuestionRow(
    row: ChoiceRow,
    typography: HierarchyTypography,
    onResponseTapped: (ChoiceQuestion, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val responseSet = row.question.responseSet
    HierarchyRowFrame(depth = row.depth, modifier = modifier) {
        QuestionCard {
            TitleWithId(
                text = row.question.content,
                id = row.id,
                style = typography.question,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (responseSet.responses.isEmpty()) {
                StatusText(stringResource(R.string.choice_no_options))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    // Tells accessibility services these chips are one group of options.
                    modifier = Modifier.selectableGroup(),
                ) {
                    responseSet.responses.forEach { response ->
                        ResponseChip(
                            response = response,
                            selected = response.id in row.selectedIds,
                            multipleSelection = responseSet.multipleSelection,
                            onClick = { onResponseTapped(row.question, response.id) },
                        )
                    }
                }
                StatusText(selectionStatus(row.selectedIds.size, responseSet.multipleSelection))
            }
        }
    }
}

@Composable
private fun ResponseChip(
    response: Response,
    selected: Boolean,
    multipleSelection: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(response.label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        trailingIcon = response.score?.let { score -> { ScoreBadge(score) } },
        // Radio buttons for single choice, checkboxes for multiple: what a screen reader announces.
        modifier = Modifier.semantics { role = if (multipleSelection) Role.Checkbox else Role.RadioButton },
    )
}

@Composable
private fun ScoreBadge(score: Int) {
    val description = stringResource(R.string.score_description, score)
    Text(
        text = score.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .semantics { contentDescription = description }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun selectionStatus(selectedCount: Int, multipleSelection: Boolean): String {
    val count = if (selectedCount == 0) {
        stringResource(R.string.choice_none_selected)
    } else {
        pluralStringResource(R.plurals.choice_selected_count, selectedCount, selectedCount)
    }
    val mode = stringResource(if (multipleSelection) R.string.choice_multiple else R.string.choice_single)
    return stringResource(R.string.choice_status, count, mode)
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
