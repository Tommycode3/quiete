package info.quiete.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import info.quiete.launcher.data.AppEntry
import info.quiete.launcher.ui.effect.Panel
import info.quiete.launcher.ui.effect.PanelDivider
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Type
import info.quiete.launcher.ui.theme.Veil

/**
 * The long-press menu, as two stacked panels.
 *
 * This is the iOS action-sheet skeleton — an identity panel, then a panel of
 * actions — and it is used here for the same reason it works there: separating
 * *what you are acting on* from *what you can do to it* removes the only real
 * ambiguity a context menu has. Both panels use the same surface primitive as the
 * home screen, so the menu reads as the same material rising, not as a new widget.
 */
@Composable
fun ActionSheet(
    entry: AppEntry,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Veil.Modal)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .systemBarsPadding()
                .padding(horizontal = Space.Gutter, vertical = Space.Loose),
        ) {
            Panel {
                Text(
                    text = entry.label,
                    style = Type.Title,
                    color = Ink.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Space.Hair))
                Text(
                    text = entry.packageName,
                    style = Type.Caption,
                    color = Ink.Tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Space.Snug))

            Panel(padding = Space.Tight) {
                Action(
                    label = if (pinned) "Togli dalla home" else "Aggiungi alla home",
                    onClick = onTogglePin,
                )
                PanelDivider(startInset = Space.Snug)
                Action(label = "Informazioni app", onClick = onAppInfo)
            }
        }
    }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(Space.Row)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Space.Snug),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = label, style = Type.Body, color = Ink.Primary)
    }
}
