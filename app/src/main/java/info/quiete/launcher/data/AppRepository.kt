package info.quiete.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Process
import android.os.UserHandle
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * Source of truth for the installed-app list.
 *
 * Backed by [LauncherApps] rather than PackageManager: it is the API the platform
 * intends launchers to use, it reports install/remove/update callbacks without a
 * BroadcastReceiver, and [LauncherApps.startMainActivity] accepts source bounds so
 * the system can run the zoom-from-the-row open animation.
 */
class AppRepository(private val context: Context) {

    private val launcherApps: LauncherApps = checkNotNull(context.getSystemService())
    private val user: UserHandle = Process.myUserHandle()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    /** Locale-aware sort, so "Ä" files next to "A" for an Italian or German user. */
    private val collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String?, u: UserHandle?) = invalidate()
        override fun onPackageRemoved(packageName: String?, u: UserHandle?) = invalidate()
        override fun onPackageChanged(packageName: String?, u: UserHandle?) = invalidate()
        override fun onPackagesAvailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = invalidate()
        override fun onPackagesUnavailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = invalidate()
    }

    private var scope: CoroutineScope? = null

    fun start(scope: CoroutineScope) {
        this.scope = scope
        launcherApps.registerCallback(callback)
        invalidate()
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
        scope = null
    }

    private fun invalidate() {
        scope?.launch { _apps.value = load() }
    }

    private suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        launcherApps.getActivityList(null, user)
            .asSequence()
            // Hide ourselves: tapping "Quiete" from inside Quiete does nothing useful.
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                AppEntry(
                    label = info.label.toString(),
                    component = info.componentName,
                )
            }
            .sortedWith(compareBy(collator) { it.label })
            .toList()
    }

    /** Load a single icon on demand — never eagerly for the whole list. */
    suspend fun icon(entry: AppEntry, densityDpi: Int) = withContext(Dispatchers.IO) {
        runCatching {
            launcherApps.getActivityList(entry.packageName, user)
                .firstOrNull { it.componentName == entry.component }
                ?.getIcon(densityDpi)
        }.getOrNull()
    }

    /**
     * [sourceBounds] are the on-screen coordinates of the tapped row. Passing them
     * lets the system animate the app opening *out of that row* instead of the
     * generic full-screen fade.
     */
    fun launch(entry: AppEntry, sourceBounds: Rect? = null) {
        runCatching {
            launcherApps.startMainActivity(entry.component, user, sourceBounds, null)
        }
    }

    fun openAppInfo(entry: AppEntry, sourceBounds: Rect? = null) {
        runCatching {
            launcherApps.startAppDetailsActivity(entry.component, user, sourceBounds, null)
        }
    }

    fun isLaunchable(component: ComponentName): Boolean =
        launcherApps.isActivityEnabled(component, user)
}
