package vn.hocdantranh.guzheng.ui.lessons

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import vn.hocdantranh.guzheng.R

/**
 * Plays a lesson from YouTube.
 *
 * Mirrors the iOS `LessonVideoPlayer`, which wraps YouTubePlayerKit: both drive YouTube's
 * IFrame API inside a web view, which is what satisfies YouTube's 2025 embedded-playback
 * enforcement (errors 150–153). Rolling our own web view would mean re-solving that.
 *
 * On a player error the view is replaced with a link out to the YouTube app, the same
 * fallback iOS offers.
 */
@Composable
fun YouTubeLessonPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    var failed by remember(videoId) { mutableStateOf(false) }

    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (failed) {
            PlaybackFallback(videoId)
        } else {
            YouTubeWebPlayer(videoId = videoId, onError = { failed = true })
        }
    }
}

@Composable
private fun YouTubeWebPlayer(
    videoId: String,
    onError: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = remember(videoId) { mutableStateOf<YouTubePlayerView?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            YouTubePlayerView(context).also { player ->
                view.value = player
                // Initialize by hand so the video ID can be injected into the page itself.
                player.enableAutomaticInitialization = false
                // Lets the library pause and release playback with the host lifecycle.
                lifecycleOwner.lifecycle.addObserver(player)

                val listener = object : AbstractYouTubePlayerListener() {
                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError,
                    ) {
                        onError()
                    }
                }


                player.initialize(
                    youTubePlayerListener = listener,
                    handleNetworkEvents = true,
                    playerOptions = IFramePlayerOptions.Builder()
                        .controls(1)
                        .origin(YOUTUBE_ORIGIN)
                        .build(),
                    videoId = videoId,
                )
            }
        },
    )

    DisposableEffect(videoId) {
        onDispose {
            view.value?.let { player ->
                lifecycleOwner.lifecycle.removeObserver(player)
                player.release()
            }
        }
    }
}

/** Shown when YouTube refuses in-app playback — hands the video to the YouTube app instead. */
@Composable
private fun PlaybackFallback(videoId: String) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.lesson_player_error),
            color = Color(0xFFBFBFBF),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_WATCH_URL + videoId))
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000),
                contentColor = Color.White,
            ),
        ) {
            Text(text = stringResource(R.string.lesson_watch_on_youtube), fontSize = 15.sp)
        }
    }
}

private const val YOUTUBE_ORIGIN = "https://www.youtube.com"
private const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
