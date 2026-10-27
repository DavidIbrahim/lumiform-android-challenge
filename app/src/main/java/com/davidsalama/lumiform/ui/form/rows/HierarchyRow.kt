package com.davidsalama.lumiform.ui.form.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val IndentStep = 14.dp
private val GuideWidth = 2.dp

/** Past this depth rows stop moving right, so deep forms stay readable on a phone. */
private const val MAX_VISUAL_DEPTH = 6

/**
 * Indents content by [depth] and draws one vertical guide per ancestor: the page's in
 * [pageColor], the sections' in [sectionColor]. Rows are laid out with no gap between them,
 * so the guides of consecutive rows join into continuous lines.
 */
fun Modifier.hierarchyGuides(depth: Int, pageColor: Color, sectionColor: Color): Modifier {
    val levels = depth.coerceIn(0, MAX_VISUAL_DEPTH)
    return drawBehind {
        for (level in 0 until levels) {
            val x = (IndentStep * level + IndentStep / 2).toPx()
            drawLine(
                color = if (level == 0) pageColor else sectionColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = GuideWidth.toPx(),
            )
        }
    }.padding(start = IndentStep * levels)
}

/** The shared frame of every row: screen margin, hierarchy guides and vertical rhythm. */
@Composable
fun HierarchyRowFrame(
    depth: Int,
    modifier: Modifier = Modifier,
    top: Dp = 4.dp,
    bottom: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .hierarchyGuides(
                depth = depth,
                // Quiet enough that the titles, not the lines, carry the colour.
                pageColor = colors.primary.copy(alpha = 0.35f),
                sectionColor = colors.secondary.copy(alpha = 0.3f),
            )
            .padding(start = 8.dp, top = top, bottom = bottom),
    ) {
        content()
    }
}

/** A title followed by the item's `#id` tag, as in the brief's sample. */
@Composable
fun TitleWithId(text: String, id: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Baseline-aligned, so the badge sits on the first line of a title that wraps.
        Text(text = text, style = style, color = color, modifier = Modifier.weight(1f, fill = false).alignByBaseline())
        IdBadge(id, Modifier.alignByBaseline())
    }
}

/** Metadata, so it's kept small and light: visible when looked for, never competing with titles. */
@Composable
fun IdBadge(id: Long, modifier: Modifier = Modifier) {
    Text(
        text = "#$id",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        // Debug-style metadata: not useful to read out to screen reader users.
        modifier = modifier.clearAndSetSemantics { },
    )
}

private val QuestionCardShape = RoundedCornerShape(12.dp)

/** Questions sit on a softly tinted card, so they stand apart from the page and section headers. */
@Composable
fun QuestionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(QuestionCardShape)
            .then(modifier)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), QuestionCardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}
