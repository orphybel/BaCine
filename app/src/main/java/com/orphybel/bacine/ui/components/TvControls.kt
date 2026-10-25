package com.orphybel.bacine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Ink
import com.orphybel.bacine.ui.theme.Surface2
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary

/**
 * Buttons and chips share [TvFocusable], so focus behaves identically whether
 * the remote lands on a poster, a filter or an action.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        scaleOnFocus = 1.04f,
        background = if (primary) Gold else Surface2,
    ) { focused ->
        val contentColor = when {
            !enabled -> TextSecondary
            primary -> Ink
            focused -> TextPrimary
            else -> TextPrimary
        }
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            // A label never wraps: squeezed by a crowded row, a wrapping label
            // folds one letter per line into a tall sliver of a button.
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** A filter value. Selection is shown by fill, focus by the shared outline. */
@Composable
fun TvChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        shape = RoundedCornerShape(20.dp),
        scaleOnFocus = 1.05f,
        background = if (selected) Gold else Surface2,
    ) { _ ->
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

/** Non-interactive pill, for genres and other metadata. */
@Composable
fun TvTag(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Surface2,
    contentColor: Color = TextSecondary,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
        modifier = modifier
            .background(background, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
