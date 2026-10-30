package info.quiete.launcher.ui.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.Motion
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Type

/**
 * The A–Z rail down the right edge.
 *
 * Scrolling 200 apps with a flick is a lottery; the rail turns it into a single
 * deterministic thumb movement. Three things make it feel good rather than fiddly:
 *
 *  - **A haptic tick on every letter change.** Without it the rail is a guess.
 *  - **Letters swell toward the finger.** The active letter and its neighbours
 *    scale and brighten, so the target is legible under the thumb that covers it.
 *  - **It stays cold until touched.** At rest the rail is barely-there 20% ink;
 *    it must not compete with the app names it exists to serve.
 */
@Composable
fun Scrubber(
    letters: List<String>,
    onScrub: (String) -> Unit,
    onActiveChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.size < 2) return

    val haptics = LocalHapticFeedback.current
    var activeIndex by remember { mutableIntStateOf(-1) }
    var railHeight by remember { mutableStateOf(1f) }

    fun indexFor(y: Float): Int =
        ((y / railHeight) * letters.size).toInt().coerceIn(0, letters.lastIndex)

    fun select(index: Int) {
        if (index == activeIndex) return
        activeIndex = index
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onScrub(letters[index])
        onActiveChange(letters[index])
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp)
            .padding(vertical = Space.Base)
            .pointerInput(letters) {
                railHeight = size.height.toFloat()
                detectVerticalDragGestures(
                    onDragStart = { offset -> select(indexFor(offset.y)) },
                    onDragEnd = { activeIndex = -1; onActiveChange(null) },
                    onDragCancel = { activeIndex = -1; onActiveChange(null) },
                    onVerticalDrag = { change, _ -> select(indexFor(change.position.y)) },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEachIndexed { index, letter ->
            // Distance from the finger, in letters. 0 = under the thumb.
            val distance = if (activeIndex < 0) Int.MAX_VALUE else kotlin.math.abs(index - activeIndex)

            val emphasis by animateFloatAsState(
                targetValue = when (distance) {
                    0 -> 1f
                    1 -> 0.55f
                    2 -> 0.22f
                    else -> 0f
                },
                animationSpec = Motion.standard(),
                label = "railEmphasis",
            )

            Text(
                text = letter,
                style = Type.Rail,
                color = Ink.Primary.copy(alpha = 0.20f + 0.80f * emphasis),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    val s = 1f + 0.7f * emphasis
                    scaleX = s
                    scaleY = s
                    // Letters slide left toward the list as they activate, which
                    // pulls the eye off the screen edge and onto the content.
                    translationX = -14.dp.toPx() * emphasis
                },
            )
        }
    }
}
