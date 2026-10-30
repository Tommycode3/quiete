package info.quiete.launcher.ui.drawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import info.quiete.launcher.R
import info.quiete.launcher.ui.theme.Ink
import info.quiete.launcher.ui.theme.Motion
import info.quiete.launcher.ui.theme.Space
import info.quiete.launcher.ui.theme.Type

/**
 * Search, drawn as a line of writing rather than a box.
 *
 * No pill, no rounded container, no magnifier icon — a single hairline underneath
 * that brightens and thickens on focus. A boxed search field would be the only
 * enclosed shape in the entire launcher, and one stray container is what makes an
 * otherwise clean interface look like a template.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val active = focused || value.isNotEmpty()

    val emphasis by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = Motion.standard(),
        label = "fieldEmphasis",
    )

    Column(modifier) {
        Box(contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = Type.Field,
                    color = Ink.Tertiary,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = Type.Field.copy(color = Ink.Primary),
                cursorBrush = SolidColor(Ink.Primary),
                singleLine = true,
                interactionSource = interactions,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // The hairline. Grows from 1dp to 1.5dp and from 12% to 55% ink — a change
        // you register without ever consciously looking at it.
        Box(
            Modifier
                .padding(top = Space.Snug)
                .fillMaxWidth()
                .height(1.dp + 0.5.dp * emphasis)
                .graphicsLayer { alpha = 0.12f + 0.43f * emphasis }
                .background(Ink.Primary),
        )
    }
}
