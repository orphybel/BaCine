package com.orphybel.bacine.ui.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hands a video off to the YouTube app, or to whatever can open a YouTube URL.
 *
 * This is the escape hatch for the embedded player. The IFrame player runs
 * inside a WebView, and WebView behaviour on TV firmware is the least
 * predictable part of this app — some videos are also flagged as
 * non-embeddable by their owner and will never play in-app. Either way the
 * user still gets to watch the trailer.
 *
 * @return false when nothing on the device can handle it, so the caller can say
 *   so instead of silently doing nothing.
 */
fun openInYouTube(context: Context, videoKey: String): Boolean {
    val candidates = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoKey")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoKey")),
    )
    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the next form.
        }
    }
    return false
}
