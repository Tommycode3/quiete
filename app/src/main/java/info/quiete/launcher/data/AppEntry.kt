package info.quiete.launcher.data

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

/**
 * One launchable activity.
 *
 * [icon] is intentionally nullable and loaded lazily: decoding every icon on a
 * device with 200 apps costs tens of megabytes, and this launcher only ever
 * shows an icon at 20dp beside a text row. Text first, always.
 */
@Immutable
data class AppEntry(
    val label: String,
    val component: ComponentName,
    val icon: Drawable? = null,
) {
    val packageName: String get() = component.packageName

    /** Case- and accent-insensitive key used for sorting and A–Z sectioning. */
    val sortKey: String = label.trim().uppercase()

    /** Section letter. Anything not A–Z lands under "#". */
    val section: String
        get() = sortKey.firstOrNull()
            ?.takeIf { it in 'A'..'Z' }
            ?.toString()
            ?: "#"
}
