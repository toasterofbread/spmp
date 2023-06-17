package com.spectre7.spmp.ui.component

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.MusicTopBarMode
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.model.SongLyrics
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.spectre7.utils.composable.rememberSongUpdateLyrics
import com.spectre7.utils.getContrasted
import kotlinx.coroutines.delay

private fun getModeState(mode: MusicTopBarMode, song: Song?): Any? {
    return when (mode) {
        MusicTopBarMode.LYRICS -> song?.lyrics?.lyrics?.let {  lyrics ->
            if (lyrics.synced) lyrics else null
        }
        MusicTopBarMode.VISUALISER -> mode
    }
}

@Composable
private fun isStateActive(state: Any): Boolean = when (state) {
    is SongLyrics -> true
    MusicTopBarMode.VISUALISER -> LocalPlayerState.current.status.m_playing
    else -> false
}

@Composable
fun MusicTopBar(
    target_mode_key: Settings,
    modifier: Modifier = Modifier,
    song: Song? = LocalPlayerState.current.status.m_song,
    can_show_visualiser: Boolean = false,
    hide_while_inactive: Boolean = true,
    padding: PaddingValues = PaddingValues(),
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    val player = LocalPlayerState.current
    val mode_state = LocalNowPlayingExpansion.current.top_bar_mode
    var target_mode: MusicTopBarMode by target_mode_key.rememberMutableEnumState()
    val song_state by rememberSongUpdateLyrics(song, target_mode == MusicTopBarMode.LYRICS)

    var show_toast by remember { mutableStateOf(false) }
    val visualiser_width: Float by Settings.KEY_TOPBAR_VISUALISER_WIDTH.rememberMutableState()
    check(visualiser_width in 0f .. 1f)

    val current_state by remember {
        derivedStateOf {
            for (mode_i in target_mode.ordinal downTo 0) {
                val mode = MusicTopBarMode.values()[mode_i]
                val state = getModeState(mode, song_state)
                if (state != null) {
                    mode_state.value = mode
                    return@derivedStateOf state
                }
            }
            throw NotImplementedError(target_mode.toString())
        }
    }

    val show = !hide_while_inactive || isStateActive(current_state)
    DisposableEffect(show) {
        onShowingChanged?.invoke(show)
        onDispose {
            onShowingChanged?.invoke(false)
        }
    }

    AnimatedVisibility(
        show,
        modifier
            .platformClickable(
                onClick = {
                    target_mode = target_mode.getNext(can_show_visualiser)
                    show_toast = true
                },
                onAltClick = {
                    if (current_state is SongLyrics) {
                        TODO("Open full lyrics in NowPlaying")
                    }
                }
            ),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(Modifier.padding(padding).height(30.dp), contentAlignment = Alignment.Center) {
            Crossfade(Pair(target_mode, mode_state.value), Modifier.fillMaxSize()) { state ->
                val (target, current) = state

                val toast_alpha = remember { Animatable(if (show_toast) 1f else 0f) }
                LaunchedEffect(Unit) {
                    if (!show_toast) {
                        return@LaunchedEffect
                    }

                    show_toast = false
                    delay(500)
                    toast_alpha.animateTo(0f)
                }

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .graphicsLayer { alpha = toast_alpha.value }
                            .background(LocalContentColor.current, RoundedCornerShape(16.dp)),
                    ) {
                        Row(
                            Modifier.padding(vertical = 5.dp, horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val colour = LocalContentColor.current.getContrasted()
                            Icon(
                                target.getIcon(),
                                null,
                                tint = colour
                            )

                            if (target != current) {
                                Text(getString("topbar_mode_unavailable"), color = colour)
                            }
                        }
                    }
                }
            }

            Crossfade(current_state, Modifier.fillMaxSize()) { s ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (s) {
                        is SongLyrics -> {
                            val linger: Boolean by Settings.KEY_TOPBAR_LYRICS_LINGER.rememberMutableState()
                            val show_furigana: Boolean by Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.rememberMutableState()

                            LyricsLineDisplay(
                                s,
                                {
                                    (player.player?.current_position_ms ?: 0) +
                                        (song?.song_reg_entry?.getLyricsSyncOffset() ?: 0)
                                },
                                linger,
                                show_furigana
                            )
                        }
                        MusicTopBarMode.VISUALISER -> {
                            player.player?.Visualiser(
                                LocalContentColor.current,
                                Modifier.fillMaxHeight().fillMaxWidth(visualiser_width).padding(vertical = 10.dp),
                                opacity = 0.5f
                            )
                        }
                        else -> {
                            // TOOD State indicator
                        }
                    }
                }
            }
        }
    }
}
