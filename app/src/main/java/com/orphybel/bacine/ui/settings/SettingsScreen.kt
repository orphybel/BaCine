package com.orphybel.bacine.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.ui.components.TvButton
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import com.orphybel.bacine.ui.theme.TvDimens

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val viewModel = containerViewModel(key = "settings") {
        SettingsViewModel(it.settings, it.tmdbClient)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = TvDimens.ScreenPaddingHorizontal,
                vertical = TvDimens.ScreenPaddingVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_api_key),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = when {
                    state.storedKey.isNotBlank() ->
                        stringResource(R.string.settings_api_key_set, state.maskedStoredKey)
                    state.hasBuildTimeKey -> stringResource(R.string.settings_api_key_from_build)
                    else -> stringResource(R.string.settings_api_key_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            ApiKeyEditor(state = state, onSave = viewModel::save)

            if (state.storedKey.isNotBlank()) {
                TvButton(
                    text = stringResource(R.string.settings_change_key),
                    onClick = viewModel::clearKey,
                    primary = false,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_region),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = state.region,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            // TMDB's terms require this attribution, and the provider data is
            // JustWatch's — neither line is decorative.
            Text(
                text = stringResource(R.string.settings_attribution),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.width(900.dp),
            )
            Text(
                text = "themoviedb.org · justwatch.com",
                style = MaterialTheme.typography.labelMedium,
                color = Gold,
            )
        }
    }
}
