package info.quiete.launcher.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Continuous corner curvature — the squircle.
 *
 * This is the single most-copied and least-implemented detail in interface design.
 * A normal rounded rectangle joins a straight edge to a circular arc, and at that
 * junction the curvature jumps from zero to 1/r instantly. You cannot name what is
 * wrong when you look at it, but you can see it: the corner reads as *stuck on*.
 * Apple has used a superellipse instead since iOS 7, which is most of why their
 * rectangles look softer than everyone else's at identical corner radii.
 *
 * Here the straight edges are kept and only the corners follow a superellipse of
 * exponent [exponent], so curvature ramps up gradually instead of switching on.
 * Sampled as a polyline rather than fitted with Béziers — at 20 segments per corner
 * the error is far below one physical pixel on a phone, and the code stays legible
 * instead of becoming a table of magic control-point ratios.
 *
 * `n = 4.5` sits very close to Apple's curve. Lower tends to a circle (n = 2 *is* a
 * circle), higher tends to a square.
 */
class SquircleShape(
    private val radius: Dp,
    private val exponent: Float = 4.5f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { radius.toPx() }
            .coerceAtMost(min(size.width, size.height) / 2f)
        return Outline.Generic(path(size, r))
    }

    private fun path(size: Size, r: Float): Path {
        val w = size.width
        val h = size.height
        val p = Path()
        val k = 2f / exponent

        // Superellipse quarter, normalised to the unit square and evaluated at
        // `steps` points. Both axes use the same profile, mirrored per corner.
        //
        // The clamp is load-bearing, not defensive. In Float, cos(PI/2) evaluates to
        // -4.4e-8 rather than 0, and a negative base raised to a fractional exponent
        // is NaN — which silently poisons the whole path and makes anything clipped
        // to this shape vanish. Costs one instruction; cost me an afternoon.
        fun arc(steps: Int, place: (fx: Float, fy: Float) -> Unit) {
            for (i in 0..steps) {
                val t = (i.toFloat() / steps) * (PI.toFloat() / 2f)
                place(
                    cos(t).coerceAtLeast(0f).pow(k),
                    sin(t).coerceAtLeast(0f).pow(k),
                )
            }
        }

        p.moveTo(r, 0f)
        p.lineTo(w - r, 0f)
        arc(STEPS) { fx, fy -> p.lineTo(w - r + r * fy, r - r * fx) }

        p.lineTo(w, h - r)
        arc(STEPS) { fx, fy -> p.lineTo(w - r + r * fx, h - r + r * fy) }

        p.lineTo(r, h)
        arc(STEPS) { fx, fy -> p.lineTo(r - r * fy, h - r + r * fx) }

        p.lineTo(0f, r)
        arc(STEPS) { fx, fy -> p.lineTo(r - r * fx, r - r * fy) }

        p.close()
        return p
    }

    private companion object {
        const val STEPS = 20
    }
}

/** Panel-level corner. Everything that reads as a surface uses this. */
val PanelShape = SquircleShape(22.dp)

/** Elements nested inside a panel — rows, thumbnails, buttons. */
val InsetShape = SquircleShape(13.dp)

/** Small controls and chips. */
val ChipShape = SquircleShape(9.dp)
