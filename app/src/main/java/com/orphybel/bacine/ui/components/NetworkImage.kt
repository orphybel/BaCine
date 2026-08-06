package com.orphybel.bacine.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.orphybel.bacine.ui.LocalAppContainer
import com.orphybel.bacine.ui.theme.Surface2

/**
 * Loads and draws a remote image, fading it in once decoded.
 *
 * A missing or failed image is not an error state: the placeholder simply
 * stays, so a poster TMDB happens not to have never interrupts browsing.
 *
 * @param maxWidthPx downsample target; leave at 0 for posters, set it for
 *   full-bleed backdrops so a 1280px JPEG does not sit in memory at full size.
 */
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxWidthPx: Int = 0,
    placeholder: Color = Surface2,
    fallback: (@Composable () -> Unit)? = null,
) {
    val loader = LocalAppContainer.current.imageLoader
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url, maxWidthPx) {
        bitmap = null
        failed = false
        if (url.isNullOrBlank()) {
            failed = true
            return@LaunchedEffect
        }
        val loaded = loader.load(url, maxWidthPx)
        if (loaded == null) failed = true else bitmap = loaded
    }

    val alpha by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "imageFade",
    )

    Box(modifier = modifier.background(placeholder), contentAlignment = Alignment.Center) {
        bitmap?.let { loaded ->
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize().alpha(alpha),
                contentScale = contentScale,
            )
        }
        if (failed && fallback != null) {
            fallback()
        }
    }
}
