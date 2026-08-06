package com.orphybel.bacine.ui.nav

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.ui.components.TvFocusable
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Ink
import com.orphybel.bacine.ui.theme.Surface1
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary

/**
 * The left navigation rail.
 *
 * Collapsed to icons while the user is browsing content, and expanded to show
 * labels as soon as focus enters it — the standard Google TV shape, and the
 * reason the rail costs almost no horizontal space most of the time.
 */
@Composable
fun NavRail(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val width by animateDpAsState(
        targetValue = if (expanded) EXPANDED_WIDTH else COLLAPSED_WIDTH,
        animationSpec = tween(180),
        label = "railWidth",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(180),
        label = "railLabel",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(if (expanded) Surface1 else Ink)
            .onFocusChanged { expanded = it.hasFocus }
            .padding(vertical = 28.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Destination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            TvFocusable(
                onClick = { onNavigate(destination) },
                modifier = Modifier.fillMaxWidth(),
                scaleOnFocus = 1f,
                background = if (selected) Gold.copy(alpha = 0.18f) else Ink,
            ) { focused ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        tint = if (selected || focused) Gold else TextSecondary,
                        modifier = Modifier.size(26.dp),
                    )
                    // Only composed while the rail is open: at 76dp collapsed
                    // there is no room for a label, and a zero-alpha one would
                    // still squeeze the icon out of its own row.
                    if (labelAlpha > 0.01f) {
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected || focused) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.alpha(labelAlpha),
                        )
                    }
                }
            }
        }
    }
}

private val COLLAPSED_WIDTH = 76.dp
private val EXPANDED_WIDTH = 240.dp
