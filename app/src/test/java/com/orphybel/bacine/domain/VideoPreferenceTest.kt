package com.orphybel.bacine.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The trailer picker decides what actually plays when the user presses the
 * button, so its ordering rules are worth pinning down.
 */
class VideoPreferenceTest {

    private fun video(
        key: String,
        type: String = "Trailer",
        official: Boolean = true,
        language: String = "en",
    ) = Video(key = key, name = key, site = "YouTube", type = type, official = official, language = language)

    private fun details(videos: List<Video>) = MovieDetails(
        movie = Movie(
            id = 1,
            title = "Film",
            originalTitle = "Film",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = "2026-01-01",
            voteAverage = 7.0,
            voteCount = 500,
            genreIds = emptyList(),
        ),
        runtimeMinutes = 100,
        genres = emptyList(),
        tagline = "",
        cast = emptyList(),
        directors = emptyList(),
        videos = videos,
        providers = WatchProviders.EMPTY,
    )

    @Test
    fun `a French trailer wins over an English one`() {
        val chosen = details(
            listOf(video("en-trailer", language = "en"), video("fr-trailer", language = "fr"))
        ).bestTrailer

        assertEquals("fr-trailer", chosen?.key)
    }

    @Test
    fun `an official video wins over a fan upload in the same language`() {
        val chosen = details(
            listOf(video("fan", official = false), video("official", official = true))
        ).bestTrailer

        assertEquals("official", chosen?.key)
    }

    @Test
    fun `a full trailer wins over a teaser`() {
        val chosen = details(listOf(video("teaser", type = "Teaser"), video("trailer"))).bestTrailer

        assertEquals("trailer", chosen?.key)
    }

    @Test
    fun `language beats officialness`() {
        // A French fan-uploaded trailer is still more useful to this user than
        // a polished English one.
        val chosen = details(
            listOf(
                video("en-official", official = true, language = "en"),
                video("fr-fan", official = false, language = "fr"),
            )
        ).bestTrailer

        assertEquals("fr-fan", chosen?.key)
    }

    @Test
    fun `an unknown video type sorts after the known ones`() {
        val chosen = details(
            listOf(video("bloopers", type = "Bloopers"), video("clip", type = "Clip"))
        ).bestTrailer

        assertEquals("clip", chosen?.key)
    }

    @Test
    fun `no videos means no trailer`() {
        assertNull(details(emptyList()).bestTrailer)
    }

    @Test
    fun `other videos exclude the one already chosen`() {
        val subject = details(listOf(video("a"), video("b", type = "Teaser"), video("c", type = "Clip")))

        assertEquals("a", subject.bestTrailer?.key)
        assertEquals(listOf("b", "c"), subject.otherVideos.map { it.key })
    }
}
