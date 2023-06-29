package com.spectre7.spmp.ui.layout.nowplaying

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.MusicTopBarMode
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.ui.component.LikeDislikeButton
import com.spectre7.spmp.ui.component.MusicTopBarWithVisualiser
import com.spectre7.utils.setAlpha

@Composable
fun TopBar(modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    val show_lyrics_in_queue: Boolean by Settings.KEY_TOPBAR_SHOW_LYRICS_IN_QUEUE.rememberMutableState()
    val show_visualiser_in_queue: Boolean by Settings.KEY_TOPBAR_SHOW_VISUALISER_IN_QUEUE.rememberMutableState()

    val hide_in_queue =
        when (expansion.top_bar_mode.value) {
            MusicTopBarMode.VISUALISER -> !show_visualiser_in_queue
            MusicTopBarMode.LYRICS -> !show_lyrics_in_queue
        }

    val top_bar_height = if (hide_in_queue || expansion.getBounded() < 1f) expansion.getAppearing() else 1f

    val max_height by animateDpAsState(
        if (hide_in_queue) NOW_PLAYING_TOP_BAR_HEIGHT.dp * (2f - expansion.get().coerceIn(1f, 2f))
        else NOW_PLAYING_TOP_BAR_HEIGHT.dp
    )

    Crossfade(
        player.status.m_song,
        modifier
            .fillMaxWidth()
            .requiredHeight(minOf(NOW_PLAYING_TOP_BAR_HEIGHT.dp * top_bar_height, max_height))
            .padding(horizontal = NOW_PLAYING_MAIN_PADDING.dp)
            .graphicsLayer { alpha = if (hide_in_queue || expansion.getBounded() < 1f) 1f - expansion.getDisappearing() else 1f }
    ) { song ->
        if (song == null) {
            return@Crossfade
        }
        
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            val buttons_alpha = 1f - expansion.getDisappearing()
            val buttons_enabled = buttons_alpha > 0f

            LikeDislikeButton(
                song,
                Modifier.width(40.dp * buttons_alpha).fillMaxHeight().graphicsLayer { alpha = buttons_alpha },
                buttons_enabled,
                { getNPOnBackground().setAlpha(0.5f) }
            )

            MusicTopBarWithVisualiser(
                Settings.INTERNAL_TOPBAR_MODE_NOWPLAYING,
                Modifier.fillMaxSize().weight(1f),
                song = song
            )

            IconButton(
                {
                    player.onMediaItemLongClicked(song, player.status.m_index)
                },
                Modifier.graphicsLayer { alpha = buttons_alpha }.width(40.dp * buttons_alpha),
                enabled = buttons_enabled
            ) {
                Icon(Icons.Filled.MoreHoriz, null, tint = getNPOnBackground().setAlpha(0.5f))
            }
        }
    }
}
