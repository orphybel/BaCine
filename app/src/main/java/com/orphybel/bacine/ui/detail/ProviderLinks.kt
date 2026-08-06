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
 * Opens a streaming provider on a given film — as directly as the data allows.
 *
 * The honest limit: TMDB (via JustWatch) gives a provider's name and logo but
 * not that provider's own id for the film, so there is no exact "play this
 * title in Netflix" deep link to build. What works is opening the provider's
 * app on a **search for the title**, which lands on the film in almost every
 * case, using a web URL the installed app claims. When the app is absent or
 * refuses the link, we fall back to the aggregate "where to watch" page TMDB
 * did give us.
 *
 * The attempt order per provider:
 *  1. the provider's known package, aimed at its title-search URL — opens the
 *     app itself;
 *  2. the same URL with no package — another matching app, or a browser;
 *  3. the JustWatch/TMDB aggregate link;
 *  4. nothing worked.
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
        for (pkg in target.packages) {
            if (context.launch(Intent(Intent.ACTION_VIEW, url).setPackage(pkg))) {
                return ProviderOpenResult.APP
            }
        }
        // The app was not installed or did not claim that URL; let the system
        // pick — could be another matching app, could be a browser.
        if (context.launch(Intent(Intent.ACTION_VIEW, url))) return ProviderOpenResult.WEB
    }

    if (!justWatchLink.isNullOrBlank()) {
        if (context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(justWatchLink)))) {
            return ProviderOpenResult.WEB
        }
    }
    return ProviderOpenResult.NONE
}

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
    listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
) { "https://www.primevideo.com/search?phrase=$it" }

private val max = ProviderTarget(
    listOf("com.wbd.stream"),
) { "https://play.max.com/search?q=$it" }

private val appleTv = ProviderTarget(
    listOf("com.apple.atve.androidtv.appletv", "com.apple.atve.amazon.appletv"),
) { "https://tv.apple.com/search?term=$it" }

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
    531 to ProviderTarget(listOf("com.cbs.ott")) { "https://www.paramountplus.com/search/?query=$it" },
    283 to ProviderTarget(listOf("com.crunchyroll.crunchyroid")) { "https://www.crunchyroll.com/search?q=$it" },
)
