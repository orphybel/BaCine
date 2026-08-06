package com.orphybel.bacine.data.remote.dto

import com.orphybel.bacine.domain.CastMember
import com.orphybel.bacine.domain.Genre
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.domain.MovieDetails
import com.orphybel.bacine.domain.Provider
import com.orphybel.bacine.domain.Video
import com.orphybel.bacine.domain.WatchProviders

/** Wire types in, domain types out. Kept in one place so it stays testable. */

fun MovieDto.toDomain(): Movie = Movie(
    id = id,
    title = title.ifBlank { originalTitle },
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate.orEmpty(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    genreIds = genreIds,
)

fun GenreDto.toDomain(): Genre = Genre(id = id, name = name)

fun VideoDto.toDomain(): Video = Video(
    key = key,
    name = name,
    site = site,
    type = type,
    official = official,
    language = language,
)

fun CastDto.toDomain(): CastMember = CastMember(
    id = id,
    name = name,
    character = character,
    profilePath = profilePath,
)

fun ProviderDto.toDomain(): Provider = Provider(
    id = providerId,
    name = providerName,
    logoPath = logoPath,
)

/** Providers are per-country; anything outside [region] is irrelevant here. */
fun WatchProvidersResponseDto.toDomain(region: String): WatchProviders {
    val country = results[region.uppercase()] ?: return WatchProviders.EMPTY
    return country.toDomain()
}

fun CountryProvidersDto.toDomain(): WatchProviders = WatchProviders(
    rent = rent.sortedBy { it.displayPriority }.map { it.toDomain() },
    buy = buy.sortedBy { it.displayPriority }.map { it.toDomain() },
    flatrate = flatrate.sortedBy { it.displayPriority }.map { it.toDomain() },
    link = link,
)

/**
 * @param extraVideos videos from a second language pass, merged in because a
 *   `fr-FR` request alone frequently comes back empty.
 */
fun MovieDetailsDto.toDomain(
    region: String,
    extraVideos: List<VideoDto> = emptyList(),
): MovieDetails {
    val allVideos = ((videos?.results.orEmpty()) + extraVideos)
        .distinctBy { it.key }
        .filter { it.key.isNotBlank() }
        .map { it.toDomain() }
        .filter { it.isYouTube }

    return MovieDetails(
        movie = Movie(
            id = id,
            title = title.ifBlank { originalTitle },
            originalTitle = originalTitle,
            overview = overview,
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate.orEmpty(),
            voteAverage = voteAverage,
            voteCount = voteCount,
            genreIds = genres.map { it.id },
        ),
        runtimeMinutes = runtime?.takeIf { it > 0 },
        genres = genres.map { it.toDomain() },
        tagline = tagline,
        cast = credits?.cast.orEmpty().sortedBy { it.order }.take(12).map { it.toDomain() },
        directors = credits?.crew.orEmpty().filter { it.job == "Director" }.map { it.name },
        videos = allVideos,
        providers = watchProviders?.toDomain(region) ?: WatchProviders.EMPTY,
    )
}
