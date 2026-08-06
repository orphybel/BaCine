package com.orphybel.bacine.domain

import kotlinx.serialization.Serializable

/** Sort orders offered in the catalog, mapped to TMDB's `sort_by` values. */
enum class SortOrder(val tmdbValue: String) {
    POPULARITY("popularity.desc"),
    RELEASE_DATE("primary_release_date.desc"),
    RATING("vote_average.desc"),
    TITLE("title.asc"),
}

/** Maps to TMDB's `with_watch_monetization_types`, plus an in-theatres case. */
enum class Availability {
    ANY,
    THEATERS,
    RENT,
    BUY,
    FLATRATE;

    val monetizationType: String?
        get() = when (this) {
            RENT -> "rent"
            BUY -> "buy"
            FLATRATE -> "flatrate"
            ANY, THEATERS -> null
        }
}

/**
 * The catalog's filter state. Serialized into DataStore so the user's last set
 * of filters survives a restart, and turned into `/discover/movie` query
 * parameters by [com.orphybel.bacine.data.remote.TmdbClient].
 */
@Serializable
data class CatalogQuery(
    val sort: SortOrder = SortOrder.POPULARITY,
    val genreIds: Set<Int> = emptySet(),
    val year: Int? = null,
    val minRating: Int = 0,
    val availability: Availability = Availability.ANY,
) {
    val isDefault: Boolean get() = this == DEFAULT

    /**
     * Sorting by rating without a vote floor surfaces obscure films with a
     * single 10/10 vote, so the catalog applies one whenever that sort is used.
     */
    val voteCountFloor: Int?
        get() = if (sort == SortOrder.RATING) 200 else null

    companion object {
        val DEFAULT = CatalogQuery()
    }
}
