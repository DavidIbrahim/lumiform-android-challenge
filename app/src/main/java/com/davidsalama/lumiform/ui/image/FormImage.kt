package com.davidsalama.lumiform.ui.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.davidsalama.lumiform.R

/**
 * Light, theme-independent backdrop. Form images are often transparent PNGs (the sample's iOS
 * logo is black), which would disappear on the dark theme's surface.
 */
val ImageCanvas = Color(0xFFF4F4F4)

/** The memory-cache key of a thumbnail, which the viewer shows while the full image loads. */
fun thumbnailCacheKey(url: String): String = "thumbnail:$url"

/** A form image on its canvas, with a placeholder while loading and an icon if it fails. */
@Composable
fun FormImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    /** Space between the canvas edge and the image, so logos don't touch the frame. */
    contentPadding: Dp = 0.dp,
    /** The image's own size, for callers that size the canvas to the image's shape. */
    onImageSize: (Size) -> Unit = {},
    /** Stores the decoded image under this key, so another screen can reuse it. */
    memoryCacheKey: String? = null,
    /** Shows the image stored under this key while this one loads, e.g. the thumbnail. */
    placeholderMemoryCacheKey: String? = null,
) {
    val context = LocalPlatformContext.current
    val request = remember(url, memoryCacheKey, placeholderMemoryCacheKey) {
        ImageRequest.Builder(context)
            .data(url)
            .memoryCacheKey(memoryCacheKey)
            .placeholderMemoryCacheKey(placeholderMemoryCacheKey)
            // When the thumbnail stands in, swap to the full image directly: a crossfade or a
            // colour placeholder would flash between two copies of the same picture.
            .apply { if (placeholderMemoryCacheKey != null) crossfade(false) }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = if (placeholderMemoryCacheKey == null) ColorPainter(MaterialTheme.colorScheme.surfaceContainer) else null,
        error = painterResource(R.drawable.ic_broken_image),
        // The placeholder from the memory cache already has the image's shape.
        onLoading = { loading -> loading.painter?.intrinsicSize?.takeIf { it.isSpecified }?.let(onImageSize) },
        onSuccess = { success -> onImageSize(success.painter.intrinsicSize) },
        modifier = modifier
            .background(ImageCanvas)
            .padding(contentPadding),
    )
}
