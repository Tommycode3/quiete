package info.quiete.launcher.ui.effect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import info.quiete.launcher.ui.theme.Ambient
import info.quiete.launcher.ui.theme.LocalAmbient
import info.quiete.launcher.ui.theme.PanelShape
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Surface

/**
 * A panel: the one surface primitive the interface is built from.
 *
 * The illusion of a physical object comes almost entirely from the **ring** — a
 * single hairline whose colour runs from bright at the top to nearly nothing at the
 * bottom. That is what an edge does under a light source above it, and the eye is
 * extremely good at reading it. A uniform 1px border, by contrast, reads as a drawn
 * outline: flat, and instantly cheaper.
 *
 * Two further details that are individually invisible and collectively the effect:
 *
 *  - the ring is stroked at a half-pixel inset, so it lands on exactly one physical
 *    pixel instead of straddling two and losing half its opacity to antialiasing;
 *  - a wash of ambient wallpaper light falls across the top of the fill, so panels
 *    on a warm wallpaper are warm and panels on a cold one are cold. Without it a
 *    neutral card sits *on* the photograph rather than in it.
 */
@Composable
fun Modifier.panel(
    shape: Shape = PanelShape,
    ambient: Ambient = LocalAmbient.current,
    fill: Color = Surface.Panel,
): Modifier = this
    .clip(shape)
    .background(fill)
    .drawWithContent {
        // Ambient light pooling at the top of the fill, under the content.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(ambient.light.copy(alpha = 0.10f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.85f,
            ),
        )

        drawContent()

        val px = 1.dp.toPx()
        val ring = Brush.verticalGradient(
            0.00f to Surface.RingTop,
            0.45f to Surface.RingTop.copy(alpha = Surface.RingTop.alpha * 0.35f),
            1.00f to Surface.RingBottom,
            startY = 0f,
            endY = size.height,
        )
        translate(px / 2f, px / 2f) {
            when (val outline = shape.createOutline(size, layoutDirection, this)) {
                is Outline.Generic ->
                    drawPath(outline.path, brush = ring, style = Stroke(width = px))

                else ->
                    drawRect(brush = ring, style = Stroke(width = px))
            }
        }
    }

/**
 * A panel with the standard internal padding already applied. Most call sites want
 * this rather than the raw modifier.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: Dp = Space.PanelPad,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .panel()
            .padding(padding),
        content = content,
    )
}

/**
 * Separator between rows inside a panel.
 *
 * Inset from the left so it begins under the text rather than under the panel edge
 * — the Apple grouped-list convention. A divider running the full width cuts the
 * panel into two panels; an inset one groups rows *within* one.
 */
@Composable
fun PanelDivider(startInset: Dp = 0.dp) {
    Box(
        Modifier
            .padding(start = startInset)
            .fillMaxWidth()
            .height(1.dp)
            .background(Surface.Divider),
    )
}
