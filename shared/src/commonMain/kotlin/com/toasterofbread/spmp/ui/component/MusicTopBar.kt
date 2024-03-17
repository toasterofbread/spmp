package com.toasterofbread.spmp.ui.component

import LocalPlayerState
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.cash.sqldelight.Query
import com.toasterofbread.composekit.platform.composable.composeScope
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.composable.AlignableCrossfade
import com.toasterofbread.composekit.utils.composable.pauseableInfiniteRepeatableAnimation
import com.toasterofbread.composekit.utils.composable.wave.drawWave
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.SettingsKey
import com.toasterofbread.spmp.model.settings.category.MusicTopBarMode
import com.toasterofbread.spmp.model.settings.category.TopBarSettings
import com.toasterofbread.spmp.model.settings.rememberMutableEnumState
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.youtubeapi.lyrics.createFuriganaTokeniser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MusicTopBar(val player: PlayerState) {
    var lyrics: SongLyrics? by mutableStateOf(null)
        private set
    private var current_song: Song? by mutableStateOf(null)

    private val coroutine_scope = CoroutineScope(Job())
    private val lyrics_listener = Query.Listener {
        val song = player.status.m_song
        if (song == null) {
            return@Listener
        }

        val reference = song.Lyrics.get(player.database)
        if (lyrics != null && lyrics?.reference == reference) {
            return@Listener
        }

        lyrics = null

        if (reference?.isNone() == true) {
            return@Listener
        }

        coroutine_scope.launchSingle {
            val result: SongLyrics? = (
                if (reference != null) SongLyricsLoader.loadByLyrics(reference, player.context, createFuriganaTokeniser())
                else SongLyricsLoader.loadBySong(song, player.context)
            )?.getOrNull()

            if (result != null) {
                lyrics = result
            }
        }
    }

    init {
        reconnect()
    }

    fun reconnect() {
        player.database.songQueries.lyricsById("").addListener(lyrics_listener)
    }

    fun release() {
        player.database.songQueries.lyricsById("").removeListener(lyrics_listener)
    }

    @Composable
    fun MusicTopBarWithVisualiser(
        target_mode_key: SettingsKey,
        modifier: Modifier = Modifier,
        song: Song? = LocalPlayerState.current.status.m_song,
        can_show_visualiser: Boolean = false,
        hide_while_inactive: Boolean = true,
        alignment: Alignment = Alignment.Center,
        padding: PaddingValues = PaddingValues()
    ): MusicTopBarState {
        var target_mode: MusicTopBarMode by target_mode_key.rememberMutableEnumState()
        val show_toast: MutableState<Boolean> = remember { mutableStateOf(false) }

        return MusicTopBar(
            { target_mode },
            true,
            can_show_visualiser,
            hide_while_inactive,
            modifier,
            song,
            padding,
            alignment,
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
            }
        )
    }

    @Composable
    fun MusicTopBar(
        can_show_key: SettingsKey,
        modifier: Modifier = Modifier,
        padding: PaddingValues = PaddingValues(),
        getBottomBorderOffset: ((height: Float) -> Float)? = null,
        getBottomBorderColour: (() -> Color)? = null
    ): MusicTopBarState {
        val can_show: Boolean by can_show_key.rememberMutableState()
        return MusicTopBar(
            { MusicTopBarMode.LYRICS },
            can_show = can_show,
            can_show_visualiser = false,
            hide_while_inactive = true,
            modifier = modifier,
            padding = padding,
            getBottomBorderOffset = getBottomBorderOffset,
            getBottomBorderColour = getBottomBorderColour
        )
    }

    data class MusicTopBarState(val top_padding: Dp, val showing: Boolean)

    @Composable
    private fun MusicTopBar(
        getTargetMode: () -> MusicTopBarMode,
        can_show: Boolean,
        can_show_visualiser: Boolean,
        hide_while_inactive: Boolean,
        modifier: Modifier = Modifier,
        song: Song? = LocalPlayerState.current.status.m_song,
        padding: PaddingValues = PaddingValues(),
        alignment: Alignment = Alignment.Center,
        innerContent: (@Composable (MusicTopBarMode) -> Unit)? = null,
        getBottomBorderOffset: ((height: Float) -> Float)? = null,
        getBottomBorderColour: (() -> Color)? = null,
        onClick: (() -> Unit)? = null
    ): MusicTopBarState {
        val player = LocalPlayerState.current
        var mode_state: MusicTopBarMode by mutableStateOf(getTargetMode())

        val lyrics_enabled: Boolean by TopBarSettings.Key.LYRICS_ENABLE.rememberMutableState()
        val visualiser_width: Float by TopBarSettings.Key.VISUALISER_WIDTH.rememberMutableState()
        check(visualiser_width in 0f .. 1f)

        val sync_offset_state: State<Long?>? = song?.getLyricsSyncOffset(player.database, true)

        val current_state by remember(lyrics, lyrics_enabled) {
            derivedStateOf {
                val target = getTargetMode()
                for (mode_i in target.ordinal downTo 0) {
                    val mode = MusicTopBarMode.entries[mode_i]
                    val state = getModeState(mode, if (lyrics_enabled) lyrics else null)
                    if (state != null) {
                        mode_state = mode
                        return@derivedStateOf state
                    }
                }
                throw NotImplementedError(target.toString())
            }
        }

        val show = !hide_while_inactive || isStateActive(current_state, can_show_visualiser)

        LaunchedEffect(song?.id) {
            if (song?.id == current_song?.id) {
                return@LaunchedEffect
            }

            lyrics = null
            current_song = song

            if (song != null) {
                val reference = song.Lyrics.get(player.database)
                if (lyrics == null || lyrics?.reference != reference) {
                    val loaded_lyrics = reference?.let {
                        SongLyricsLoader.getLoadedByLyrics(it)
                    }

                    if (loaded_lyrics != null) {
                        lyrics = loaded_lyrics
                    }
                    else {
                        coroutine_scope.launchSingle {
                            val result: SongLyrics? =
                                SongLyricsLoader.loadBySong(song, player.context)?.getOrNull()

                            if (result != null) {
                                lyrics = result
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            show,
            modifier
                .height(IntrinsicSize.Min)
                .platformClickable(
                    onClick = { onClick?.invoke() },
                    onAltClick = {
                        if (current_state is SongLyrics) {
                            player.openNowPlayingPlayerOverlayMenu(PlayerOverlayMenu.getLyricsMenu())
                        }
                    }
                ),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
                Column(
                    Modifier.heightIn(30.dp + padding.calculateTopPadding() + padding.calculateBottomPadding()).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        innerContent?.invoke(mode_state)

                        AlignableCrossfade(current_state, Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { state ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                when (state) {
                                    is SongLyrics -> {
                                        LyricsLineDisplay(
                                            lyrics = state,
                                            getTime = {
                                                (player.controller?.current_position_ms ?: 0) +
                                                    (sync_offset_state?.value ?: 0)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            emptyContent = {
                                                TopBarEmptyContent()
                                            }
                                        )
                                    }
                                    MusicTopBarMode.VISUALISER -> {
                                        player.controller?.Visualiser(
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

        return MusicTopBarState(
            if (!show) padding.calculateTopPadding() else if (getBottomBorderColour != null) WAVE_BORDER_HEIGHT_DP.dp else 0.dp,
            show
        )
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

    val colour = LocalContentColor.current.copy(alpha = 0.15f)
    val stroke_width = with(LocalDensity.current) { 3.dp.toPx() }

    Canvas(
        Modifier
            .padding(horizontal = 10.dp)
            .height(20.dp)
            .fillMaxWidth()
            .clipToBounds()
    ) {
        drawWave(
            wave_size = size.copy(height = size.height / 2),
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
    is SongLyrics -> LocalPlayerState.current.form_factor != FormFactor.LANDSCAPE
    MusicTopBarMode.VISUALISER -> can_show_visualiser && LocalPlayerState.current.status.m_playing
    else -> false
}
