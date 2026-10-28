package com.davidsalama.lumiform.ui.form

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.davidsalama.lumiform.domain.DataError

private const val ARC_COUNT = 3
private const val DIM_ALPHA = 0.22f

/**
 * A looping, decorative picture for the full-screen error: Wi-Fi arcs that light up one after
 * another, as if looking for a signal, when offline; a gently floating warning otherwise.
 * Soft ripples spread out behind both. Drawn with Canvas, so no animation library is needed.
 */
@Composable
fun ErrorIllustration(error: DataError, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "errorIllustration")
    // 0 → 1 over one ripple cycle; the second ripple runs half a cycle behind the first.
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ripple",
    )
    val container = MaterialTheme.colorScheme.secondaryContainer
    val accent = MaterialTheme.colorScheme.secondary

    Box(modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val core = size.minDimension * 0.32f
            drawRipple(container, core, ripple)
            drawRipple(container, core, (ripple + 0.5f) % 1f)
            drawCircle(container, radius = core)
        }
        if (error == DataError.NoConnection) {
            // 0..ARC_COUNT+1: the dot, then each arc lights up, then all hold briefly before restarting.
            val signal by transition.animateFloat(
                initialValue = 0f,
                targetValue = ARC_COUNT + 1.5f,
                animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
                label = "signal",
            )
            Canvas(Modifier.size(84.dp)) { drawSearchingWifi(accent, signal) }
        } else {
            val float by transition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
                label = "float",
            )
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { translationY = float.dp.toPx() },
            )
        }
    }
}

/** A ring that grows from the core circle to the edge while fading out. */
private fun DrawScope.drawRipple(color: Color, coreRadius: Float, progress: Float) {
    val maxRadius = size.minDimension / 2
    drawCircle(
        color = color,
        radius = coreRadius + (maxRadius - coreRadius) * progress,
        alpha = (1f - progress) * 0.6f,
    )
}

/** A dot and three arcs above it; each part brightens once [signal] reaches its index. */
private fun DrawScope.drawSearchingWifi(color: Color, signal: Float) {
    fun alphaFor(index: Int) = DIM_ALPHA + (1f - DIM_ALPHA) * (signal - index).coerceIn(0f, 1f)

    val stroke = size.minDimension * 0.09f
    val origin = Offset(size.width / 2, size.height * 0.82f)
    drawCircle(color, radius = stroke * 0.75f, center = origin, alpha = alphaFor(0))

    val spacing = (size.height * 0.72f) / ARC_COUNT
    for (arc in 1..ARC_COUNT) {
        val radius = spacing * arc
        drawArc(
            color = color,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(origin.x - radius, origin.y - radius),
            size = Size(radius * 2, radius * 2),
            alpha = alphaFor(arc),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
