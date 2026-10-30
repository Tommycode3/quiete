package info.quiete.launcher.ui.effect

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

/**
 * Film grain.
 *
 * A large field of #000 on an OLED panel is not "elegant black", it is an absence —
 * the eye reads it as a dead region and any gradient crossing it bands visibly.
 * A few percent of monochrome noise fixes both: it gives the darkness a surface,
 * and it dithers the gradients so their steps disappear.
 *
 * This is the cheapest possible version of an expensive-looking trick. One 128px
 * tile is generated once and repeated by the GPU, so the cost is a single texture
 * and one blend — no shader, no per-frame work, and it runs on API 31 where AGSL
 * would not.
 */
private const val TILE = 128

private fun noiseTile(): Bitmap {
    // Fixed seed: the grain must be identical across process restarts, otherwise
    // the texture visibly "resets" every time the launcher is recreated.
    val random = Random(20260720)
    val pixels = IntArray(TILE * TILE) {
        // White with a random low alpha. Monochrome only — coloured noise would
        // fight the ambient light instead of sitting under it.
        (random.nextInt(0, 46) shl 24) or 0x00FFFFFF
    }
    return Bitmap.createBitmap(pixels, TILE, TILE, Bitmap.Config.ARGB_8888)
}

/**
 * Overlays repeating grain on whatever this modifier is applied to.
 *
 * [intensity] is deliberately tiny. Above about 0.05 it stops reading as material
 * and starts reading as a compression artefact.
 */
@Composable
fun Modifier.grain(intensity: Float = 0.035f): Modifier {
    val brush = remember {
        ShaderBrush(ImageShader(noiseTile().asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    return drawWithContent {
        drawContent()
        drawRect(brush = brush, alpha = intensity)
    }
}
