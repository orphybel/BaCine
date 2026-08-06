package com.orphybel.bacine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.R
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Gold, modifier = Modifier.size(56.dp))
        Text(
            text = stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/**
 * @param onRetry when null no button is shown — used for states the user cannot
 *   act on by retrying, such as an empty result set.
 */
@Composable
fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actionLabel: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp).width(720.dp),
            )
        }
        if (onRetry != null) {
            TvButton(
                text = actionLabel ?: stringResource(R.string.state_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, message: String? = null) {
    MessageState(
        message = message ?: stringResource(R.string.state_nothing_here),
        modifier = modifier,
    )
}
