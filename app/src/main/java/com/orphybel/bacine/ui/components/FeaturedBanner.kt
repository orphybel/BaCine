package com.orphybel.bacine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.R
import com.orphybel.bacine.data.remote.TmdbImages
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Ink
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import com.orphybel.bacine.ui.theme.TvDimens
import java.util.Locale

/**
 * The hero at the top of the home screen.
 *
 * Two scrims do the work: a horizontal one so the text sits on a dark field
 * whatever the artwork looks like, and a vertical one that dissolves the image
 * into the first row of posters below.
 */
@Composable
fun FeaturedBanner(
    movie: Movie,
    onPlayTrailer: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    playFocusRequester: FocusRequester? = null,
) {
    Box(modifier = modifier.fillMaxWidth().height(BANNER_HEIGHT)) {
        NetworkImage(
            url = TmdbImages.backdrop(movie.backdropPath),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            maxWidthPx = 1280,
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Ink.copy(alpha = 0.96f),
                    0.45f to Ink.copy(alpha = 0.75f),
                    1f to Color.Transparent,
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Ink,
                )
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = TvDimens.ScreenPaddingHorizontal,
                    end = TvDimens.ScreenPaddingHorizontal,
                    bottom = 24.dp,
                )
                .width(760.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (movie.year.isNotBlank()) {
                    Text(
                        text = movie.year,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                    )
                }
                if (movie.hasMeaningfulRating) {
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
                            text = String.format(Locale.FRANCE, "%.1f", movie.voteAverage),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            if (movie.overview.isNotBlank()) {
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvButton(
                    text = stringResource(R.string.action_watch_trailer),
                    onClick = onPlayTrailer,
                    icon = BaCineIcons.Play,
                    focusRequester = playFocusRequester,
                )
                TvButton(
                    text = stringResource(R.string.action_details),
                    onClick = onOpenDetails,
                    primary = false,
                )
            }
        }
    }
}

private val BANNER_HEIGHT = 460.dp
