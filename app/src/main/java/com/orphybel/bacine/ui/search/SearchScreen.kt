package com.orphybel.bacine.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.components.LoadingState
import com.orphybel.bacine.ui.components.MessageState
import com.orphybel.bacine.ui.components.MoviePosterCard
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Surface2
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import com.orphybel.bacine.ui.theme.TvDimens

@Composable
fun SearchScreen(
    onOpenMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = containerViewModel(key = "search") { SearchViewModel(it.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fieldFocus = remember { FocusRequester() }

    // Land on the text field: on a TV the first thing the user wants is the
    // on-screen keyboard, not the empty result grid.
    LaunchedEffect(Unit) {
        runCatching { fieldFocus.requestFocus() }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(top = TvDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OutlinedTextField(
            value = state.term,
            onValueChange = viewModel::onTermChange,
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Surface2,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = Gold,
                unfocusedLabelColor = TextSecondary,
                cursorColor = Gold,
            ),
            modifier = Modifier
                .padding(horizontal = TvDimens.ScreenPaddingHorizontal)
                .width(640.dp)
                .focusRequester(fieldFocus),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.error != null -> MessageState(
                    message = stringResource(R.string.state_error),
                    detail = state.error?.message,
                    onRetry = viewModel::retry,
                )

                state.searching && state.results.isEmpty() -> LoadingState()

                state.showEmptyPrompt -> MessageState(
                    message = stringResource(R.string.search_empty),
                )

                state.showNoResults -> MessageState(
                    message = stringResource(R.string.search_no_result),
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = TvDimens.ScreenPaddingHorizontal,
                        end = TvDimens.ScreenPaddingHorizontal,
                        bottom = TvDimens.ScreenPaddingVertical,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
                    verticalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
                ) {
                    items(state.results, key = { it.id }) { movie ->
                        MoviePosterCard(movie = movie, onClick = { onOpenMovie(movie) })
                    }
                }
            }
        }
    }
}

private const val COLUMNS = 6
