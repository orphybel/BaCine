package com.orphybel.bacine

import android.content.Context
import com.orphybel.bacine.data.image.ImageLoader
import com.orphybel.bacine.data.prefs.SettingsStore
import com.orphybel.bacine.data.remote.TmdbClient
import com.orphybel.bacine.data.repo.MovieRepository
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled dependency graph.
 *
 * The app has exactly one repository and one HTTP client, so a DI framework
 * would cost an annotation processor and a version pairing for no benefit.
 * Everything is lazy: nothing touches disk or the network until first use.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // A projector is often on a mediocre Wi-Fi link; be patient but not
            // so patient that a dead network leaves the UI spinning forever.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // TMDB serves artwork with long-lived cache headers, so an HTTP
            // cache means posters survive a restart without any extra work.
            .cache(Cache(File(appContext.cacheDir, "http"), HTTP_CACHE_BYTES))
            .build()
    }

    val imageLoader: ImageLoader by lazy { ImageLoader(httpClient) }

    val tmdbClient: TmdbClient by lazy {
        // Named rather than a trailing lambda: TmdbClient takes a baseUrl after
        // the key provider, and trailing-lambda syntax would bind to that.
        TmdbClient(
            http = httpClient,
            // Reads the key per request so a key entered in settings applies at once.
            apiKeyProvider = { settings.currentApiKey() },
        )
    }

    val repository: MovieRepository by lazy { MovieRepository(tmdbClient, settings) }

    private companion object {
        const val HTTP_CACHE_BYTES = 96L * 1024 * 1024
    }
}
