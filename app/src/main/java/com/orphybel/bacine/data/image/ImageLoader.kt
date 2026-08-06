package com.orphybel.bacine.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Minimal bitmap loader for TMDB artwork.
 *
 * Deliberately hand-rolled instead of pulling in an image library: the app
 * loads exactly one kind of thing (immutable, CDN-hosted, already correctly
 * sized JPEGs) over an OkHttp client that is already configured with a disk
 * cache. That makes bytes-on-disk caching free, and leaves only bitmap decoding
 * and an in-memory cache to handle here.
 */
class ImageLoader(private val client: OkHttpClient) {

    private val memory = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    /**
     * @param maxWidthPx downsamples large artwork (backdrops) to roughly this
     *   width. Zero decodes at full size, which is right for posters.
     * @return the bitmap, or null when the fetch or decode failed. Callers show
     *   a placeholder rather than an error: a missing poster is not a failure
     *   worth interrupting the user for.
     */
    suspend fun load(url: String, maxWidthPx: Int = 0): Bitmap? {
        memory[url]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val bytes = client.newCall(Request.Builder().url(url).get().build())
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) return@withContext null
                        response.body?.bytes()
                    } ?: return@withContext null

                val bitmap = decode(bytes, maxWidthPx) ?: return@withContext null
                memory.put(url, bitmap)
                bitmap
            } catch (e: IOException) {
                ensureActive() // Cancellation arrives as an IOException here.
                null
            } catch (e: OutOfMemoryError) {
                memory.evictAll()
                null
            }
        }
    }

    private fun decode(bytes: ByteArray, maxWidthPx: Int): Bitmap? {
        if (maxWidthPx <= 0) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidthPx) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    fun clear() = memory.evictAll()

    private companion object {
        /**
         * A sixth of the heap. Posters are small, but a screen full of them
         * plus a 1280px backdrop adds up fast on a low-memory TV device.
         */
        fun cacheSizeKb(): Int =
            (Runtime.getRuntime().maxMemory() / 1024L / 6L).coerceIn(8_192L, 96_000L).toInt()
    }
}
