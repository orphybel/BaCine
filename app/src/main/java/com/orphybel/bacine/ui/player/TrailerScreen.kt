package com.orphybel.bacine.ui.player

import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orphybel.bacine.R
import com.orphybel.bacine.ui.UiState
import com.orphybel.bacine.ui.components.LoadingState
import com.orphybel.bacine.ui.components.MessageState
import com.orphybel.bacine.ui.components.TvButton
import com.orphybel.bacine.ui.containerViewModel
import com.orphybel.bacine.ui.theme.Gold
import com.orphybel.bacine.ui.theme.Surface2
import com.orphybel.bacine.ui.theme.TextPrimary
import com.orphybel.bacine.ui.theme.TextSecondary
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

@Composable
fun TrailerScreen(
    movieId: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    videoKey: String? = null,
) {
    val viewModel = containerViewModel(key = "trailer_${movieId}_${videoKey.orEmpty()}") {
        TrailerViewModel(it.repository, movieId, videoKey)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(onBack = onExit)

    when (val current = state) {
        is UiState.Loading -> LoadingState(modifier)
        is UiState.Failure -> MessageState(
            message = stringResource(R.string.detail_no_trailer),
            detail = current.error.message,
            onRetry = viewModel::load,
            modifier = modifier,
        )
        is UiState.Success -> TrailerPlayer(
            target = current.data,
            onExit = onExit,
            modifier = modifier,
        )
    }
}

@Composable
private fun TrailerPlayer(
    target: TrailerTarget,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoKey = target.video.key

    var player by remember(videoKey) { mutableStateOf<YouTubePlayer?>(null) }
    var ready by remember(videoKey) { mutableStateOf(false) }
    var failure by remember(videoKey) { mutableStateOf<PlaybackFailure?>(null) }
    var playing by remember(videoKey) { mutableStateOf(true) }
    var currentSecond by remember(videoKey) { mutableFloatStateOf(0f) }
    var duration by remember(videoKey) { mutableFloatStateOf(0f) }
    var controlsVisible by remember(videoKey) { mutableStateOf(true) }
    // Bumped on every key press; the auto-hide timer keys off this rather than
    // off the playback position, which would restart it once a second.
    var interactionPulse by remember(videoKey) { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }

    // The embedded player is the flaky part of this app. If it has not reported
    // itself ready in a reasonable time, stop waiting and offer the fallback
    // rather than leaving the user on a black screen with no way forward.
    LaunchedEffect(videoKey, ready) {
        if (!ready) {
            delay(READY_TIMEOUT_MS)
            if (!ready) failure = PlaybackFailure.Timeout
        }
    }

    LaunchedEffect(videoKey) {
        runCatching { focusRequester.requestFocus() }
    }

    // Controls reveal themselves on interaction and then get out of the way.
    LaunchedEffect(interactionPulse) {
        controlsVisible = true
        delay(CONTROLS_TIMEOUT_MS)
        controlsVisible = false
    }

    val currentFailure = failure
    if (currentFailure != null) {
        TrailerFallback(
            videoKey = videoKey,
            movieTitle = target.movieTitle,
            failure = currentFailure,
            onExit = onExit,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val current = player ?: return@onKeyEvent false
                interactionPulse++
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar, Key.MediaPlayPause -> {
                        if (playing) current.pause() else current.play()
                        true
                    }
                    Key.MediaPlay -> { current.play(); true }
                    Key.MediaPause -> { current.pause(); true }
                    Key.DirectionLeft, Key.MediaRewind -> {
                        current.seekTo((currentSecond - SEEK_SECONDS).coerceAtLeast(0f))
                        true
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        current.seekTo(currentSecond + SEEK_SECONDS)
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    // Keep the WebView out of the focus graph: every key press
                    // must reach the Compose overlay above, which owns the
                    // D-pad mapping.
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    isFocusable = false
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)

                    // The builder takes the context so it can set the IFrame
                    // `origin` to this app's package. Older versions claimed
                    // https://www.youtube.com, which YouTube now rejects.
                    val options = IFramePlayerOptions.Builder(ctx)
                        .controls(0)
                        .rel(0)
                        .build()

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                player = youTubePlayer
                                ready = true
                                youTubePlayer.loadVideo(videoKey, 0f)
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState,
                            ) {
                                playing = state == PlayerConstants.PlayerState.PLAYING
                                if (state == PlayerConstants.PlayerState.ENDED) onExit()
                            }

                            override fun onCurrentSecond(
                                youTubePlayer: YouTubePlayer,
                                second: Float,
                            ) {
                                currentSecond = second
                            }

                            override fun onVideoDuration(
                                youTubePlayer: YouTubePlayer,
                                videoDuration: Float,
                            ) {
                                duration = videoDuration
                            }

                            override fun onError(
                                youTubePlayer: YouTubePlayer,
                                error: PlayerConstants.PlayerError,
                            ) {
                                // Covers videos their owner blocked from being
                                // embedded, which no amount of retrying fixes.
                                failure = PlaybackFailure.Reported(error)
                            }
                        },
                        options,
                    )
                }
            },
            onRelease = { view ->
                // The observer is on the activity's lifecycle, which outlives
                // this screen: without removing it, every trailer watched
                // leaves a released view behind still receiving events.
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            },
        )

        if (!ready) {
            LoadingState(Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = controlsVisible && ready,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PlayerOverlay(
                title = target.movieTitle,
                videoName = target.video.name,
                progress = if (duration > 0f) (currentSecond / duration).coerceIn(0f, 1f) else 0f,
                position = currentSecond,
                duration = duration,
            )
        }
    }
}

@Composable
private fun PlayerOverlay(
    title: String,
    videoName: String,
    progress: Float,
    position: Float,
    duration: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.9f),
                )
            )
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        if (videoName.isNotBlank()) {
            Text(
                text = videoName,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Gold)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${formatTime(position)} / ${formatTime(duration)}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Text(
                text = stringResource(R.string.player_hint_controls),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun TrailerFallback(
    videoKey: String,
    movieTitle: String,
    failure: PlaybackFailure,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Naming the cause turns a bug report into something actionable: a blocked
    // embed and a WebView that never woke up need different fixes.
    val reason = when (failure) {
        PlaybackFailure.Timeout -> stringResource(R.string.player_error_timeout)
        is PlaybackFailure.Reported ->
            stringResource(R.string.player_error_code, failure.error.name)
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = movieTitle,
            style = MaterialTheme.typography.titleLarge,
            color = Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text(
            text = stringResource(R.string.player_error),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.player_error_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TvButton(
                text = stringResource(R.string.player_open_in_youtube),
                onClick = {
                    if (!openInYouTube(context, videoKey)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_youtube_missing),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                focusRequester = focusRequester,
            )
            TvButton(
                text = stringResource(R.string.action_back),
                onClick = onExit,
                primary = false,
            )
        }
    }
}

/** Why the embedded player gave up, so the fallback screen can say which. */
private sealed interface PlaybackFailure {
    /** It never reported itself ready — WebView trouble, or no network. */
    data object Timeout : PlaybackFailure

    /** The IFrame player named a cause, e.g. an embed the owner disabled. */
    data class Reported(val error: PlayerConstants.PlayerError) : PlaybackFailure
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private const val SEEK_SECONDS = 10f

// Cold-starting a WebView on TV hardware is slow, and the projector is slower
// than a phone. Waiting a while beats sending the user to the fallback for a
// player that was merely still booting.
private const val READY_TIMEOUT_MS = 25_000L
private const val CONTROLS_TIMEOUT_MS = 3_500L
