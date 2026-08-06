package com.orphybel.bacine.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.orphybel.bacine.R
import com.orphybel.bacine.domain.Availability
import com.orphybel.bacine.domain.CatalogQuery
import com.orphybel.bacine.domain.Genre
import com.orphybel.bacine.domain.SortOrder
import com.orphybel.bacine.ui.components.TvChip
import com.orphybel.bacine.ui.theme.TextSecondary
import com.orphybel.bacine.ui.theme.TvDimens
import java.time.LocalDate

/**
 * Filters, laid out as rows of chips.
 *
 * Chips rather than dropdowns on purpose: a dropdown costs a click to open, a
 * vertical scroll and a click to dismiss, whereas a row of chips is one D-pad
 * press per option and every value stays readable at a glance.
 */
@Composable
fun FilterBar(
    query: CatalogQuery,
    genres: List<Genre>,
    onQueryChange: (CatalogQuery) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Zero when the caller already indents the content — the catalog grid
     * applies the screen padding itself, and Compose forbids negative padding
     * to claw it back.
     */
    horizontalPadding: Dp = TvDimens.ScreenPaddingHorizontal,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterRow(horizontalPadding, label = stringResource(R.string.filter_sort)) {
            items(SortOrder.entries.toList(), key = { it.name }) { order ->
                TvChip(
                    text = stringResource(order.labelRes()),
                    selected = query.sort == order,
                    onClick = { onQueryChange(query.copy(sort = order)) },
                )
            }
        }

        FilterRow(horizontalPadding, label = stringResource(R.string.filter_availability)) {
            items(Availability.entries.toList(), key = { it.name }) { availability ->
                TvChip(
                    text = stringResource(availability.labelRes()),
                    selected = query.availability == availability,
                    onClick = { onQueryChange(query.copy(availability = availability)) },
                )
            }
        }

        if (genres.isNotEmpty()) {
            FilterRow(horizontalPadding, label = stringResource(R.string.filter_genre)) {
                item(key = "genre_all") {
                    TvChip(
                        text = stringResource(R.string.filter_all),
                        selected = query.genreIds.isEmpty(),
                        onClick = { onQueryChange(query.copy(genreIds = emptySet())) },
                    )
                }
                items(genres, key = { it.id }) { genre ->
                    TvChip(
                        text = genre.name,
                        selected = genre.id in query.genreIds,
                        onClick = {
                            // Multi-select: tapping a selected genre removes it.
                            val next = if (genre.id in query.genreIds) {
                                query.genreIds - genre.id
                            } else {
                                query.genreIds + genre.id
                            }
                            onQueryChange(query.copy(genreIds = next))
                        },
                    )
                }
            }
        }

        FilterRow(horizontalPadding, label = stringResource(R.string.filter_year)) {
            item(key = "year_all") {
                TvChip(
                    text = stringResource(R.string.filter_all),
                    selected = query.year == null,
                    onClick = { onQueryChange(query.copy(year = null)) },
                )
            }
            items(recentYears(), key = { it }) { year ->
                TvChip(
                    text = year.toString(),
                    selected = query.year == year,
                    onClick = { onQueryChange(query.copy(year = year)) },
                )
            }
        }

        FilterRow(horizontalPadding, label = stringResource(R.string.filter_rating)) {
            items(RATING_STEPS, key = { it }) { rating ->
                TvChip(
                    text = if (rating == 0) stringResource(R.string.filter_all) else "$rating+",
                    selected = query.minRating == rating,
                    onClick = { onQueryChange(query.copy(minRating = rating)) },
                )
            }
            item(key = "reset") {
                TvChip(
                    text = stringResource(R.string.filter_reset),
                    selected = false,
                    onClick = onReset,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    horizontalPadding: Dp,
    label: String,
    content: LazyListScope.() -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = horizontalPadding, bottom = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

private val RATING_STEPS = listOf(0, 5, 6, 7, 8)

/** TMDB has little worth showing before this; 15 years is plenty on a remote. */
private fun recentYears(): List<Int> {
    val now = LocalDate.now().year
    return (now downTo now - 14).toList()
}

@StringRes
private fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.POPULARITY -> R.string.sort_popularity
    SortOrder.RELEASE_DATE -> R.string.sort_release_date
    SortOrder.RATING -> R.string.sort_rating
    SortOrder.TITLE -> R.string.sort_title
}

@StringRes
private fun Availability.labelRes(): Int = when (this) {
    Availability.ANY -> R.string.availability_any
    Availability.THEATERS -> R.string.availability_theaters
    Availability.RENT -> R.string.availability_rent
    Availability.BUY -> R.string.availability_buy
    Availability.FLATRATE -> R.string.availability_flatrate
}
