package com.orphybel.bacine.data.remote

/**
 * Image URLs for the TMDB CDN.
 *
 * Sizes are chosen for a 1080p projector: posters are drawn around 200 dp wide
 * so `w342` is already generous, while backdrops fill the screen and need
 * `w1280`. Asking for `original` would waste bandwidth on a portable device.
 */
object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p/"

    fun poster(path: String?): String? = path?.let { "${BASE}w342$it" }
    fun backdrop(path: String?): String? = path?.let { "${BASE}w1280$it" }
    fun profile(path: String?): String? = path?.let { "${BASE}w185$it" }
    fun logo(path: String?): String? = path?.let { "${BASE}w92$it" }
}
