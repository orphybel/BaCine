package com.orphybel.bacine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orphybel.bacine.R
import com.orphybel.bacine.data.remote.TmdbImages
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Surface2
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import java.util.Locale

/**
 * A poster tile. The title stays permanently visible rather than appearing on
 * focus only — on a projector, a wall of untitled artwork is hard to scan.
 */
@Composable
fun MoviePosterCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onLongClick: (() -> Unit)? = null,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        background = Surface2,
        onLongClick = onLongClick,
    ) { focused ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                NetworkImage(
                    url = TmdbImages.poster(movie.posterPath),
                    contentDescription = stringResource(R.string.cd_poster, movie.title),
                    // Posters are a 2:3 format; deriving the height from the
                    // width lets the same card work in a fixed-width row and in
                    // a grid cell whose width the layout decides.
                    modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT_RATIO),
                    fallback = {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp),
                        )
                    },
                )
                if (movie.hasMeaningfulRating) {
                    RatingBadge(
                        rating = movie.voteAverage,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (focused) TextPrimary else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (movie.year.isNotBlank()) {
                    Text(
                        text = movie.year,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

/** TMDB posters are 2:3. */
private const val POSTER_ASPECT_RATIO = 2f / 3f

@Composable
private fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = BaCineIcons.Star,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = String.format(Locale.FRANCE, "%.1f", rating),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
        )
    }
}
