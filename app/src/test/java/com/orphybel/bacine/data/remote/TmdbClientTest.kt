package com.orphybel.bacine.data.remote

import com.orphybel.bacine.domain.Availability
import com.orphybel.bacine.domain.CatalogQuery
import com.orphybel.bacine.domain.SortOrder
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the query TMDB actually receives. These parameters are the whole
 * behaviour of the filters and of the VOD row, and a silent typo in one of them
 * would show up as "the filter does nothing" rather than as an error.
 */
class TmdbClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: TmdbClient

    private var apiKey: String = "test-key"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = TmdbClient(
            http = OkHttpClient(),
            apiKeyProvider = { apiKey },
            baseUrl = server.url("/3").toString().trimEnd('/'),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueEmptyPage() {
        server.enqueue(
            MockResponse().setBody("""{"page":1,"results":[],"total_pages":1,"total_results":0}""")
        )
    }

    private fun takeUrl(): HttpUrl = server.takeRequest().requestUrl!!

    @Test
    fun `every request carries the key and the locale`() = runTest {
        enqueueEmptyPage()

        client.nowPlaying(page = 2, locale = TmdbLocale.FRANCE)

        val url = takeUrl()
        assertTrue(url.encodedPath.endsWith("/movie/now_playing"))
        assertEquals("test-key", url.queryParameter("api_key"))
        assertEquals("fr-FR", url.queryParameter("language"))
        assertEquals("FR", url.queryParameter("region"))
        assertEquals("2", url.queryParameter("page"))
    }

    @Test
    fun `the VOD row asks for paid monetization in the watch region`() = runTest {
        enqueueEmptyPage()

        client.recentVod(page = 1, locale = TmdbLocale.FRANCE)

        val url = takeUrl()
        assertEquals("FR", url.queryParameter("watch_region"))
        assertEquals("rent|buy", url.queryParameter("with_watch_monetization_types"))
        assertEquals("primary_release_date.desc", url.queryParameter("sort_by"))
        // The window must be bounded at both ends: no future releases, and not
        // the entire history of cinema either.
        assertTrue(url.queryParameter("primary_release_date.gte")!!.isNotBlank())
        assertTrue(url.queryParameter("primary_release_date.lte")!!.isNotBlank())
        assertTrue(
            url.queryParameter("primary_release_date.gte")!! <
                url.queryParameter("primary_release_date.lte")!!
        )
    }

    @Test
    fun `default filters send no narrowing parameters`() = runTest {
        enqueueEmptyPage()

        client.discover(CatalogQuery.DEFAULT, page = 1, locale = TmdbLocale.FRANCE)

        val url = takeUrl()
        assertEquals("popularity.desc", url.queryParameter("sort_by"))
        assertNull(url.queryParameter("with_genres"))
        assertNull(url.queryParameter("primary_release_year"))
        assertNull(url.queryParameter("vote_average.gte"))
        assertNull(url.queryParameter("watch_region"))
    }

    @Test
    fun `multiple genres are combined with OR`() = runTest {
        enqueueEmptyPage()

        client.discover(
            CatalogQuery(genreIds = setOf(35, 28)),
            page = 1,
            locale = TmdbLocale.FRANCE,
        )

        // Sorted so the request is deterministic, pipe-separated so the two
        // genres widen the result set instead of intersecting it.
        assertEquals("28|35", takeUrl().queryParameter("with_genres"))
    }

    @Test
    fun `sorting by rating adds a vote floor`() = runTest {
        enqueueEmptyPage()

        client.discover(
            CatalogQuery(sort = SortOrder.RATING),
            page = 1,
            locale = TmdbLocale.FRANCE,
        )

        val url = takeUrl()
        assertEquals("vote_average.desc", url.queryParameter("sort_by"))
        // Without this, the top of the list is films with a single 10/10 vote.
        assertEquals("200", url.queryParameter("vote_count.gte"))
    }

    @Test
    fun `sorting by popularity adds no vote floor`() = runTest {
        enqueueEmptyPage()

        client.discover(
            CatalogQuery(sort = SortOrder.POPULARITY),
            page = 1,
            locale = TmdbLocale.FRANCE,
        )

        assertNull(takeUrl().queryParameter("vote_count.gte"))
    }

    @Test
    fun `the rent filter maps to the TMDB monetization type`() = runTest {
        enqueueEmptyPage()

        client.discover(
            CatalogQuery(availability = Availability.RENT),
            page = 1,
            locale = TmdbLocale.FRANCE,
        )

        val url = takeUrl()
        assertEquals("rent", url.queryParameter("with_watch_monetization_types"))
        // TMDB rejects the monetization filter unless a watch region comes with it.
        assertEquals("FR", url.queryParameter("watch_region"))
    }

    @Test
    fun `the in-theatres filter uses a recent theatrical release window`() = runTest {
        enqueueEmptyPage()

        client.discover(
            CatalogQuery(availability = Availability.THEATERS),
            page = 1,
            locale = TmdbLocale.FRANCE,
        )

        val url = takeUrl()
        assertEquals("2|3", url.queryParameter("with_release_type"))
        assertNull(url.queryParameter("with_watch_monetization_types"))
        assertTrue(url.queryParameter("primary_release_date.gte")!!.isNotBlank())
    }

    @Test
    fun `year and minimum rating are forwarded`() = runTest {
        enqueueEmptyPage()

        client.discover(
            CatalogQuery(year = 2024, minRating = 7),
            page = 1,
            locale = TmdbLocale.FRANCE,
        )

        val url = takeUrl()
        assertEquals("2024", url.queryParameter("primary_release_year"))
        assertEquals("7", url.queryParameter("vote_average.gte"))
    }

    @Test
    fun `a missing key fails before any request is sent`() = runTest {
        apiKey = "   "

        assertFailsWith<TmdbException.MissingKey> {
            client.nowPlaying(page = 1, locale = TmdbLocale.FRANCE)
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a rejected key is reported as invalid, not as a generic failure`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody(
                """{"success":false,"status_code":7,"status_message":"Invalid API key"}"""
            )
        )

        val failure = assertFailsWith<TmdbException.InvalidKey> {
            client.nowPlaying(page = 1, locale = TmdbLocale.FRANCE)
        }
        assertEquals("Invalid API key", failure.message)
    }

    @Test
    fun `key validation reports false rather than throwing on a bad key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"status_message":"nope"}"""))

        assertEquals(false, client.validateKey("wrong"))
    }

    @Test
    fun `key validation succeeds on a 200`() = runTest {
        server.enqueue(MockResponse().setBody("""{"images":{}}"""))

        assertEquals(true, client.validateKey("right"))
    }

    @Test
    fun `a malformed body surfaces as a parse error`() = runTest {
        server.enqueue(MockResponse().setBody("not json at all"))

        assertFailsWith<TmdbException.Parse> {
            client.nowPlaying(page = 1, locale = TmdbLocale.FRANCE)
        }
    }

    @Test
    fun `details request the appended sub-resources in one round trip`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":42,"title":"Film"}"""))

        client.details(42, TmdbLocale.FRANCE)

        val url = takeUrl()
        assertTrue(url.encodedPath.endsWith("/movie/42"))
        assertEquals("videos,credits,watch/providers", url.queryParameter("append_to_response"))
    }

    @Test
    fun `watch providers are read out of the country map`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"id":42,"results":{"FR":{"rent":[{"provider_id":2,"provider_name":"Apple TV"}]}}}"""
            )
        )

        val providers = client.watchProviders(42, "FR")

        assertEquals(listOf("Apple TV"), providers?.rent?.map { it.providerName })
    }
}
