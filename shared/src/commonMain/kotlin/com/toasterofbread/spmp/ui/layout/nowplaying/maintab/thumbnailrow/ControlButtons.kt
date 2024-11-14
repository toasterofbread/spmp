package com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow

import LocalPlayerState
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import com.toasterofbread.spmp.platform.playerservice.seekToPreviousOrRepeat
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.media_pause
import spmp.shared.generated.resources.media_play

@Composable
fun ThumbnailRowControlButtons(
    button_modifier: Modifier = Modifier,
    image_modifier: Modifier = Modifier,
    show_prev_button: Boolean = true,
    rounded_icons: Boolean = true
) {
    val player: PlayerState = LocalPlayerState.current

    if (show_prev_button) {
        IconButton({ player.controller?.seekToPreviousOrRepeat() }, button_modifier) {
            Image(
                if (rounded_icons) Icons.Rounded.SkipPrevious else Icons.Default.SkipPrevious,
                null,
                image_modifier,
                colorFilter = ColorFilter.tint(player.getNPOnBackground())
            )
        }
    }

    IconButton({ player.controller?.playPause() }, button_modifier) {
        Image(
            if (player.status.m_playing) {
                if (rounded_icons) Icons.Rounded.Pause else Icons.Default.Pause
            }
            else {
                if (rounded_icons) Icons.Rounded.PlayArrow else Icons.Default.PlayArrow
            },
            stringResource(if (player.status.m_playing) Res.string.media_pause else Res.string.media_play),
            image_modifier,
            colorFilter = ColorFilter.tint(player.getNPOnBackground())
        )
    }

    IconButton({ player.controller?.seekToNext() }, button_modifier) {
        Image(
            if (rounded_icons) Icons.Rounded.SkipNext else Icons.Default.SkipNext,
            null,
            image_modifier,
            colorFilter = ColorFilter.tint(player.getNPOnBackground())
        )
    }
}
