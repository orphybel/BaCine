package com.orphybel.bacine.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.domain.CatalogQuery
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.ErrorKind
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.components.LoadingState
import com.orphybel.bacine.ui.components.MessageState
import com.orphybel.bacine.ui.components.MoviePosterCard
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TvDimens
import com.orphybel.bacine.ui.watchlist.rememberWatchlistToggle
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CatalogScreen(
    onOpenMovie: (Movie) -> Unit,
    onMissingKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = containerViewModel(key = "catalog") {
        CatalogViewModel(it.repository, it.settings)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        is UiState.Loading -> LoadingState(modifier)

        is UiState.Failure -> {
            LaunchedEffect(current.error.kind) {
                if (current.error.kind == ErrorKind.MISSING_KEY) onMissingKey()
            }
            MessageState(
                message = stringResource(R.string.state_error),
                detail = current.error.message,
                onRetry = viewModel::reload,
                modifier = modifier,
            )
        }

        is UiState.Success -> CatalogGrid(
            content = current.data,
            onOpenMovie = onOpenMovie,
            onQueryChange = viewModel::updateQuery,
            onReset = viewModel::resetFilters,
            onLoadMore = viewModel::loadMore,
            modifier = modifier,
        )
    }
}

@Composable
private fun CatalogGrid(
    content: CatalogContent,
    onOpenMovie: (Movie) -> Unit,
    onQueryChange: (CatalogQuery) -> Unit,
    onReset: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val onLongPress = rememberWatchlistToggle()

    LaunchedEffect(gridState, content.movies.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= content.movies.size - LOAD_MORE_THRESHOLD) onLoadMore()
            }
    }

    LazyVerticalGrid(
        state = gridState,
        // Fixed rather than adaptive: a predictable column count keeps D-pad
        // left/right movement consistent as the user scrolls.
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
        // The grid already applies the screen padding, so the filter rows must
        // not add their own on top of it.
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            FilterBar(
                query = content.query,
                genres = content.genres,
                onQueryChange = onQueryChange,
                onReset = onReset,
                horizontalPadding = 0.dp,
            )
        }

        if (content.movies.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.state_nothing_here),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 48.dp),
                )
            }
        }

        items(content.movies, key = { it.id }) { movie ->
            // No explicit width: the tile takes the grid cell's width and
            // derives its poster height from the 2:3 aspect ratio.
            MoviePosterCard(
                movie = movie,
                onClick = { onOpenMovie(movie) },
                onLongClick = { onLongPress(movie) },
            )
        }

        item(key = "bottom", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(TvDimens.ScreenPaddingVertical))
        }
    }
}

private const val COLUMNS = 6
private const val LOAD_MORE_THRESHOLD = 12
