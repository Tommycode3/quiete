package info.quiete.launcher.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Swap this one line to change the entire voice of the launcher.
 * Roboto ships on every device; a bundled variable grotesk drops in here later.
 */
private val Face = FontFamily.SansSerif

/**
 * The type system.
 *
 * Rebuilt around density. The earlier scale had one 86sp display and nothing
 * between it and 12sp captions — dramatic, but it is a poster, not an interface.
 * The reference set (Linear, Notion, Cursor, Apple's grouped lists) all run 13–17sp
 * for nearly everything and earn hierarchy from *weight, colour and position*
 * rather than from size. Size is the crudest lever and the one that costs the most
 * space.
 *
 * Two rules still hold from before, because they are simply correct:
 * optical tracking (large type tightens, small type opens up), and tabular figures
 * anywhere a number changes in place.
 */
object Type {

    private val trimmed = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

    /**
     * The clock. Still the largest thing on screen, but now Medium rather than
     * ExtraLight and 56sp rather than 86 — it has to sit *inside* a panel and hold
     * its own against a border, which hairline type at display size cannot do.
     */
    val Hero = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 56.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2.2).sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = trimmed,
    )

    /** Panel headings and the primary line of a card. */
    val Title = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = trimmed,
    )

    /** The workhorse: app rows, list items, everything you actually read. */
    val Body = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = trimmed,
    )

    /** Supporting line under a title: date, artist, package name, timestamp. */
    val Caption = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = (-0.1).sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = trimmed,
    )

    /** Section labels and A–Z headers. Small, wide, uppercase. */
    val Micro = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp,
    )

    /** The A–Z rail. */
    val Rail = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.4.sp,
    )

    /** The giant letter shown while dragging the rail. */
    val Scrub = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Medium,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2.5).sp,
        lineHeightStyle = trimmed,
    )

    /** Search field. Sized as an input, not as a headline. */
    val Field = TextStyle(
        fontFamily = Face,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = trimmed,
    )
}
