package info.quiete.launcher.ui.theme

import android.app.WallpaperManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

/**
 * The colour of the light in the room.
 *
 * An interface that sits on a photograph and ignores it looks pasted on. Quiete
 * samples the wallpaper and lets its dominant hue bleed into the glass, the edge
 * highlights and the glow behind the clock — at intensities low enough that you
 * never consciously see a colour, you just stop noticing a seam.
 *
 * This is deliberately *not* Material You. Monet paints components in the accent;
 * here the accent is only ever light — it falls on surfaces, it never fills them.
 */
@Immutable
data class Ambient(
    /** Dominant wallpaper hue, saturated and clamped for use as light. */
    val light: Color,
    /** Cooler secondary, used on the far side of gradients so the glow has depth. */
    val shade: Color,
)

val LocalAmbient = compositionLocalOf {
    // Neutral fallback: a very slightly blue-cold white, which is what an overcast
    // sky does to a room. Never pure grey — pure grey reads as "no light at all".
    Ambient(
        light = Color(0xFF8FA6C4),
        shade = Color(0xFF4A5568),
    )
}

/**
 * Tracks the system wallpaper's colours and re-derives [Ambient] when it changes.
 *
 * Uses `getWallpaperColors` rather than reading the wallpaper bitmap: it needs no
 * permission, it is already computed by the system, and sampling a full-resolution
 * bitmap on every launcher start to learn three colours would be indefensible.
 */
@Composable
fun rememberAmbient(): State<Ambient> {
    val context = LocalContext.current
    val manager = remember { WallpaperManager.getInstance(context) }
    val fallback = LocalAmbient.current
    val state = remember { mutableStateOf(fallback) }

    DisposableEffect(manager) {
        fun refresh() {
            val colors = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            state.value = derive(
                primary = colors?.primaryColor?.toArgb(),
                secondary = colors?.secondaryColor?.toArgb(),
            ) ?: state.value
        }

        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) refresh()
        }
        manager.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
        refresh()

        onDispose { manager.removeOnColorsChangedListener(listener) }
    }

    return state
}

/**
 * Turn raw wallpaper colours into usable light.
 *
 * Two corrections do all the work. Wallpaper primaries are often either washed out
 * or nearly black, and both extremes read as dirt rather than illumination — so
 * saturation gets a floor and a ceiling, and lightness is pinned to a narrow band.
 * The result is a hue you can recognise as coming from the image, at an intensity
 * that never competes with the type.
 */
private fun derive(primary: Int?, secondary: Int?): Ambient? {
    val base = primary ?: return null
    return Ambient(
        light = base.asLight(saturation = 0.42f, lightness = 0.68f),
        shade = (secondary ?: base).asLight(saturation = 0.30f, lightness = 0.34f),
    )
}

private fun Int.asLight(saturation: Float, lightness: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this, hsv)
    hsv[1] = saturation
    hsv[2] = lightness
    return Color(android.graphics.Color.HSVToColor(hsv))
}
