package com.spectre7.spmp.ui.layout.nowplaying

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MusicTopBarMode
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.ui.component.LikeDislikeButton
import com.spectre7.spmp.ui.component.MusicTopBar
import com.spectre7.utils.setAlpha

@Composable
fun TopBar(modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    val show_in_queue: Boolean = Settings.KEY_TOPBAR_SHOW_IN_QUEUE.get()
    val top_bar_height = if (!show_in_queue || expansion.getBounded() < 1f) expansion.getAppearing() else 1f
    val hide_when_empty: Boolean by Settings.KEY_TOPBAR_HIDE_WHEN_EMPTY_IN_QUEUE.rememberMutableState()

    val max_height by animateDpAsState(
        if (!expansion.top_bar_showing.value && hide_when_empty) NOW_PLAYING_TOP_BAR_HEIGHT.dp * (2f - expansion.get().coerceIn(1f, 2f))
        else NOW_PLAYING_TOP_BAR_HEIGHT.dp
    )

    val target_mode: MutableState<MusicTopBarMode> = remember { mutableStateOf(Settings.KEY_TOPBAR_DEFAULT_MODE_NOWPLAYING.getEnum()) }
    val buttons_alpha = 1f - expansion.getDisappearing()

    Crossfade(
        PlayerServiceHost.status.m_song,
        modifier
            .fillMaxWidth()
            .requiredHeight(minOf(NOW_PLAYING_TOP_BAR_HEIGHT.dp * top_bar_height, max_height))
            .padding(horizontal = NOW_PLAYING_MAIN_PADDING.dp)
            .graphicsLayer { alpha = if (!show_in_queue || expansion.getBounded() < 1f) 1f - expansion.getDisappearing() else 1f }
    ) { song ->
        if (song == null) {
            return@Crossfade
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
            val buttons_visible by remember { derivedStateOf { buttons_alpha > 0f } }

            LikeDislikeButton(
                song,
                Modifier.width(40.dp * buttons_alpha).fillMaxHeight().graphicsLayer { alpha = buttons_alpha },
                buttons_visible,
                { getNPOnBackground().setAlpha(0.5f) }
            )

            MusicTopBar(
                song,
                Settings.KEY_TOPBAR_DEFAULT_MODE_NOWPLAYING.getEnum(),
                Modifier.fillMaxSize().weight(1f),
                target_mode,
                expansion.top_bar_showing
            )

            IconButton(
                {
                    player.onMediaItemLongClicked(song, PlayerServiceHost.status.index)
                },
                Modifier.graphicsLayer { alpha = buttons_alpha }.width(40.dp * buttons_alpha),
                enabled = buttons_visible
            ) {
                Icon(Icons.Filled.MoreHoriz, null, tint = getNPOnBackground().setAlpha(0.5f))
            }
        }
    }
}
