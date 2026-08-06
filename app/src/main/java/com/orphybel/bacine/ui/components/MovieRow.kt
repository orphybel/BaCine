package com.orphybel.bacine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TvDimens
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A titled horizontal shelf of posters.
 *
 * Paging is driven by what is actually on screen: once focus reaches within a
 * few tiles of the end, [onLoadMore] is asked for the next page, so the user
 * never meets the end of a row while scrolling.
 */
@Composable
fun MovieRow(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    firstItemFocusRequester: FocusRequester? = null,
) {
    if (movies.isEmpty()) return

    val listState = rememberLazyListState()

    LaunchedEffect(listState, movies.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= movies.size - LOAD_MORE_THRESHOLD) onLoadMore()
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.padding(
                start = TvDimens.ScreenPaddingHorizontal,
                end = TvDimens.ScreenPaddingHorizontal,
                bottom = 12.dp,
            ),
        )
        LazyRow(
            state = listState,
            // The padding doubles as breathing room for the focus outline,
            // which would otherwise be clipped at the edges of the screen.
            contentPadding = PaddingValues(horizontal = TvDimens.ScreenPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
        ) {
            items(items = movies, key = { it.id }) { movie ->
                MoviePosterCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) },
                    // A row sets the tile width; in the catalog grid the cell does.
                    modifier = Modifier.width(TvDimens.PosterWidth),
                    focusRequester = firstItemFocusRequester
                        ?.takeIf { movie.id == movies.first().id },
                )
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 5
