package com.orphybel.bacine.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri

/** YouTube for Android TV — what a Google TV device such as the MoGo 4 ships. */
private const val YOUTUBE_TV_PACKAGE = "com.google.android.youtube.tv"

/** YouTube for phones and tablets, in case the app was sideloaded. */
private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

/**
 * Hands a video off to the YouTube app, or to whatever can open a YouTube URL.
 *
 * This is the escape hatch for the embedded player. The IFrame player runs
 * inside a WebView, and WebView behaviour on TV firmware is the least
 * predictable part of this app — some videos are also flagged as
 * non-embeddable by their owner and will never play in-app. Either way the
 * user still gets to watch the trailer.
 *
 * Order matters, and not for the reason one would expect. The TV YouTube app
 * declares an intent filter for the `vnd.youtube:` scheme, so firing that first
 * *succeeds* — and then drops the video id and opens its home screen, which
 * looks exactly like a working handoff from here. The `https` watch URL is the
 * form it actually honours, so that one goes first, aimed straight at the TV
 * package.
 *
 * @return false when nothing on the device can handle it, so the caller can say
 *   so instead of silently doing nothing.
 */
fun openInYouTube(context: Context, videoKey: String): Boolean {
    if (videoKey.isBlank()) return false

    val watchUrl = Uri.parse("https://www.youtube.com/watch?v=$videoKey")
    val candidates = listOf(
        Intent(Intent.ACTION_VIEW, watchUrl).setPackage(YOUTUBE_TV_PACKAGE),
        Intent(Intent.ACTION_VIEW, watchUrl).setPackage(YOUTUBE_PACKAGE),
        // Whatever the system picks — another YouTube build, or a browser.
        Intent(Intent.ACTION_VIEW, watchUrl),
        // Last resort: the legacy scheme, which at least opens the app.
        Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://$videoKey")),
    )

    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // resolveActivity rather than catching ActivityNotFoundException: the
        // targeted intents are the ones most likely to have no handler, and
        // asking first keeps the fallthrough cheap.
        if (intent.resolveActivity(context.packageManager) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return true
    }
    return false
}
