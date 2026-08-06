package com.orphybel.bacine.ui.watchlist

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.orphybel.bacine.R
import com.orphybel.bacine.domain.Movie
import com.orphybel.bacine.ui.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * A ready-to-use "toggle this film in the watchlist" action.
 *
 * The long-press gesture is the same everywhere — home rows, the catalog grid,
 * search results — so the behaviour lives here once: flip the film's saved
 * state and confirm with a short toast, since a long press gives no other
 * visible feedback. Screens just drop the returned lambda into
 * `onMovieLongClick`.
 */
@Composable
fun rememberWatchlistToggle(): (Movie) -> Unit {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(container, context, scope) {
        { movie ->
            scope.launch {
                val added = container.settings.toggleWatchlist(movie)
                val message = context.getString(
                    if (added) R.string.watchlist_added else R.string.watchlist_removed,
                    movie.title,
                )
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
