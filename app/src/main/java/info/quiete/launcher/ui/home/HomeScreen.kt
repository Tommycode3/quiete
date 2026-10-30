package info.quiete.launcher.ui.home

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import info.quiete.launcher.data.AppEntry
import info.quiete.launcher.ui.AppRow
import info.quiete.launcher.ui.effect.Panel
import info.quiete.launcher.ui.effect.PanelDivider
import info.quiete.launcher.ui.effect.ambientGlow
import info.quiete.launcher.ui.effect.grain
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.LocalAmbient
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Type
import info.quiete.launcher.ui.theme.Veil

/**
 * The home screen: a bottom-anchored stack of panels.
 *
 * The composition rule is Apple's grouped-list convention rather than a free canvas
 * — panels are full-bleed to a single gutter, stacked with one consistent gap, and
 * each holds one category of thing. It is a deliberately conventional skeleton,
 * because the craft is supposed to live in the *edges, light and rhythm*, not in an
 * unusual layout. Unusual layouts are how interfaces look designed by someone who
 * had something to prove.
 *
 * [progress] is the drawer's open fraction. The layers do not move together: the
 * scrim goes first, then the panel stack, then the clock. Parallax between them is
 * what gives the screen depth — move everything by the same amount and it reads as
 * one flat sheet sliding away.
 */
@Composable
fun HomeScreen(
    pinned: List<AppEntry>,
    progress: Float,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onLongPress: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ambient = LocalAmbient.current
    val fade = (1f - progress * 1.7f).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colorStops = Veil.HomeScrim))
                .ambientGlow(ambient, alpha = 1f - progress)
                .grain(),
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .systemBarsPadding()
                .padding(horizontal = Space.Gutter, vertical = Space.Loose),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Furthest layer: moves least, like something seen through the window
            // rather than sitting on the glass.
            NowPanel(
                Modifier.graphicsLayer { translationY = -progress * 26.dp.toPx() },
            )

            Spacer(Modifier.height(Space.Snug))

            FavouritesPanel(
                pinned = pinned,
                onLaunch = onLaunch,
                onLongPress = onLongPress,
                modifier = Modifier.graphicsLayer { translationY = -progress * 72.dp.toPx() },
            )
        }
    }
}

/** Time and date, as a card rather than as a poster. */
@Composable
private fun NowPanel(modifier: Modifier = Modifier) {
    Panel(modifier) {
        Clock()
    }
}

/**
 * Pinned apps as an inset grouped list.
 *
 * The dividers start under the label rather than at the panel edge — that inset is
 * the entire difference between "rows that belong to one group" and "a panel that
 * has been sliced up".
 */
@Composable
private fun FavouritesPanel(
    pinned: List<AppEntry>,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onLongPress: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pinned.isEmpty()) return

    Panel(modifier, padding = Space.Tight) {
        pinned.forEachIndexed { index, entry ->
            AppRow(
                entry = entry,
                style = Type.Body,
                onLaunch = onLaunch,
                onLongPress = onLongPress,
            )
            if (index != pinned.lastIndex) {
                PanelDivider(startInset = Space.Snug)
            }
        }
    }
}
