package com.orphybel.bacine.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.ErrorKind
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.components.FeaturedBanner
import com.orphybel.bacine.ui.components.LoadingState
import com.orphybel.bacine.ui.components.MessageState
import com.orphybel.bacine.ui.components.MovieRow
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.theme.TvDimens

@Composable
fun HomeScreen(
    onOpenMovie: (Movie) -> Unit,
    onPlayTrailer: (Movie) -> Unit,
    onMissingKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = containerViewModel(key = "home") { HomeViewModel(it.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        is UiState.Loading -> LoadingState(modifier)

        is UiState.Failure -> {
            // A missing key is not something the user can retry their way out
            // of; send them straight to the screen that fixes it.
            LaunchedEffect(current.error.kind) {
                if (current.error.kind == ErrorKind.MISSING_KEY) onMissingKey()
            }
            MessageState(
                message = stringResource(R.string.state_error),
                detail = current.error.message,
                onRetry = viewModel::load,
                modifier = modifier,
            )
        }

        is UiState.Success -> HomeContentList(
            content = current.data,
            onOpenMovie = onOpenMovie,
            onPlayTrailer = onPlayTrailer,
            onLoadMore = viewModel::loadMore,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeContentList(
    content: HomeContent,
    onOpenMovie: (Movie) -> Unit,
    onPlayTrailer: (Movie) -> Unit,
    onLoadMore: (HomeRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (content.isEmpty) {
        MessageState(message = stringResource(R.string.state_empty), modifier = modifier)
        return
    }

    val firstActionFocus = remember { FocusRequester() }

    // Park focus on the banner's trailer button so the remote has somewhere
    // sensible to start the moment the screen appears.
    LaunchedEffect(content.featured?.id) {
        runCatching { firstActionFocus.requestFocus() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TvDimens.RowSpacing),
    ) {
        content.featured?.let { featured ->
            item(key = "featured") {
                FeaturedBanner(
                    movie = featured,
                    onPlayTrailer = { onPlayTrailer(featured) },
                    onOpenDetails = { onOpenMovie(featured) },
                    playFocusRequester = firstActionFocus,
                )
            }
        }

        item(key = "now_playing") {
            MovieRow(
                title = stringResource(R.string.row_now_playing),
                movies = content.nowPlaying,
                onMovieClick = onOpenMovie,
                onLoadMore = { onLoadMore(HomeRow.NOW_PLAYING) },
            )
        }

        item(key = "vod") {
            MovieRow(
                title = stringResource(R.string.row_vod),
                movies = content.vod,
                onMovieClick = onOpenMovie,
                onLoadMore = { onLoadMore(HomeRow.VOD) },
            )
        }

        item(key = "popular") {
            MovieRow(
                title = stringResource(R.string.row_popular),
                movies = content.popular,
                onMovieClick = onOpenMovie,
                onLoadMore = { onLoadMore(HomeRow.POPULAR) },
            )
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(TvDimens.ScreenPaddingVertical))
        }
    }
}
