package com.orphybel.bacine.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orphybel.bacine.domain.Provider
import com.orphybel.bacine.ui.player.findActivity
import java.net.URLEncoder

/** What happened when the user picked a provider, so the UI can say so. */
enum class ProviderOpenResult { APP, WEB, NONE }

/**
 * Opens a streaming provider on a given film — as directly as the app allows.
 *
 * Device testing set the ceiling here. TMDB (via JustWatch) gives a provider's
 * name and logo but not that provider's own id for the film, so there is no
 * exact "play this title in Netflix" deep link to build. And the TV apps vary:
 * Disney+ and Max open their search screen from a search URL but do not carry
 * the query; Netflix and Prime Video ignore the web URL entirely. So "jump
 * straight to the film" is not reliably possible — what is, is opening the
 * right app.
 *
 * The attempt order, per provider whose app is installed:
 *  1. the title-search URL aimed at the app — for the apps that honour it, this
 *     lands on their search (sometimes pre-filled, sometimes not);
 *  2. the app's own launcher — for apps that ignore the URL (Netflix, Prime),
 *     so the user still lands in the right app rather than a stranded browser
 *     tab.
 *
 * Only when the app is not installed do we fall back to the aggregate "where to
 * watch" page, which opens in a browser.
 *
 * Declaring the provider packages in the manifest `<queries>` is what makes the
 * targeting and the install check work under Android 11+ package visibility;
 * without it the system silently fell through to a browser chooser.
 */
fun openProvider(
    context: Context,
    provider: Provider,
    movieTitle: String,
    justWatchLink: String?,
): ProviderOpenResult {
    val query = URLEncoder.encode(movieTitle, "UTF-8")
    val target = PROVIDER_TARGETS[provider.id]

    if (target != null) {
        val url = Uri.parse(target.url(query))

        // 1) Known packages: try the search URL, then the app's own launcher.
        for (pkg in target.packages) {
            val launcher = context.launcherIntentFor(pkg) ?: continue // not installed
            if (context.launch(Intent(Intent.ACTION_VIEW, url).setPackage(pkg))) {
                return ProviderOpenResult.APP
            }
            if (context.launch(launcher)) return ProviderOpenResult.APP
        }

        // 2) Package guesses missed the installed app — ask the system which
        // installed, non-browser app claims this provider's domain, and open
        // that. This is what catches apps whose package name differs on this
        // device (a regional build, or HBO Max vs Max) without hard-coding it.
        val handler = context.nonBrowserHandlerOf(url)
        if (handler != null) {
            if (context.launch(Intent(Intent.ACTION_VIEW, url).setPackage(handler))) {
                return ProviderOpenResult.APP
            }
            val launcher = context.launcherIntentFor(handler)
            if (launcher != null && context.launch(launcher)) return ProviderOpenResult.APP
        }
    }

    if (!justWatchLink.isNullOrBlank()) {
        if (context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(justWatchLink)))) {
            return ProviderOpenResult.WEB
        }
    }
    return ProviderOpenResult.NONE
}

/** The app's launcher intent, or null when the package is not installed. */
private fun Context.launcherIntentFor(pkg: String): Intent? =
    runCatching { packageManager.getLaunchIntentForPackage(pkg) }.getOrNull()

/**
 * The package of an installed app — not a browser — that handles [uri], or null.
 *
 * Providers register their web domains, so this finds the streaming app without
 * knowing its package name. Browsers are excluded by probing a throwaway URL:
 * anything that also handles that is a general browser, not the provider.
 */
private fun Context.nonBrowserHandlerOf(uri: Uri): String? {
    val browsers = handlersOf(Uri.parse("https://example.com"))
    return handlersOf(uri).firstOrNull { it !in browsers }
}

@Suppress("DEPRECATION", "QueryPermissionsNeeded")
private fun Context.handlersOf(uri: Uri): Set<String> =
    packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), 0)
        .mapTo(mutableSetOf()) { it.activityInfo.packageName }

/**
 * Starts [intent] if something can handle it.
 *
 * No NEW_TASK when we are inside an Activity: like the YouTube handoff, that
 * keeps the provider on top of our task so Back returns to BaCine instead of
 * the provider's home.
 */
private fun Context.launch(intent: Intent): Boolean {
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(packageManager) == null) return false
    return runCatching { startActivity(intent) }.isSuccess
}

/** How to reach one provider's app: the packages to try, and its search URL. */
private class ProviderTarget(
    val packages: List<String>,
    val url: (query: String) -> String,
)

private val primeVideo = ProviderTarget(
    listOf(
        "com.amazon.amazonvideo.livingroom",
        "com.amazon.avod.thirdpartyclient",
        "com.amazon.avod",
    ),
) { "https://www.primevideo.com/search/ref=atv_nb_sug?phrase=$it" }

// Max, and the HBO Max app it replaced (still installed under the old package
// on many devices).
private val max = ProviderTarget(
    listOf("com.wbd.stream", "com.hbo.hbonow"),
) { "https://play.max.com/search?q=$it" }

private val appleTv = ProviderTarget(
    listOf("com.apple.atve.androidtv.appletv", "com.apple.atve.amazon.appletv"),
) { "https://tv.apple.com/search?term=$it" }

private val paramount = ProviderTarget(
    listOf("com.cbs.ott", "com.paramount.pplus.intl"),
) { "https://www.paramountplus.com/search/?query=$it" }

/**
 * TMDB watch-provider ids mapped to how to open their app on a title search.
 *
 * Only providers with a reasonably reliable search URL are listed; everything
 * else falls through to the aggregate "where to watch" link, which is always a
 * safe destination. Ids come from TMDB's provider list.
 */
private val PROVIDER_TARGETS: Map<Int, ProviderTarget> = mapOf(
    8 to ProviderTarget(listOf("com.netflix.ninja")) { "https://www.netflix.com/search?q=$it" },
    // Prime Video subscription, plus the "Amazon Video" rent/buy storefronts.
    9 to primeVideo,
    10 to primeVideo,
    119 to primeVideo,
    337 to ProviderTarget(listOf("com.disney.disneyplus")) { "https://www.disneyplus.com/search?q=$it" },
    // Max, and the HBO Max id it replaced.
    1899 to max,
    384 to max,
    // Apple TV store and Apple TV+.
    2 to appleTv,
    350 to appleTv,
    531 to paramount,
    283 to ProviderTarget(listOf("com.crunchyroll.crunchyroid")) { "https://www.crunchyroll.com/search?q=$it" },
)
