package info.quiete.launcher.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Type
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Time and date.
 *
 * Set in sentence case, not uppercase. Wide uppercase captions are a poster
 * convention; at interface density they slow reading down for no gain, and every
 * one of the reference products uses sentence case for exactly this kind of
 * supporting line.
 */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Tick exactly on the minute boundary rather than every 60s from an arbitrary
    // start, so the displayed minute never lags behind the system clock.
    LaunchedEffect(Unit) {
        while (true) {
            val next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            delay(ChronoUnit.MILLIS.between(LocalDateTime.now(), next).coerceAtLeast(250))
            now = LocalDateTime.now()
        }
    }

    val locale = Locale.getDefault()
    val time = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val date = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMMM", locale) }

    Column(modifier) {
        Text(
            text = now.format(time),
            style = Type.Hero,
            color = Ink.Primary,
        )
        Spacer(Modifier.height(Space.Tight))
        Text(
            text = now.format(date).replaceFirstChar { it.titlecase(locale) },
            style = Type.Caption,
            color = Ink.Secondary,
        )
    }
}
