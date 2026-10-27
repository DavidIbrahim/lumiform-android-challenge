package com.davidsalama.lumiform.ui.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.davidsalama.lumiform.R

/**
 * Light, theme-independent backdrop. Form images are often transparent PNGs (the sample's iOS
 * logo is black), which would disappear on the dark theme's surface.
 */
val ImageCanvas = Color(0xFFF4F4F4)

/** A form image on its canvas, with a placeholder while loading and an icon if it fails. */
@Composable
fun FormImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    /** Space between the canvas edge and the image, so logos don't touch the frame. */
    contentPadding: Dp = 0.dp,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
        error = painterResource(R.drawable.ic_broken_image),
        modifier = modifier
            .background(ImageCanvas)
            .padding(contentPadding),
    )
}
