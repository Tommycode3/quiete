package info.quiete.launcher.ui.effect

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import info.quiete.launcher.ui.theme.Ambient

/**
 * Glass, built the way a physical panel actually behaves rather than the way a
 * "frosted" preset does.
 *
 * Three layers, in this order, and the order is the whole effect:
 *
 *  1. **Body** — a dark wash tinted with the ambient hue, denser at the bottom.
 *     Real glass is not uniformly grey; it takes the colour of what is behind it,
 *     and it pools darker where it is thickest.
 *  2. **Bloom** — the wallpaper's light leaking through the top of the panel. This
 *     is what stops it reading as a black rectangle laid over a photo.
 *  3. **Edge** — a single hairline along the top, brightest in the middle and
 *     falling to nothing at both ends. This is the detail that does the most work
 *     per pixel in the entire launcher: a lit edge is how the eye decides that
 *     something is a physical panel with a thickness, rather than a fill.
 *
 * The actual blur is not here — it is the platform's cross-window blur, applied to
 * the window in `MainActivity`. This only supplies what the blur cannot: tint,
 * density and an edge.
 */
fun Modifier.glass(ambient: Ambient, intensity: Float = 1f): Modifier = drawBehind {
    val a = intensity.coerceIn(0f, 1f)

    // 1. Body.
    drawRect(
        brush = Brush.verticalGradient(
            0.00f to ambient.shade.copy(alpha = 0.60f * a),
            0.35f to Color(0xFF07070A).copy(alpha = 0.80f * a),
            1.00f to Color(0xFF030304).copy(alpha = 0.93f * a),
        ),
    )

    // 2. Bloom — a wide, very faint pool of wallpaper light spilling over the top
    // edge. Radius is deliberately larger than the panel so no falloff ring shows.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                ambient.light.copy(alpha = 0.14f * a),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.28f, 0f),
            radius = size.width * 1.15f,
        ),
    )

    // 3. Edge.
    val edge = 1.dp.toPx()
    drawRect(
        brush = Brush.horizontalGradient(
            0.00f to Color.Transparent,
            0.22f to ambient.light.copy(alpha = 0.30f * a),
            0.50f to ambient.light.copy(alpha = 0.52f * a),
            0.78f to ambient.light.copy(alpha = 0.30f * a),
            1.00f to Color.Transparent,
        ),
        size = Size(size.width, edge),
    )
}

/**
 * The light source of the home screen.
 *
 * A single soft pool of wallpaper-coloured light, low and to the left, sitting
 * behind the clock. It has no shape you can point at — its only job is to make the
 * bottom of the screen feel *lit from somewhere* rather than uniformly dimmed, so
 * the type sits in a scene instead of floating on a void.
 */
fun Modifier.ambientGlow(ambient: Ambient, alpha: Float = 1f): Modifier = drawBehind {
    if (alpha <= 0.01f) return@drawBehind
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                ambient.light.copy(alpha = 0.13f * alpha),
                ambient.shade.copy(alpha = 0.05f * alpha),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.16f, size.height * 0.80f),
            radius = size.height * 0.52f,
        ),
    )
}
