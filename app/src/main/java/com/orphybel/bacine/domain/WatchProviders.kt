package com.orphybel.bacine.domain

/**
 * Where a movie can be watched in one country. Sourced from JustWatch through
 * TMDB, which is why the attribution in the settings screen is not optional.
 */
data class WatchProviders(
    val rent: List<Provider> = emptyList(),
    val buy: List<Provider> = emptyList(),
    val flatrate: List<Provider> = emptyList(),
    /** JustWatch page for this movie, if TMDB gave us one. */
    val link: String? = null,
) {
    /** True when the movie is actually purchasable or rentable — paid VOD. */
    val hasPaidVod: Boolean get() = rent.isNotEmpty() || buy.isNotEmpty()

    val isEmpty: Boolean get() = rent.isEmpty() && buy.isEmpty() && flatrate.isEmpty()

    /** De-duplicated providers across every monetization type, for compact rows. */
    val all: List<Provider>
        get() = (rent + buy + flatrate).distinctBy { it.id }

    companion object {
        val EMPTY = WatchProviders()
    }
}

data class Provider(
    val id: Int,
    val name: String,
    val logoPath: String?,
)
