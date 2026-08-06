package com.orphybel.bacine.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.ui.theme.Gold

/**
 * The single interactive primitive in the app.
 *
 * Everything a remote can land on goes through this, so focus always looks and
 * behaves the same: the item lifts slightly and gains a gold outline, and the
 * D-pad centre key activates it. `clickable` already makes the node focusable
 * and asks the enclosing scrollable to bring it into view, which is what makes
 * long rows navigable without any manual scroll handling.
 *
 * @param content receives whether it is currently focused, so callers can
 *   reveal extra detail (a title, a synopsis) only on the focused item.
 */
@Composable
fun TvFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleOnFocus: Float = 1.06f,
    borderWidth: Dp = 3.dp,
    background: Color = Color.Transparent,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (focused) scaleOnFocus else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 900f),
        label = "focusScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(background)
            .border(
                width = if (focused) borderWidth else 0.dp,
                color = if (focused) Gold else Color.Transparent,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                // The border and the lift are the focus affordance; a ripple
                // adds nothing on a device with no touch input.
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        content(focused)
    }
}
