package com.orphybel.bacine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import com.orphybel.bacine.ui.theme.TvDimens

/**
 * First-run screen, shown only when no key is available from either the build
 * or storage.
 *
 * @param onDone called once TMDB has accepted a key, so the caller can move on
 *   to the home screen.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = containerViewModel(key = "onboarding") {
        SettingsViewModel(it.settings, it.tmdbClient)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedAt) {
        if (state.savedAt != null) {
            viewModel.consumeSaved()
            onDone()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = TvDimens.ScreenPaddingHorizontal,
                vertical = TvDimens.ScreenPaddingVertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(56.dp),
    ) {
        Column(
            modifier = Modifier.width(640.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Text(
                text = "themoviedb.org",
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
            )
        }

        ApiKeyEditor(
            state = state,
            onSave = viewModel::save,
            autoFocus = true,
            modifier = Modifier.padding(top = 60.dp),
        )
    }
}
