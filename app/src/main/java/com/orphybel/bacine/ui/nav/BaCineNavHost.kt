package com.orphybel.bacine.ui.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orphybel.bacine.ui.catalog.CatalogScreen
import com.orphybel.bacine.ui.detail.DetailScreen
import com.orphybel.bacine.ui.home.HomeScreen
import com.orphybel.bacine.ui.player.TrailerScreen
import com.orphybel.bacine.ui.search.SearchScreen
import com.orphybel.bacine.ui.settings.OnboardingScreen
import com.orphybel.bacine.ui.settings.SettingsScreen

@Composable
fun BaCineNavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The rail is for browsing. A detail page, the player and onboarding are
    // full-bleed, and showing a rail there would only steal focus stops.
    val showRail = currentRoute in Routes.TOP_LEVEL

    Row(modifier = modifier.fillMaxSize()) {
        if (showRail) {
            NavRail(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        // Switching sections should not pile up a back stack:
                        // one press of Back from any section leaves the app.
                        popUpTo(Routes.HOME) { inclusive = destination.route == Routes.HOME }
                        launchSingleTop = true
                    }
                },
            )
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenMovie = { navController.navigate(Routes.detail(it.id)) },
                    onPlayTrailer = { navController.navigate(Routes.trailer(it.id)) },
                    onMissingKey = {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.CATALOG) {
                CatalogScreen(
                    onOpenMovie = { navController.navigate(Routes.detail(it.id)) },
                    onMissingKey = { navController.navigate(Routes.ONBOARDING) },
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onOpenMovie = { navController.navigate(Routes.detail(it.id)) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.MOVIE_ID_ARG) { type = NavType.IntType }),
            ) { entry ->
                val movieId = entry.arguments?.getInt(Routes.MOVIE_ID_ARG) ?: return@composable
                DetailScreen(
                    movieId = movieId,
                    onPlayTrailer = { navController.navigate(Routes.trailer(movieId)) },
                    onPlayVideo = { key -> navController.navigate(Routes.trailer(movieId, key)) },
                )
            }

            composable(
                route = Routes.TRAILER,
                arguments = listOf(
                    navArgument(Routes.MOVIE_ID_ARG) { type = NavType.IntType },
                    navArgument(Routes.VIDEO_KEY_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val movieId = entry.arguments?.getInt(Routes.MOVIE_ID_ARG) ?: return@composable
                TrailerScreen(
                    movieId = movieId,
                    videoKey = entry.arguments?.getString(Routes.VIDEO_KEY_ARG),
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}
