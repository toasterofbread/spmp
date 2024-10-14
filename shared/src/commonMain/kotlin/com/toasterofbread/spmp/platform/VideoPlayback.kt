package com.toasterofbread.spmp.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import LocalPlayerState
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import com.toasterofbread.spmp.model.mediaitem.song.getSongFormats

expect fun doesPlatformSupportVideoPlayback(): Boolean

@Composable
expect fun VideoPlayback(
    url: String,
    getPositionMs: () -> Long,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    getAlpha: () -> Float = { 1f }
): Boolean

@Composable
fun SongVideoPlayback(
    song_id: String,
    getPositionMs: () -> Long,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    getAlpha: () -> Float = { 1f }
): Boolean {
    val player: PlayerState = LocalPlayerState.current
    var playback_url: String? by remember { mutableStateOf(null) }

    LaunchedEffect(song_id) {
        playback_url = null

        val format: YoutubeVideoFormat =
            getSongFormats(
                song_id,
                player.context,
                filter = { format ->
                    format.mimeType.startsWith("video/mp4")
                }
            ).getOrNull()?.firstOrNull() ?: return@LaunchedEffect

        playback_url = format.url
    }

    playback_url?.also { url ->
        return VideoPlayback(
            url,
            getPositionMs = getPositionMs,
            modifier = modifier,
            fill = fill,
            getAlpha = getAlpha
        )
    }

    return false
}
