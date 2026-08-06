package com.orphybel.bacine.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire types for the TMDB v3 API.
 *
 * Every optional field carries a default: TMDB routinely omits `release_date`,
 * `poster_path` and friends, and a missing key must not blow up a whole page.
 */

@Serializable
data class PageDto<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class MovieDto(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class MovieDetailsDto(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val runtime: Int? = null,
    val tagline: String = "",
    val genres: List<GenreDto> = emptyList(),
    val videos: VideoListDto? = null,
    val credits: CreditsDto? = null,
    // TMDB really does return a key with a slash in it.
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponseDto? = null,
)

@Serializable
data class GenreDto(val id: Int, val name: String = "")

@Serializable
data class GenreListDto(val genres: List<GenreDto> = emptyList())

@Serializable
data class VideoListDto(
    val results: List<VideoDto> = emptyList(),
)

@Serializable
data class VideoDto(
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
    @SerialName("iso_639_1") val language: String = "",
)

@Serializable
data class CreditsDto(
    val cast: List<CastDto> = emptyList(),
    val crew: List<CrewDto> = emptyList(),
)

@Serializable
data class CastDto(
    val id: Int,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = Int.MAX_VALUE,
)

@Serializable
data class CrewDto(
    val id: Int,
    val name: String = "",
    val job: String = "",
)

@Serializable
data class WatchProvidersResponseDto(
    val results: Map<String, CountryProvidersDto> = emptyMap(),
)

@Serializable
data class CountryProvidersDto(
    val link: String? = null,
    val rent: List<ProviderDto> = emptyList(),
    val buy: List<ProviderDto> = emptyList(),
    val flatrate: List<ProviderDto> = emptyList(),
)

@Serializable
data class ProviderDto(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = Int.MAX_VALUE,
)

/** TMDB's error envelope, returned with a 4xx status. */
@Serializable
data class TmdbErrorDto(
    val success: Boolean = false,
    @SerialName("status_code") val statusCode: Int = 0,
    @SerialName("status_message") val statusMessage: String = "",
)
