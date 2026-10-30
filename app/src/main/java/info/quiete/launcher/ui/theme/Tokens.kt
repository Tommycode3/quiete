package info.quiete.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ink — the type ramp.
 *
 * Four steps, no more. The values are close to Linear's, which are close to
 * Apple's, because both arrived at the same conclusion: on a dark surface the drop
 * from primary to secondary has to be large (roughly 40% of luminance) or the
 * hierarchy reads as "some text is slightly dirty" instead of as two levels.
 *
 * Never pure #FFF for primary — pure white on near-black is a contrast ratio the
 * eye reads as glare, and it makes thin type shimmer.
 */
object Ink {
    val Primary = Color(0xFFF7F8F8)
    val Secondary = Color(0xFF9BA1AC)
    val Tertiary = Color(0xFF6B7079)
    val Faint = Color(0xFF4A4E56)
}

/**
 * Surface — the panels.
 *
 * Every panel is three things drawn in order: a translucent fill over the window
 * blur, a hairline ring, and a separator system inside it. The ring is not one
 * colour: it is bright along the top and dark along the bottom, because that is
 * what an edge does when light comes from above. Getting that gradient right is
 * most of the difference between "a card" and "an object".
 */
object Surface {
    /** Panel fill, over the platform blur. Cool, not neutral — neutral reads dead. */
    val Panel = Color(0xB814161A)

    /** Nested rows and controls inside a panel. */
    val Inset = Color(0x14FFFFFF)

    /** Pressed state for an inset row. */
    val Press = Color(0x1FFFFFFF)

    /** Top of the hairline ring — the lit edge. */
    val RingTop = Color(0x24FFFFFF)

    /** Bottom of the hairline ring — the shaded edge. */
    val RingBottom = Color(0x0AFFFFFF)

    /** Separators between rows inside a panel. Must be felt, not read. */
    val Divider = Color(0x12FFFFFF)
}

object Veil {
    /**
     * Wallpaper scrim under the panel stack. Much lighter than before — the panels
     * now carry their own legibility, so the scrim only has to keep the wallpaper
     * from punching through the gaps between them.
     */
    val HomeScrim = arrayOf(
        0.00f to Color(0x00000000),
        0.38f to Color(0x1A000000),
        0.72f to Color(0x66000000),
        1.00f to Color(0x9E000000),
    )

    /** Backdrop behind the long-press menu. */
    val Modal = Color(0xC00A0B0D)
}

/**
 * Space — a 4dp grid, six steps, and a page gutter.
 *
 * Deliberately tighter than the first draft. Density is not clutter: Linear and
 * Notion are extremely dense and read as calm, because the rhythm is regular. What
 * reads as cluttered is *irregular* spacing, not close spacing.
 */
object Space {
    val Hair = 2.dp
    val Tight = 6.dp
    val Snug = 10.dp
    val Base = 14.dp
    val Loose = 20.dp
    val Vast = 28.dp

    /** Page margin. Panels align to this on both sides. */
    val Gutter = 16.dp

    /** Padding inside a panel. */
    val PanelPad = 16.dp

    /** Minimum height of a tappable row. Below 44dp accuracy falls off a cliff. */
    val Row = 46.dp
}

/**
 * Motion.
 *
 * Recalibrated to be *fast and precise* rather than soft. The reference interfaces
 * all resolve a transition in around 150–200ms with almost no overshoot; anything
 * floatier reads as a consumer app rather than a tool. Springs are kept — they are
 * what makes an interrupted gesture resolve from real velocity — but stiffened
 * hard and damped close to critical.
 */
object Motion {
    /** Default: appears, moves, resizes. Snappy, essentially no bounce. */
    fun <T> standard() = spring<T>(
        dampingRatio = 0.92f,
        stiffness = 620f,
    )

    /** The drawer. Still the heaviest thing on screen, but no longer languid. */
    fun <T> sheet() = spring<T>(
        dampingRatio = 0.94f,
        stiffness = 420f,
    )

    /** Touch feedback. Must feel like a physical response, not an animation. */
    fun <T> press() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessHigh,
    )

    /** Opacity only: springs on alpha look mushy, use a curve. */
    fun <T> fade(durationMillis: Int = 160) = tween<T>(
        durationMillis = durationMillis,
        easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f),
    )

    /** Scale a row shrinks to while held. Subtler now that rows have edges. */
    const val PressScale = 0.985f
}
