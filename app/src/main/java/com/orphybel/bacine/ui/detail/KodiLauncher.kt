package com.orphybel.bacine.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/** Kodi for Android, from the Play Store or kodi.tv. */
internal const val KODI_PACKAGE = "org.xbmc.kodi"

/** The BaCine bridge add-on that must be installed inside Kodi (see `kodi/`). */
internal const val BRIDGE_ADDON_ID = "plugin.video.bacine"

/** Whether Kodi is installed, so the detail screen only offers it when it can work. */
fun isKodiInstalled(context: Context): Boolean = context.kodiLauncher() != null

/**
 * Asks Kodi to search vStream for [movieTitle].
 *
 * Kodi turns any URL it receives by intent into a *playback* request, and
 * cannot open a folder that way — which is what vStream's search is. So the
 * URL goes to BaCine's own bridge add-on, which cancels that playback and
 * opens vStream's search itself.
 *
 * `plugin://` is not among the schemes Kodi's intent filter declares, so the
 * intent is aimed at Kodi's launcher activity explicitly: an explicit
 * component bypasses filter matching. Kodi queues an intent received during a
 * cold start and replays it once its UI is up, so this works whether or not
 * Kodi is already running.
 *
 * @return false when Kodi is not installed or refused the intent.
 */
fun searchInKodi(context: Context, movieTitle: String): Boolean {
    val intent = context.kodiLauncher() ?: return false
    intent.action = Intent.ACTION_VIEW
    intent.data = Uri.parse(bridgeSearchUrl(movieTitle))
    // Kodi's launcher activity is singleInstance: it gets a task of its own
    // regardless, so Back from Kodi cannot return here anyway.
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}

/** The bridge add-on URL for a title search. Spaces as `%20`: Kodi passes the URL through verbatim. */
internal fun bridgeSearchUrl(movieTitle: String): String {
    val title = URLEncoder.encode(movieTitle.trim(), "UTF-8").replace("+", "%20")
    return "plugin://$BRIDGE_ADDON_ID/?action=search&title=$title"
}

private fun Context.kodiLauncher(): Intent? =
    runCatching { packageManager.getLaunchIntentForPackage(KODI_PACKAGE) }.getOrNull()
