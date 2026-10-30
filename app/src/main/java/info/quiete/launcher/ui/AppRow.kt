package info.quiete.launcher.ui

import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import info.quiete.launcher.data.AppEntry
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.InsetShape
import info.quiete.launcher.ui.theme.Motion
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Surface
import info.quiete.launcher.ui.theme.Type

/**
 * A row inside a panel.
 *
 * Now that rows live in a bounded surface, the press state changed character: the
 * old version shrank the text from its left edge, which needed to be dramatic
 * because there was nothing else on screen to react. Inside a panel the correct
 * feedback is a **fill** — a squircle of light appearing behind the row, sized to
 * the row, the way every grouped list on iOS and every row in Linear behaves. The
 * scale is kept but reduced to almost nothing; it now reads as pressure rather than
 * as an animation.
 *
 * The row reports its on-screen [Rect] to [onLaunch] so the system can animate the
 * opening app outward from exactly this line.
 */
@Composable
fun AppRow(
    entry: AppEntry,
    style: TextStyle,
    onLaunch: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((AppEntry, Rect?) -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PressScale else 1f,
        animationSpec = Motion.press(),
        label = "rowScale",
    )
    val tint by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = Motion.fade(110),
        label = "rowTint",
    )

    // A plain holder, not state: the bounds are only read inside gesture callbacks,
    // so writing them must not schedule a recomposition.
    val bounds = remember { arrayOfNulls<Rect>(1) }

    val longPress: ((Offset) -> Unit)? = onLongPress?.let { handler ->
        { _: Offset -> handler(entry, bounds[0]) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Space.Row)
            .onGloballyPositioned { coords ->
                val r = coords.boundsInWindow()
                bounds[0] = Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .clip(InsetShape)
            .background(Surface.Press.copy(alpha = Surface.Press.alpha * tint))
            .pointerInput(entry.component) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactions.emit(press)
                        val released = tryAwaitRelease()
                        interactions.emit(
                            if (released) PressInteraction.Release(press)
                            else PressInteraction.Cancel(press),
                        )
                    },
                    onTap = { onLaunch(entry, bounds[0]) },
                    onLongPress = longPress,
                )
            }
            .padding(horizontal = Space.Snug, vertical = Space.Snug),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = entry.label,
            style = style,
            color = Ink.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small A–Z heading between drawer sections. */
@Composable
fun SectionHeader(letter: String, modifier: Modifier = Modifier) {
    Text(
        text = letter,
        style = Type.Micro,
        color = Ink.Faint,
        modifier = modifier.padding(
            start = Space.Snug,
            top = Space.Base,
            bottom = Space.Tight,
        ),
    )
}
