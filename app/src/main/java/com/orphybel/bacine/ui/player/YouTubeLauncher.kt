package com.orphybel.bacine.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

/** YouTube for Android TV — what a Google TV device such as the MoGo 4 ships. */
private const val YOUTUBE_TV_PACKAGE = "com.google.android.youtube.tv"

/** YouTube for phones and tablets, in case the app was sideloaded. */
private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

/**
 * Hands a video off to the YouTube app, or to whatever can open a YouTube URL.
 *
 * This is the escape hatch for the embedded player, and for cinema trailers it
 * is a frequently taken path rather than a rare one: studios routinely disable
 * embedded playback on their official trailers, so the in-app player reports
 * `VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER` and the user ends up here. It is
 * worth getting right.
 *
 * Two things beyond simply opening the video:
 *
 * - **Coming back.** Left to itself, the TV YouTube app opens in its own task
 *   and, when the trailer ends or the user presses Back, shows *its* home
 *   screen — the app they came from is gone. Launching from an Activity
 *   *without* `NEW_TASK` keeps YouTube on top of our task instead, so Back
 *   returns here; and the `finish_on_ended` extra asks YouTube to close itself
 *   when playback ends. Those extras are long-standing but unofficial, so a
 *   build that ignores them simply behaves as before — they never hurt.
 *
 * - **Which intent.** The TV YouTube app declares an intent filter for the
 *   `vnd.youtube:` scheme, so firing that first *succeeds* and then drops the
 *   video id and opens its home screen — a success from our side, a home
 *   screen from the user's. The `https` watch URL is the form it actually
 *   honours, so that goes first, aimed straight at the TV package.
 *
 * @return false when nothing on the device can handle it, so the caller can say
 *   so instead of silently doing nothing.
 */
fun openInYouTube(context: Context, videoKey: String): Boolean {
    if (videoKey.isBlank()) return false

    // Only an Activity context can start an activity without NEW_TASK; from
    // anything else the flag is mandatory, and Back returning here is moot
    // anyway since there is no "here" on our task's stack.
    val fromActivity = context.findActivity() != null
    val watchUrl = Uri.parse("https://www.youtube.com/watch?v=$videoKey")

    fun intentFor(uri: Uri, pkg: String?): Intent = Intent(Intent.ACTION_VIEW, uri).apply {
        if (pkg != null) setPackage(pkg)
        putExtra("finish_on_ended", true)
        putExtra("force_fullscreen", true)
        if (!fromActivity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val candidates = listOf(
        intentFor(watchUrl, YOUTUBE_TV_PACKAGE),
        intentFor(watchUrl, YOUTUBE_PACKAGE),
        // Whatever the system picks — another YouTube build, or a browser.
        intentFor(watchUrl, null),
        // Last resort: the legacy scheme, which at least opens the app.
        intentFor(Uri.parse("vnd.youtube://$videoKey"), null),
    )

    for (intent in candidates) {
        // resolveActivity rather than catching ActivityNotFoundException: the
        // targeted intents are the ones most likely to have no handler, and
        // asking first keeps the fallthrough cheap.
        if (intent.resolveActivity(context.packageManager) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return true
    }
    return false
}

/** Unwraps the Activity a Compose context is hosted in, or null if there is none. */
internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
