package com.orphybel.bacine.domain

/** A movie as shown in a row or a grid. */
data class Movie(
    val id: Int,
    val title: String,
    val originalTitle: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    /** ISO date, `yyyy-MM-dd`. Empty when TMDB has no release date. */
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: List<Int>,
) {
    val year: String get() = releaseDate.take(4)

    /** Ratings with almost no votes are noise; don't show them as a score. */
    val hasMeaningfulRating: Boolean get() = voteCount >= 10 && voteAverage > 0.0
}

/** Full movie page: [movie] plus everything fetched with `append_to_response`. */
data class MovieDetails(
    val movie: Movie,
    val runtimeMinutes: Int?,
    val genres: List<Genre>,
    val tagline: String,
    val cast: List<CastMember>,
    val directors: List<String>,
    val videos: List<Video>,
    val providers: WatchProviders,
) {
    /**
     * The trailer to play when the user presses the trailer button.
     *
     * TMDB very often has no `fr-FR` videos at all, so the caller merges a
     * French and an English fetch and this picks the best of the lot: French
     * first, official before fan uploads, full trailers before teasers.
     */
    val bestTrailer: Video? get() = videos.minWithOrNull(Video.PREFERENCE)

    val otherVideos: List<Video>
        get() = videos.sortedWith(Video.PREFERENCE).drop(1)
}

data class Genre(val id: Int, val name: String)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?,
)

data class Video(
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean,
    val language: String,
) {
    val isYouTube: Boolean get() = site.equals("YouTube", ignoreCase = true)

    companion object {
        private val TYPE_RANK = listOf("Trailer", "Teaser", "Clip", "Featurette")

        /** Best first. */
        val PREFERENCE: Comparator<Video> =
            compareBy<Video> { if (it.language.startsWith("fr", ignoreCase = true)) 0 else 1 }
                .thenBy { if (it.official) 0 else 1 }
                .thenBy { TYPE_RANK.indexOf(it.type).takeIf { i -> i >= 0 } ?: TYPE_RANK.size }
                .thenBy { it.name }
    }
}
