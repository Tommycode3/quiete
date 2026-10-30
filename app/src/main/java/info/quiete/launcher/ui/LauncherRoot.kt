package info.quiete.launcher.ui

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import info.quiete.launcher.data.AppEntry
import info.quiete.launcher.ui.drawer.DrawerScreen
import info.quiete.launcher.ui.home.HomeScreen
import info.quiete.launcher.ui.theme.Motion
import kotlinx.coroutines.launch

/**
 * Owns the single number the whole launcher is built around: `progress`, the
 * drawer's open fraction from 0 (home) to 1 (drawer).
 *
 * Both screens are always composed and both read that one value, which is why the
 * transition is continuous and interruptible — you can catch the drawer mid-flight,
 * push it back down, and it resolves from wherever your finger left it. That is the
 * difference between a launcher that feels like an app and one that feels like the
 * operating system.
 */
@Composable
fun LauncherRoot(
    apps: List<AppEntry>,
    pinned: List<AppEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onAppInfo: (AppEntry, Rect?) -> Unit,
    isPinned: (AppEntry) -> Boolean,
    onProgress: (Float) -> Unit,
    /** Increments every time HOME is pressed while the launcher is already showing. */
    homeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    var height by remember { mutableFloatStateOf(1f) }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }

    val open = progress.value > 0.5f

    /**
     * The only way the drawer ever closes. Every entry point — back gesture, HOME
     * key, flinging it down — routes through here, so the query and the keyboard
     * can never survive a close and greet you stale on the way back in.
     */
    fun close() {
        scope.launch {
            keyboard?.hide()
            onQueryChange("")
            progress.animateTo(0f, Motion.sheet())
        }
    }

    fun settle(velocity: Float) {
        // Flings win over position: a fast flick commits even from 10% open, which
        // is what makes the gesture feel responsive rather than fussy.
        val opening = when {
            velocity < -900f -> true
            velocity > 900f -> false
            else -> progress.value > 0.35f
        }
        if (opening) scope.launch { progress.animateTo(1f, Motion.sheet()) } else close()
    }

    // Lets the drawer be dragged shut by the list itself, but only from the very
    // top — anywhere else, the same downward drag scrolls the list, as it should.
    val nested = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (available.y > 0 && atTop && progress.value > 0f) {
                    scope.launch {
                        progress.snapTo((progress.value - available.y / height).coerceIn(0f, 1f))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity) =
                if (progress.value < 1f) {
                    settle(available.y)
                    available
                } else {
                    androidx.compose.ui.unit.Velocity.Zero
                }
        }
    }

    LaunchedEffect(progress) {
        snapshotFlow { progress.value }.collect(onProgress)
    }

    BackHandler(enabled = open || menuFor != null) {
        when {
            menuFor != null -> menuFor = null
            else -> close()
        }
    }

    // HOME pressed while already home. The system delivers a fresh intent rather
    // than restarting us, so this is the only signal that it happened — and the
    // expected behaviour is "put everything back", not "do nothing".
    LaunchedEffect(homeSignal) {
        if (homeSignal == 0) return@LaunchedEffect
        menuFor = null
        if (progress.value > 0f) close()
    }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
            .nestedScroll(nested)
            .draggable(
                orientation = Orientation.Vertical,
                // Once open, the list owns vertical drags; the nested-scroll
                // connection above hands them back at the top of the list.
                enabled = !open,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        progress.snapTo((progress.value - delta / height).coerceIn(0f, 1f))
                    }
                },
                onDragStopped = { velocity -> settle(velocity) },
            ),
    ) {
        HomeScreen(
            pinned = pinned,
            progress = progress.value,
            onLaunch = onLaunch,
            onLongPress = { entry, _ -> menuFor = entry },
        )

        if (progress.value > 0f) {
            DrawerScreen(
                apps = apps,
                query = query,
                progress = progress.value,
                listState = listState,
                onQueryChange = onQueryChange,
                onLaunch = onLaunch,
                onLongPress = { entry, _ -> menuFor = entry },
            )
        }

        menuFor?.let { entry ->
            ActionSheet(
                entry = entry,
                pinned = isPinned(entry),
                onDismiss = { menuFor = null },
                onTogglePin = { onTogglePin(entry); menuFor = null },
                onAppInfo = { onAppInfo(entry, null); menuFor = null },
            )
        }
    }
}
