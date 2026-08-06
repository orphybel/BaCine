package com.orphybel.bacine.ui.nav

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.orphybel.bacine.R
import com.orphybel.bacine.ui.components.BaCineIcons

/** The top-level sections, i.e. everything the navigation rail offers. */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, BaCineIcons.Home),
    CATALOG(Routes.CATALOG, R.string.nav_catalog, BaCineIcons.Grid),
    SEARCH(Routes.SEARCH, R.string.nav_search, BaCineIcons.Search),
    SETTINGS(Routes.SETTINGS, R.string.nav_settings, BaCineIcons.Settings),
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val MOVIE_ID_ARG = "movieId"
    const val VIDEO_KEY_ARG = "videoKey"
    const val DETAIL = "detail/{$MOVIE_ID_ARG}"
    const val TRAILER = "trailer/{$MOVIE_ID_ARG}?$VIDEO_KEY_ARG={$VIDEO_KEY_ARG}"

    fun detail(movieId: Int) = "detail/$movieId"

    /**
     * @param videoKey a specific video to play. Omitted, the player picks the
     *   best trailer itself — which is what the home banner needs, since it has
     *   never loaded the movie's video list.
     */
    fun trailer(movieId: Int, videoKey: String? = null): String =
        if (videoKey.isNullOrBlank()) "trailer/$movieId" else "trailer/$movieId?$VIDEO_KEY_ARG=$videoKey"

    /** Routes that keep the navigation rail on screen. */
    val TOP_LEVEL = setOf(HOME, CATALOG, SEARCH, SETTINGS)
}
