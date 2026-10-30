package info.quiete.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

/**
 * Local persistence. Deliberately SharedPreferences and not a database: the entire
 * state of this launcher is a short ordered list of components plus a counter map.
 */
class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("quiete", Context.MODE_PRIVATE)
    private val packageManager = context.packageManager

    // ---- Pinned apps (the home screen list) --------------------------------

    /** `null` when the user has never had a home screen — the caller then seeds it. */
    fun pinnedOrNull(): List<ComponentName>? =
        prefs.getString(KEY_PINNED, null)
            ?.split('\n')
            ?.mapNotNull { ComponentName.unflattenFromString(it) }

    fun pinned(): List<ComponentName> = pinnedOrNull().orEmpty()

    fun save(components: List<ComponentName>) {
        prefs.edit {
            putString(KEY_PINNED, components.joinToString("\n") { it.flattenToString() })
        }
    }

    fun toggle(component: ComponentName) {
        val current = pinned().toMutableList()
        if (!current.remove(component) && current.size < MAX_PINNED) current.add(component)
        save(current)
    }

    /**
     * Packages to seed the first-run home screen with.
     *
     * Resolving intents rather than hardcoding names lands us on whatever
     * dialer/browser/camera the device actually ships. Only the *package* is
     * returned, never the resolved component: the activity that handles ACTION_DIAL
     * is not the same activity that appears in the launcher, so the caller has to
     * map each package back to its own launcher entry.
     */
    fun defaultPackages(): List<String> = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:")),
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")),
        Intent("android.media.action.STILL_IMAGE_CAMERA"),
        Intent(android.provider.Settings.ACTION_SETTINGS),
    ).mapNotNull { intent ->
        packageManager.resolveActivity(intent, 0)
            ?.activityInfo
            ?.packageName
            ?.takeIf { it != "android" }
    }.distinct()

    // ---- Usage counts (search ranking) -------------------------------------

    fun recordLaunch(component: ComponentName) {
        val key = KEY_USAGE + component.flattenToShortString()
        prefs.edit { putInt(key, prefs.getInt(key, 0) + 1) }
    }

    fun launchCount(component: ComponentName): Int =
        prefs.getInt(KEY_USAGE + component.flattenToShortString(), 0)

    private companion object {
        const val KEY_PINNED = "pinned"
        const val KEY_USAGE = "usage:"
        const val MAX_PINNED = 7
    }
}
