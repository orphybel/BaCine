package com.orphybel.bacine.data.remote

import com.orphybel.bacine.data.remote.dto.CountryProvidersDto
import com.orphybel.bacine.data.remote.dto.GenreListDto
import com.orphybel.bacine.data.remote.dto.MovieDetailsDto
import com.orphybel.bacine.data.remote.dto.MovieDto
import com.orphybel.bacine.data.remote.dto.PageDto
import com.orphybel.bacine.data.remote.dto.TmdbErrorDto
import com.orphybel.bacine.data.remote.dto.VideoDto
import com.orphybel.bacine.data.remote.dto.VideoListDto
import com.orphybel.bacine.data.remote.dto.WatchProvidersResponseDto
import com.orphybel.bacine.domain.Availability
import com.orphybel.bacine.domain.CatalogQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Thin TMDB v3 client.
 *
 * Deliberately plain OkHttp rather than Retrofit: there are only a handful of
 * GET endpoints, and every one of them needs the same api_key/language
 * treatment, which is cheaper to express here than through an interface plus a
 * converter.
 *
 * @param apiKeyProvider resolves the key on each call, so a key entered in
 *   settings takes effect without rebuilding the client.
 */
class TmdbClient(
    private val http: OkHttpClient,
    private val apiKeyProvider: suspend () -> String,
    /** Overridden by tests so requests can be pointed at a local server. */
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    // ---------------------------------------------------------------- lists

    suspend fun nowPlaying(page: Int, locale: TmdbLocale): PageDto<MovieDto> =
        getPage("movie/now_playing", locale) {
            addQueryParameter("page", page.toString())
            addQueryParameter("region", locale.region)
        }

    suspend fun popular(page: Int, locale: TmdbLocale): PageDto<MovieDto> =
        getPage("movie/popular", locale) {
            addQueryParameter("page", page.toString())
            addQueryParameter("region", locale.region)
        }

    /**
     * Recent releases that can be rented or bought in [locale]'s region.
     *
     * TMDB's `with_watch_monetization_types` filter is known to be loose, so the
     * repository re-checks each result against `/watch/providers` before showing
     * it. See [MovieRepository.recentVod].
     */
    suspend fun recentVod(page: Int, locale: TmdbLocale, monthsBack: Long = 9): PageDto<MovieDto> {
        val today = LocalDate.now()
        return getPage("discover/movie", locale) {
            addQueryParameter("page", page.toString())
            addQueryParameter("region", locale.region)
            addQueryParameter("watch_region", locale.region)
            addQueryParameter("with_watch_monetization_types", "rent|buy")
            addQueryParameter("sort_by", "primary_release_date.desc")
            addQueryParameter("primary_release_date.gte", today.minusMonths(monthsBack).format(ISO))
            addQueryParameter("primary_release_date.lte", today.format(ISO))
            addQueryParameter("vote_count.gte", "20")
            addQueryParameter("include_adult", "false")
        }
    }

    suspend fun discover(query: CatalogQuery, page: Int, locale: TmdbLocale): PageDto<MovieDto> =
        getPage("discover/movie", locale) {
            addQueryParameter("page", page.toString())
            addQueryParameter("region", locale.region)
            addQueryParameter("include_adult", "false")
            addQueryParameter("sort_by", query.sort.tmdbValue)
            applyCatalogFilters(query, locale)
        }

    suspend fun search(term: String, page: Int, locale: TmdbLocale): PageDto<MovieDto> =
        getPage("search/movie", locale) {
            addQueryParameter("query", term)
            addQueryParameter("page", page.toString())
            addQueryParameter("region", locale.region)
            addQueryParameter("include_adult", "false")
        }

    // -------------------------------------------------------------- details

    suspend fun details(id: Int, locale: TmdbLocale): MovieDetailsDto {
        val body = get("movie/$id", locale) {
            addQueryParameter("append_to_response", "videos,credits,watch/providers")
        }
        return decode(body) { json.decodeFromString<MovieDetailsDto>(it) }
    }

    /** Fallback pass for trailers, since `fr-FR` videos are often absent. */
    suspend fun videos(id: Int, language: String): List<VideoDto> {
        val body = get("movie/$id/videos", TmdbLocale(language = language, region = "FR"))
        return decode(body) { json.decodeFromString<VideoListDto>(it) }.results
    }

    suspend fun watchProviders(id: Int, region: String): CountryProvidersDto? {
        val body = get("movie/$id/watch/providers", TmdbLocale(region = region))
        val parsed = decode(body) { json.decodeFromString<WatchProvidersResponseDto>(it) }
        return parsed.results[region.uppercase()]
    }

    suspend fun genres(locale: TmdbLocale): GenreListDto {
        val body = get("genre/movie/list", locale)
        return decode(body) { json.decodeFromString<GenreListDto>(it) }
    }

    /**
     * Cheapest endpoint that still exercises authentication, used to tell the
     * user straight away whether the key they typed is good.
     */
    suspend fun validateKey(candidate: String): Boolean {
        val url = "$baseUrl/configuration".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", candidate)
            .build()
        return try {
            execute(Request.Builder().url(url).build())
            true
        } catch (_: TmdbException.InvalidKey) {
            false
        }
    }

    // --------------------------------------------------------------- plumbing

    private fun HttpUrl.Builder.applyCatalogFilters(query: CatalogQuery, locale: TmdbLocale) {
        if (query.genreIds.isNotEmpty()) {
            // Pipe means OR: picking two genres widens the list rather than
            // narrowing it to films that are both, which is what browsing wants.
            addQueryParameter("with_genres", query.genreIds.sorted().joinToString("|"))
        }
        query.year?.let { addQueryParameter("primary_release_year", it.toString()) }
        if (query.minRating > 0) {
            addQueryParameter("vote_average.gte", query.minRating.toString())
        }
        query.voteCountFloor?.let { addQueryParameter("vote_count.gte", it.toString()) }

        when (query.availability) {
            Availability.ANY -> Unit
            Availability.THEATERS -> {
                // Discover has no "in theatres" flag; a recent theatrical
                // release window is the closest faithful approximation.
                val today = LocalDate.now()
                addQueryParameter("with_release_type", "2|3")
                addQueryParameter("primary_release_date.gte", today.minusMonths(3).format(ISO))
                addQueryParameter("primary_release_date.lte", today.format(ISO))
            }
            else -> query.availability.monetizationType?.let {
                addQueryParameter("watch_region", locale.region)
                addQueryParameter("with_watch_monetization_types", it)
            }
        }
    }

    private suspend fun getPage(
        path: String,
        locale: TmdbLocale,
        block: HttpUrl.Builder.() -> Unit = {},
    ): PageDto<MovieDto> {
        val body = get(path, locale, block)
        return decode(body) { json.decodeFromString<PageDto<MovieDto>>(it) }
    }

    private suspend fun get(
        path: String,
        locale: TmdbLocale,
        block: HttpUrl.Builder.() -> Unit = {},
    ): String {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) throw TmdbException.MissingKey()

        val url = "$baseUrl/$path".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", key)
            .addQueryParameter("language", locale.language)
            .apply(block)
            .build()

        return execute(Request.Builder().url(url).get().build())
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        val call = http.newCall(request)
        try {
            call.execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) return@withContext body

                val error = runCatching { json.decodeFromString<TmdbErrorDto>(body) }.getOrNull()
                val message = error?.statusMessage?.takeIf { it.isNotBlank() }
                    ?: "TMDB a répondu ${response.code}."
                throw when (response.code) {
                    401 -> TmdbException.InvalidKey(message)
                    else -> TmdbException.Http(response.code, message)
                }
            }
        } catch (e: IOException) {
            // A cancelled coroutine surfaces as an IOException from OkHttp;
            // don't dress that up as a network outage.
            ensureActive()
            throw TmdbException.Network(e)
        }
    }

    private inline fun <T> decode(body: String, parse: (String) -> T): T = try {
        parse(body)
    } catch (e: SerializationException) {
        throw TmdbException.Parse(e)
    } catch (e: IllegalArgumentException) {
        throw TmdbException.Parse(e)
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.themoviedb.org/3"
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

/** Language/region pair sent with every request. */
data class TmdbLocale(
    val language: String = "fr-FR",
    val region: String = "FR",
) {
    companion object {
        val FRANCE = TmdbLocale()
    }
}
