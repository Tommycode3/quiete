package info.quiete.launcher.ui.drawer

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import info.quiete.launcher.data.AppEntry
import info.quiete.launcher.ui.AppRow
import info.quiete.launcher.ui.SectionHeader
import info.quiete.launcher.ui.effect.glass
import info.quiete.launcher.ui.effect.grain
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.LocalAmbient
import info.quiete.launcher.ui.theme.Motion
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Type
import info.quiete.launcher.ui.theme.Veil
import kotlinx.coroutines.launch

private sealed interface Row {
    data class Header(val letter: String) : Row
    data class App(val entry: AppEntry) : Row
}

/**
 * The app drawer: one alphabetical column, an A–Z rail, and a search field.
 *
 * No grid, no folders, no pages. A vertical list of names is the fastest surface
 * to scan that exists, and it is the only layout that stays elegant at 40 apps and
 * at 400. The design effort went into the two things that make a long list usable —
 * the rail and search — rather than into decorating the rows.
 */
@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    query: String,
    progress: Float,
    listState: LazyListState,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onLongPress: (AppEntry, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var scrubbing by remember { mutableStateOf<String?>(null) }

    val searching = query.isNotBlank()

    val rows by remember(apps, searching) {
        derivedStateOf {
            if (searching) {
                apps.map { Row.App(it) }
            } else {
                buildList<Row> {
                    var section: String? = null
                    apps.forEach { app ->
                        if (app.section != section) {
                            section = app.section
                            add(Row.Header(app.section))
                        }
                        add(Row.App(app))
                    }
                }
            }
        }
    }

    val letters by remember(rows) {
        derivedStateOf { rows.filterIsInstance<Row.Header>().map { it.letter } }
    }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                // Rides in from below and fades, so a half-open drag never shows a
                // hard edge cutting across the wallpaper.
                translationY = (1f - progress) * size.height
                alpha = progress.coerceIn(0f, 1f)
            }
            // Density ramps with the drag: at 20% open the panel is barely there and
            // the wallpaper still reads through it, which is what makes the gesture
            // feel like lifting a pane rather than cutting to another screen.
            .glass(LocalAmbient.current, intensity = progress)
            .grain(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.Gutter, vertical = Space.Base),
            )

            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.Gutter,
                        end = Space.Vast,
                        bottom = Space.Vast,
                    ),
                ) {
                    rows.forEach { row ->
                        when (row) {
                            is Row.Header -> item(key = "h:${row.letter}") {
                                SectionHeader(row.letter)
                            }

                            is Row.App -> item(
                                key = "a:${row.entry.component.flattenToShortString()}",
                            ) {
                                AppRow(
                                    entry = row.entry,
                                    style = Type.Body,
                                    onLaunch = onLaunch,
                                    onLongPress = onLongPress,
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(vertical = 1.dp),
                                )
                            }
                        }
                    }
                }

                Scrubber(
                    letters = letters,
                    onScrub = { letter ->
                        val index = rows.indexOfFirst { it is Row.Header && it.letter == letter }
                        if (index >= 0) scope.launch { listState.scrollToItem(index) }
                    },
                    onActiveChange = { scrubbing = it },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )

                ScrubOverlay(
                    letter = scrubbing,
                    modifier = Modifier.align(Alignment.Center),
                )

                if (rows.isEmpty() && searching) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            info.quiete.launcher.R.string.drawer_empty,
                        ),
                        style = Type.Body,
                        color = Ink.Tertiary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = Space.Loose),
                    )
                }
            }
        }
    }

    // Results are ranked, so after every keystroke the answer is at position 0.
    LaunchedEffect(query) {
        if (searching) listState.scrollToItem(0)
    }
}

/**
 * The letter under the thumb while scrubbing — drawn large and centred so it stays
 * readable around the hand that is covering the rail.
 *
 * Extracted into its own composable on purpose: inside the drawer's Column the
 * `ColumnScope.AnimatedVisibility` overload shadows the plain one. Its own function
 * has no receiver, so the intended overload resolves.
 */
@Composable
private fun ScrubOverlay(letter: String?, modifier: Modifier = Modifier) {
    // Hold the last letter so it stays visible through the exit animation instead
    // of blanking the instant the finger lifts.
    var last by remember { mutableStateOf("") }
    if (letter != null) last = letter

    AnimatedVisibility(
        visible = letter != null,
        enter = fadeIn(Motion.fade(90)) + scaleIn(Motion.standard(), initialScale = 0.86f),
        exit = fadeOut(Motion.fade(160)) + scaleOut(Motion.standard(), targetScale = 0.92f),
        modifier = modifier,
    ) {
        Text(
            text = last,
            style = Type.Scrub,
            color = Ink.Primary.copy(alpha = 0.92f),
        )
    }
}
