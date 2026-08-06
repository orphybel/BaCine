package com.orphybel.bacine.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.data.remote.TmdbImages
import com.orphybel.bacine.domain.CastMember
import com.orphybel.bacine.domain.MovieDetails
import com.orphybel.bacine.domain.Provider
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.components.BaCineIcons
import com.orphybel.bacine.ui.components.LoadingState
import com.orphybel.bacine.ui.components.MessageState
import com.orphybel.bacine.ui.components.NetworkImage
import com.orphybel.bacine.ui.components.TvButton
import com.orphybel.bacine.ui.components.TvFocusable
import com.orphybel.bacine.ui.components.TvTag
import com.orphybel.bacine.ui.LocalAppContainer
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.player.openInYouTube
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Ink
import com.orphybel.bacine.ui.theme.Surface2
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import com.orphybel.bacine.ui.theme.TvDimens
import java.util.Locale

/**
 * @param onPlayTrailer takes no video key: the player screen resolves the
 *   trailer from the repository's cache, so both this screen and the home
 *   banner can open it with nothing but a movie id.
 */
@Composable
fun DetailScreen(
    movieId: Int,
    onPlayTrailer: () -> Unit,
    onPlayVideo: (videoKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = containerViewModel(key = "detail_$movieId") {
        DetailViewModel(it.repository, movieId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        is UiState.Loading -> LoadingState(modifier)
        is UiState.Failure -> MessageState(
            message = stringResource(R.string.state_error),
            detail = current.error.message,
            onRetry = { viewModel.load(forceRefresh = true) },
            modifier = modifier,
        )
        is UiState.Success -> DetailContent(
            details = current.data,
            onPlayTrailer = onPlayTrailer,
            onPlayVideo = onPlayVideo,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailContent(
    details: MovieDetails,
    onPlayTrailer: () -> Unit,
    onPlayVideo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val movie = details.movie
    val trailer = details.bestTrailer
    val trailerFocus = remember { FocusRequester() }

    val watchIds by container.settings.watchlistIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val inWatchlist = movie.id in watchIds

    LaunchedEffect(movie.id) {
        runCatching { trailerFocus.requestFocus() }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "hero") {
            Box(modifier = Modifier.fillMaxWidth().height(HERO_HEIGHT)) {
                NetworkImage(
                    url = TmdbImages.backdrop(movie.backdropPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    maxWidthPx = 1280,
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Ink.copy(alpha = 0.55f),
                            0.55f to Ink.copy(alpha = 0.85f),
                            1f to Ink,
                        )
                    )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = TvDimens.ScreenPaddingHorizontal,
                            end = TvDimens.ScreenPaddingHorizontal,
                            bottom = 20.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    NetworkImage(
                        url = TmdbImages.poster(movie.posterPath),
                        contentDescription = null,
                        modifier = Modifier
                            .width(180.dp)
                            .height(270.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )

                    Column(
                        modifier = Modifier.width(880.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (details.tagline.isNotBlank()) {
                            Text(
                                text = details.tagline,
                                style = MaterialTheme.typography.titleMedium,
                                color = Gold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        MetaLine(details)
                        if (details.genres.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                details.genres.take(4).forEach { genre ->
                                    TvTag(text = genre.name)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            TvButton(
                                text = stringResource(R.string.action_watch_trailer),
                                onClick = onPlayTrailer,
                                icon = BaCineIcons.Play,
                                enabled = trailer != null,
                                focusRequester = trailerFocus,
                            )
                            // The embedded player is the fragile part on TV
                            // firmware. Offering the handoff up front spares
                            // the user having to watch it fail first.
                            if (trailer != null) {
                                TvButton(
                                    text = stringResource(R.string.action_watch_on_youtube),
                                    onClick = {
                                        if (!openInYouTube(context, trailer.key)) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.player_youtube_missing),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    },
                                    primary = false,
                                )
                            }
                            // The same toggle as a long press on a poster, but
                            // discoverable — a plain button labelled with its
                            // current state.
                            TvButton(
                                text = stringResource(
                                    if (inWatchlist) R.string.watchlist_saved else R.string.watchlist_save,
                                ),
                                onClick = { scope.launch { container.settings.toggleWatchlist(movie) } },
                                icon = BaCineIcons.Bookmark,
                                primary = false,
                            )
                        }
                        if (trailer == null) {
                            Text(
                                text = stringResource(R.string.detail_no_trailer),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        item(key = "synopsis") {
            Section(title = stringResource(R.string.detail_synopsis)) {
                Text(
                    text = movie.overview.ifBlank { stringResource(R.string.detail_no_synopsis) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(horizontal = TvDimens.ScreenPaddingHorizontal)
                        .width(1100.dp),
                )
            }
        }

        if (!details.providers.isEmpty) {
            item(key = "providers") {
                ProviderSection(details)
            }
        }

        if (details.otherVideos.isNotEmpty()) {
            item(key = "other_videos") {
                Section(title = stringResource(R.string.detail_other_videos)) {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = TvDimens.ScreenPaddingHorizontal
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(details.otherVideos, key = { it.key }) { video ->
                            TvButton(
                                text = video.name.ifBlank { video.type },
                                onClick = { onPlayVideo(video.key) },
                                icon = BaCineIcons.Play,
                                primary = false,
                            )
                        }
                    }
                }
            }
        }

        if (details.cast.isNotEmpty()) {
            item(key = "cast") {
                Section(title = stringResource(R.string.detail_cast)) {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = TvDimens.ScreenPaddingHorizontal
                        ),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        items(details.cast, key = { it.id }) { member -> CastCard(member) }
                    }
                }
            }
        }

        item(key = "bottom") {
            Spacer(Modifier.height(TvDimens.ScreenPaddingVertical))
        }
    }
}

@Composable
private fun MetaLine(details: MovieDetails) {
    val runtime = details.runtimeMinutes?.let { stringResource(R.string.detail_runtime, it) }
    val parts = listOfNotNull(
        details.movie.year.takeIf { it.isNotBlank() },
        runtime,
        details.directors.firstOrNull(),
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (details.movie.hasMeaningfulRating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = BaCineIcons.Star,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = String.format(Locale.FRANCE, "%.1f", details.movie.voteAverage),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
        }
        if (parts.isNotEmpty()) {
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun ProviderSection(details: MovieDetails) {
    val title = details.movie.title
    val link = details.providers.link
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProviderRow(stringResource(R.string.detail_providers_rent), details.providers.rent, title, link)
        ProviderRow(stringResource(R.string.detail_providers_buy), details.providers.buy, title, link)
        ProviderRow(
            stringResource(R.string.detail_providers_flatrate),
            details.providers.flatrate,
            title,
            link,
        )
        Text(
            text = stringResource(R.string.detail_providers_hint),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = TvDimens.ScreenPaddingHorizontal),
        )
    }
}

@Composable
private fun ProviderRow(
    title: String,
    providers: List<Provider>,
    movieTitle: String,
    justWatchLink: String?,
) {
    if (providers.isEmpty()) return

    val context = LocalContext.current

    Section(title = title) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = TvDimens.ScreenPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(providers, key = { it.id }) { provider ->
                TvFocusable(
                    onClick = {
                        val message = when (openProvider(context, provider, movieTitle, justWatchLink)) {
                            ProviderOpenResult.APP ->
                                context.getString(R.string.provider_opening, provider.name)
                            ProviderOpenResult.WEB ->
                                context.getString(R.string.provider_opening_web)
                            ProviderOpenResult.NONE ->
                                context.getString(R.string.provider_open_failed, provider.name)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    scaleOnFocus = 1.06f,
                    background = Surface2,
                ) { _ ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(10.dp),
                    ) {
                        NetworkImage(
                            url = TmdbImages.logo(provider.logoPath),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            placeholder = Color.Transparent,
                        )
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    Column(
        modifier = Modifier.width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NetworkImage(
            url = TmdbImages.profile(member.profilePath),
            contentDescription = null,
            modifier = Modifier.size(96.dp).clip(CircleShape),
        )
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (member.character.isNotBlank()) {
            Text(
                text = member.character,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = TvDimens.ScreenPaddingHorizontal),
        )
        content()
    }
}

private val HERO_HEIGHT = 520.dp
