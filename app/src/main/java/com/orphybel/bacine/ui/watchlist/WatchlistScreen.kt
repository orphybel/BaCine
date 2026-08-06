package com.orphybel.bacine.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.LocalAppContainer
import com.orphybel.bacine.ui.components.MessageState
import com.orphybel.bacine.ui.components.MoviePosterCard
import com.orphybel.bacine.ui.theme.TvDimens

/**
 * The "À voir" list.
 *
 * Reads straight from settings — the saved films are stored whole, so this
 * needs no view model and no network: it renders from disk. Long-pressing a
 * poster here removes it, the mirror of the gesture that added it elsewhere.
 */
@Composable
fun WatchlistScreen(
    onOpenMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val movies by container.settings.watchlist.collectAsStateWithLifecycle(initialValue = emptyList())
    val onLongPress = rememberWatchlistToggle()

    if (movies.isEmpty()) {
        MessageState(
            message = stringResource(R.string.watchlist_empty_title),
            detail = stringResource(R.string.watchlist_empty_hint),
            modifier = modifier,
        )
        return
    }

    val firstItemFocus = remember { FocusRequester() }

    // Coming in from the rail, land focus on the first poster rather than
    // nowhere. Keyed on the first id so it does not fight the user for focus on
    // every removal, only when the head of the list actually changes.
    LaunchedEffect(movies.firstOrNull()?.id) {
        runCatching { firstItemFocus.requestFocus() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TvDimens.ScreenPaddingHorizontal,
            end = TvDimens.ScreenPaddingHorizontal,
            top = TvDimens.ScreenPaddingVertical,
            bottom = TvDimens.ScreenPaddingVertical,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
        verticalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
    ) {
        items(movies, key = { it.id }) { movie ->
            MoviePosterCard(
                movie = movie,
                onClick = { onOpenMovie(movie) },
                focusRequester = firstItemFocus.takeIf { movie.id == movies.first().id },
                onLongClick = { onLongPress(movie) },
            )
        }

        item(key = "bottom", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(TvDimens.ScreenPaddingVertical))
        }
    }
}

private const val COLUMNS = 6
