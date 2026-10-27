package com.davidsalama.lumiform.ui.form.rows

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.davidsalama.lumiform.R
import com.davidsalama.lumiform.ui.form.ImageRow
import com.davidsalama.lumiform.ui.image.FormImage
import com.davidsalama.lumiform.ui.theme.HierarchyTypography

private val ThumbnailSize = 160.dp
private val ThumbnailShape = RoundedCornerShape(8.dp)

/** A reduced-size image with its title below; tapping anywhere on it opens the full image. */
@Composable
fun ImageQuestionRow(
    row: ImageRow,
    typography: HierarchyTypography,
    onImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    HierarchyRowFrame(depth = row.depth, modifier = modifier) {
        // One clickable unit: the screen reader reads the title once, with an "open" action.
        QuestionCard(
            Modifier.clickable(
                onClickLabel = stringResource(R.string.open_image),
                role = Role.Button,
            ) { onImageClick(row.id) },
        ) {
            FormImage(
                url = row.imageUrl,
                // The title below already describes the image.
                contentDescription = null,
                contentPadding = 12.dp,
                modifier = Modifier
                    .size(ThumbnailSize)
                    .clip(ThumbnailShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ThumbnailShape),
            )
            TitleWithId(
                text = row.title,
                id = row.id,
                style = typography.question,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
