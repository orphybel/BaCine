package com.orphybel.bacine.data.repo

import com.orphybel.bacine.data.prefs.SettingsStore
import com.orphybel.bacine.data.remote.TmdbClient
import com.orphybel.bacine.data.remote.TmdbLocale
import com.orphybel.bacine.data.remote.dto.MovieDto
import com.orphybel.bacine.data.remote.dto.PageDto
import com.orphybel.bacine.data.remote.dto.toDomain
import com.orphybel.bacine.domain.CatalogQuery
import com.orphybel.bacine.domain.Genre
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.domain.MovieDetails
import com.orphybel.bacine.domain.WatchProviders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/** One page of results, plus enough context to know whether more exist. */
data class MoviePage(
    val items: List<Movie>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page < totalPages

    companion object {
        val EMPTY = MoviePage(emptyList(), 1, 1)
    }
}

/**
 * Everything the UI knows about movies.
 *
 * Holds small in-memory caches (genres, per-movie providers and details) that
 * live for the process. TMDB data changes daily at most, and a projector
 * session is short, so nothing here is persisted to disk.
 */
class MovieRepository(
    private val client: TmdbClient,
    private val settings: SettingsStore,
) {
    private val providerCache = ConcurrentHashMap<Int, WatchProviders>()
    private val detailsCache = ConcurrentHashMap<Int, MovieDetails>()

    @Volatile
    private var genreCache: List<Genre>? = null

    private suspend fun locale(): TmdbLocale = settings.locale.first()

    suspend fun nowPlaying(page: Int): MoviePage =
        client.nowPlaying(page, locale()).toPage()

    suspend fun popular(page: Int): MoviePage =
        client.popular(page, locale()).toPage()

    suspend fun discover(query: CatalogQuery, page: Int): MoviePage =
        client.discover(query, page, locale()).toPage()

    suspend fun search(term: String, page: Int): MoviePage {
        if (term.isBlank()) return MoviePage.EMPTY
        return client.search(term.trim(), page, locale()).toPage()
    }

    /**
     * Recent releases genuinely available to rent or buy.
     *
     * TMDB's `with_watch_monetization_types` filter is documented as unreliable
     * and lets through films with no paid offer at all, so every candidate is
     * re-checked against `/movie/{id}/watch/providers` — the cheap endpoint,
     * not full details — before it reaches the row. Checks run concurrently
     * with a small permit pool so a page costs about one round trip, not twenty.
     */
    suspend fun recentVod(page: Int): MoviePage {
        val loc = locale()
        val raw = client.recentVod(page, loc).toPage()
        val verified = coroutineScope {
            val gate = Semaphore(MAX_PARALLEL_CHECKS)
            raw.items
                .map { movie ->
                    async {
                        val providers = gate.withPermit { providersFor(movie.id, loc.region) }
                        movie.takeIf { providers.hasPaidVod }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        return raw.copy(items = verified)
    }

    /** Cached per movie: the VOD row asks for these in bulk. */
    suspend fun providersFor(movieId: Int, region: String? = null): WatchProviders {
        providerCache[movieId]?.let { return it }
        val resolved = region ?: locale().region
        val providers = client.watchProviders(movieId, resolved)?.toDomain() ?: WatchProviders.EMPTY
        providerCache[movieId] = providers
        return providers
    }

    /**
     * @param forceRefresh skips the cache, used by the detail screen's retry.
     */
    suspend fun details(movieId: Int, forceRefresh: Boolean = false): MovieDetails {
        if (!forceRefresh) detailsCache[movieId]?.let { return it }

        val loc = locale()
        val dto = client.details(movieId, loc)

        // A `fr-FR` request usually returns no videos at all; fall back to the
        // original language rather than telling the user there is no trailer.
        val extraVideos = if (dto.videos?.results.isNullOrEmpty()) {
            runCatching { client.videos(movieId, FALLBACK_VIDEO_LANGUAGE) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val details = dto.toDomain(region = loc.region, extraVideos = extraVideos)
        detailsCache.trimIfLarge()
        detailsCache[movieId] = details
        providerCache[movieId] = details.providers
        return details
    }

    suspend fun genres(): List<Genre> {
        genreCache?.let { return it }
        val loaded = client.genres(locale()).genres.map { it.toDomain() }
        genreCache = loaded
        return loaded
    }

    private fun PageDto<MovieDto>.toPage(): MoviePage = MoviePage(
        // TMDB occasionally repeats an entry across pages; a duplicate key
        // would crash a LazyRow, so de-duplicate defensively.
        items = results.distinctBy { it.id }.map { it.toDomain() },
        page = page,
        totalPages = totalPages,
    )

    private fun <K, V> ConcurrentHashMap<K, V>.trimIfLarge() {
        if (size >= DETAILS_CACHE_LIMIT) clear()
    }

    private companion object {
        const val MAX_PARALLEL_CHECKS = 6
        const val DETAILS_CACHE_LIMIT = 60
        const val FALLBACK_VIDEO_LANGUAGE = "en-US"
    }
}
