package com.orphybel.bacine.data.remote.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

class MappersTest {

    @Test
    fun `movie without a release date still parses`() {
        // TMDB omits release_date on a fair number of discover results; a
        // missing key must not take down the whole page.
        val dto = json.decodeFromString<MovieDto>(
            """{"id":1,"title":"Sans date","overview":"","vote_average":0.0}"""
        )

        val movie = dto.toDomain()

        assertEquals("", movie.releaseDate)
        assertEquals("", movie.year)
        assertNull(movie.posterPath)
    }

    @Test
    fun `title falls back to the original when the localised one is empty`() {
        val dto = MovieDto(id = 7, title = "", originalTitle = "The Original")

        assertEquals("The Original", dto.toDomain().title)
    }

    @Test
    fun `a rating with too few votes is not treated as meaningful`() {
        val barelyVoted = MovieDto(id = 1, voteAverage = 10.0, voteCount = 2).toDomain()
        val wellVoted = MovieDto(id = 2, voteAverage = 7.4, voteCount = 800).toDomain()

        assertTrue(!barelyVoted.hasMeaningfulRating)
        assertTrue(wellVoted.hasMeaningfulRating)
    }

    @Test
    fun `providers are read for the requested region only`() {
        val response = json.decodeFromString<WatchProvidersResponseDto>(
            """
            {"results":{
              "FR":{"link":"https://jw/fr","rent":[{"provider_id":2,"provider_name":"Apple TV","display_priority":3}]},
              "US":{"link":"https://jw/us","buy":[{"provider_id":10,"provider_name":"Amazon"}]}
            }}
            """.trimIndent()
        )

        val fr = response.toDomain("FR")

        assertEquals(listOf("Apple TV"), fr.rent.map { it.name })
        assertTrue(fr.buy.isEmpty())
        assertTrue(fr.hasPaidVod)
        assertEquals("https://jw/fr", fr.link)
    }

    @Test
    fun `an unknown region yields no providers rather than the wrong country's`() {
        val response = WatchProvidersResponseDto(
            results = mapOf("US" to CountryProvidersDto(buy = listOf(ProviderDto(1, "Amazon"))))
        )

        val providers = response.toDomain("FR")

        assertTrue(providers.isEmpty)
        assertTrue(!providers.hasPaidVod)
    }

    @Test
    fun `providers are ordered by JustWatch display priority`() {
        val country = CountryProvidersDto(
            rent = listOf(
                ProviderDto(3, "Troisième", displayPriority = 30),
                ProviderDto(1, "Premier", displayPriority = 1),
                ProviderDto(2, "Deuxième", displayPriority = 12),
            )
        )

        assertEquals(
            listOf("Premier", "Deuxième", "Troisième"),
            country.toDomain().rent.map { it.name },
        )
    }

    @Test
    fun `flatrate alone is not paid VOD`() {
        val country = CountryProvidersDto(flatrate = listOf(ProviderDto(8, "Netflix")))

        val providers = country.toDomain()

        assertTrue(!providers.hasPaidVod)
        assertTrue(!providers.isEmpty)
    }

    @Test
    fun `details merge the fallback language videos and drop non-YouTube ones`() {
        val dto = MovieDetailsDto(
            id = 42,
            title = "Un film",
            runtime = 118,
            genres = listOf(GenreDto(28, "Action")),
            videos = VideoListDto(
                results = listOf(
                    VideoDto(key = "vimeo1", name = "Ailleurs", site = "Vimeo", type = "Trailer"),
                )
            ),
            credits = CreditsDto(
                cast = listOf(
                    CastDto(2, "Second", "Rôle B", order = 2),
                    CastDto(1, "Premier", "Rôle A", order = 0),
                ),
                crew = listOf(
                    CrewDto(9, "La Réalisatrice", "Director"),
                    CrewDto(10, "Quelqu'un", "Editor"),
                ),
            ),
        )
        val fallback = listOf(
            VideoDto(key = "yt1", name = "Trailer", site = "YouTube", type = "Trailer", official = true),
        )

        val details = dto.toDomain(region = "FR", extraVideos = fallback)

        assertEquals(listOf("yt1"), details.videos.map { it.key })
        assertEquals(118, details.runtimeMinutes)
        assertEquals(listOf("Premier", "Second"), details.cast.map { it.name })
        assertEquals(listOf("La Réalisatrice"), details.directors)
    }

    @Test
    fun `a zero runtime is reported as unknown`() {
        val details = MovieDetailsDto(id = 1, runtime = 0).toDomain(region = "FR")

        assertNull(details.runtimeMinutes)
    }

    @Test
    fun `duplicate video keys across languages are collapsed`() {
        val dto = MovieDetailsDto(
            id = 1,
            videos = VideoListDto(
                results = listOf(VideoDto(key = "same", site = "YouTube", type = "Trailer"))
            ),
        )

        val details = dto.toDomain(
            region = "FR",
            extraVideos = listOf(VideoDto(key = "same", site = "YouTube", type = "Teaser")),
        )

        assertEquals(1, details.videos.size)
    }
}
