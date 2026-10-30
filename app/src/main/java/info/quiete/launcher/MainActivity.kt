package info.quiete.launcher

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import info.quiete.launcher.ui.LauncherRoot
import info.quiete.launcher.ui.theme.LocalAmbient
import info.quiete.launcher.ui.theme.rememberAmbient

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** Last radius pushed to the window manager, so we only touch it when it changes. */
    private var appliedBlur = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // A launcher never wants light system-bar icons: it draws on an unknown
        // wallpaper and its own type is always light.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            val apps by viewModel.results.collectAsState()
            val pinned by viewModel.pinned.collectAsState()
            val query by viewModel.query.collectAsState()
            val homeSignal by viewModel.homeSignal.collectAsState()
            val ambient by rememberAmbient()

            CompositionLocalProvider(LocalAmbient provides ambient) {
                LauncherRoot(
                    apps = apps,
                    pinned = pinned,
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onLaunch = viewModel::launch,
                    onTogglePin = viewModel::togglePin,
                    onAppInfo = viewModel::openAppInfo,
                    isPinned = viewModel::isPinned,
                    onProgress = ::applyBlur,
                    homeSignal = homeSignal,
                )
            }
        }
    }

    /**
     * Blur the *wallpaper itself* as the drawer rises.
     *
     * `Modifier.blur` cannot do this — it only blurs a composable's own content,
     * not the window behind it. Real depth needs the platform's cross-window blur,
     * which is why this lives in the Activity and is driven by the drawer's
     * progress rather than by anything in Compose.
     */
    private fun applyBlur(progress: Float) {
        if (!windowManager.isCrossWindowBlurEnabled) return

        // Quantised to 8px: a window-attribute write is a binder call, and at 120Hz
        // an unquantised value would issue one every frame of the drag.
        val radius = ((progress * MAX_BLUR_PX) / 8).toInt() * 8
        if (radius == appliedBlur) return
        appliedBlur = radius

        window.attributes = window.attributes.apply {
            blurBehindRadius = radius
            flags = if (radius > 0) {
                flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            } else {
                flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            }
        }
    }

    /**
     * HOME pressed while we are already the foreground activity. Because the
     * Activity is `singleTask`, the system delivers a new intent instead of
     * restarting us, so this is the only place that press is observable.
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        viewModel.onHomePressed()
    }

    private companion object {
        const val MAX_BLUR_PX = 64f
    }
}
