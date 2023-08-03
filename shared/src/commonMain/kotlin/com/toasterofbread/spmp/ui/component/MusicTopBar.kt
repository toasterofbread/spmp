package com.toasterofbread.spmp.ui.component

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.MusicTopBarMode
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.composeScope
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.toasterofbread.utils.composable.loadLyricsOnSongChange
import com.toasterofbread.utils.composable.pauseableInfiniteRepeatableAnimation
import com.toasterofbread.utils.getContrasted
import com.toasterofbread.utils.setAlpha
import kotlinx.coroutines.delay

@Composable
fun MusicTopBarWithVisualiser(
    target_mode_key: Settings,
    modifier: Modifier = Modifier,
    song: Song? = LocalPlayerState.current.status.m_song,
    can_show_visualiser: Boolean = false,
    hide_while_inactive: Boolean = true,
    padding: PaddingValues = PaddingValues(),
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    var target_mode: MusicTopBarMode by target_mode_key.rememberMutableEnumState()
    val show_toast = remember { mutableStateOf(false) }

    MusicTopBar(
        { target_mode },
        true,
        can_show_visualiser,
        hide_while_inactive,
        modifier,
        song,
        padding,
        innerContent = { mode ->
            Crossfade(Pair(target_mode, mode), Modifier.fillMaxSize()) { state ->
                val (target, current) = state

                val toast_alpha = remember { Animatable(if (show_toast.value) 1f else 0f) }
                LaunchedEffect(Unit) {
                    if (!show_toast.value) {
                        return@LaunchedEffect
                    }

                    show_toast.value = false
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
        },
        onClick = {
            target_mode = target_mode.getNext(can_show_visualiser)
            show_toast.value = true
        },
        onShowingChanged = onShowingChanged
    )
}

@Composable
fun MusicTopBar(
    can_show_key: Settings,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    getBottomBorderOffset: ((height: Int) -> Int)? = null,
    getBottomBorderColour: (() -> Color)? = null,
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    val can_show: Boolean by can_show_key.rememberMutableState()
    MusicTopBar(
        { MusicTopBarMode.LYRICS },
        can_show = can_show,
        can_show_visualiser = false,
        hide_while_inactive = true,
        modifier = modifier,
        padding = padding,
        getBottomBorderOffset = getBottomBorderOffset,
        getBottomBorderColour = getBottomBorderColour,
        onShowingChanged = onShowingChanged
    )
}

@Composable
private fun MusicTopBar(
    getTargetMode: () -> MusicTopBarMode,
    can_show: Boolean,
    can_show_visualiser: Boolean,
    hide_while_inactive: Boolean,
    modifier: Modifier = Modifier,
    song: Song? = LocalPlayerState.current.status.m_song,
    padding: PaddingValues = PaddingValues(),
    innerContent: (@Composable (MusicTopBarMode) -> Unit)? = null,
    getBottomBorderOffset: ((height: Int) -> Int)? = null,
    getBottomBorderColour: (() -> Color)? = null,
    onClick: (() -> Unit)? = null,
    onShowingChanged: ((Boolean) -> Unit)? = null
) {
    val player = LocalPlayerState.current
    val lyrics = loadLyricsOnSongChange(song, SpMp.context, getTargetMode() == MusicTopBarMode.LYRICS)
    var mode_state: MusicTopBarMode by mutableStateOf(getTargetMode())

    val visualiser_width: Float by Settings.KEY_TOPBAR_VISUALISER_WIDTH.rememberMutableState()
    check(visualiser_width in 0f .. 1f)

    val sync_offset: Long? = song?.LyricsSyncOffset?.observe(SpMp.context.database)?.value

    val current_state by remember {
        derivedStateOf {
            val target = getTargetMode()
            for (mode_i in target.ordinal downTo 0) {
                val mode = MusicTopBarMode.values()[mode_i]
                val state = getModeState(mode, lyrics)
                if (state != null) {
                    mode_state = mode
                    return@derivedStateOf state
                }
            }
            throw NotImplementedError(target.toString())
        }
    }

    val show = !hide_while_inactive || isStateActive(current_state, can_show_visualiser)
    DisposableEffect(show) {
        onShowingChanged?.invoke(show)
        onDispose {
            onShowingChanged?.invoke(false)
        }
    }

    AnimatedVisibility(
        show,
        modifier.platformClickable(
            onClick = onClick,
            onAltClick = {
                if (current_state is SongLyrics) {
                    player.openNowPlayingOverlayMenu(OverlayMenu.getLyricsMenu())
                }
            }
        ),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(Modifier.height(30.dp + padding.calculateTopPadding() + padding.calculateBottomPadding())) {
            Box(Modifier.padding(padding)) {
                innerContent?.invoke(mode_state)

                Crossfade(current_state, Modifier.fillMaxSize()) { state ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (state) {
                            is SongLyrics -> {
                                val linger: Boolean by Settings.KEY_TOPBAR_LYRICS_LINGER.rememberMutableState()
                                val show_furigana: Boolean by Settings.KEY_TOPBAR_LYRICS_SHOW_FURIGANA.rememberMutableState()

                                LyricsLineDisplay(
                                    state,
                                    {
                                        (player.player?.current_position_ms ?: 0) +
                                            (sync_offset ?: 0)
                                    },
                                    linger,
                                    show_furigana,
                                    emptyContent = {
                                        TopBarEmptyContent()
                                    }
                                )
                            }
                            MusicTopBarMode.VISUALISER -> {
                                player.player?.Visualiser(
                                    LocalContentColor.current,
                                    Modifier.fillMaxHeight().fillMaxWidth(visualiser_width).padding(vertical = 10.dp),
                                    opacity = 0.5f
                                )
                            }
                        }
                    }
                }
            }

            composeScope {
                if (getBottomBorderColour != null) {
                    WaveBorder(
                        Modifier.fillMaxWidth().zIndex(-1f),
                        getColour = { getBottomBorderColour() },
                        getOffset = getBottomBorderOffset
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarEmptyContent() {
    val player = LocalPlayerState.current
    val wave_offset by pauseableInfiniteRepeatableAnimation(
        start = 0f,
        end = 1f,
        period = 10000,
        getPlaying = {
            player.status.m_playing
        }
    )

    val colour = LocalContentColor.current.setAlpha(0.15f)
    val stroke_width = with(LocalDensity.current) { 3.dp.toPx() }

    Canvas(
        Modifier
            .padding(horizontal = 10.dp)
            .height(20.dp)
            .fillMaxWidth()
            .clipToBounds()
    ) {
        drawWave(
            height = size.height / 2,
            waves = 3,
            stroke_width = stroke_width,
            getWaveOffset = {
                wave_offset * size.width
            },
            getColour = { colour }
        )
    }
}

private fun getModeState(mode: MusicTopBarMode, lyrics: SongLyrics?): Any? {
    return when (mode) {
        MusicTopBarMode.LYRICS -> if (lyrics?.synced == true) lyrics else null
        MusicTopBarMode.VISUALISER -> mode
    }
}

@Composable
private fun isStateActive(state: Any, can_show_visualiser: Boolean): Boolean = when (state) {
    is SongLyrics -> true
    MusicTopBarMode.VISUALISER -> can_show_visualiser && LocalPlayerState.current.status.m_playing
    else -> false
}
