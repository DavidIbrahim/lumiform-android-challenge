package com.davidsalama.lumiform.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PageOrangeDark,
    onPrimary = Color(0xFF3A1606),
    secondary = SectionGreenDark,
    onSecondary = Color(0xFF00382A),
    // Used by the stale-data banner and selected chips; Material's default here is purple.
    secondaryContainer = SectionContainerDark,
    onSecondaryContainer = OnSectionContainerDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    // Question cards.
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PageOrangeLight,
    onPrimary = Color.White,
    secondary = SectionGreenLight,
    onSecondary = Color.White,
    secondaryContainer = SectionContainerLight,
    onSecondaryContainer = OnSectionContainerLight,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
)

/**
 * Dynamic (wallpaper) color is off on purpose: page and section colors encode the hierarchy,
 * so they must stay the same on every device.
 */
@Composable
fun LumiformTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
