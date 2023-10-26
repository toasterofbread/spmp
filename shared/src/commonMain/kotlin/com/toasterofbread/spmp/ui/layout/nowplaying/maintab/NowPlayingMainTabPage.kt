package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.toastercomposetools.platform.composable.BackHandler
import com.toasterofbread.toastercomposetools.platform.composable.composeScope
import com.toasterofbread.spmp.platform.isPortrait
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.ThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.QueueTab
import com.toasterofbread.toastercomposetools.utils.common.getThemeColour
import com.toasterofbread.toastercomposetools.utils.common.launchSingle
import com.toasterofbread.toastercomposetools.utils.modifier.recomposeHighlighter
import kotlinx.coroutines.delay

private const val ACCENT_CLEAR_WAIT_TIME_MS: Long = 1000

class NowPlayingMainTabPage: NowPlayingPage() {
    private var theme_colour by mutableStateOf<Color?>(null)
    private var colour_song: Song? by mutableStateOf(null)
    var seek_state by mutableStateOf(-1f)

    private lateinit var player: PlayerState
    private val current_song: Song? get() = player.status.m_song

    fun setThemeColour(value: Color?, custom: Boolean) {
        theme_colour = value
        player.theme.currentThumbnnailColourChanged(theme_colour)

        if (custom) {
            player.status.song
            current_song?.ThemeColour?.set(theme_colour, player.database)
        }

        if (value != null) {
            colour_song = current_song
        }
    }

    fun onThumbnailLoaded(song: Song?, image: ImageBitmap?) {
        if (song?.id != current_song?.id || (song?.id == colour_song?.id && image == null)) {
            return
        }

        if (song == null) {
            setThemeColour(null, false)
        }
        else {
            val song_theme = song.ThemeColour.get(player.database)
            if (song_theme != null) {
                setThemeColour(song_theme, false)
            }
            else {
                setThemeColour(image?.getThemeColour(), false)
            }
        }
    }

    override fun shouldShow(player: PlayerState): Boolean = true

    @Composable
    override fun Page(page_height: Dp, top_bar: NowPlayingTopBar, modifier: Modifier) {
        player = LocalPlayerState.current
        val current_song: Song? by player.status.song_state

        LaunchedEffect(current_song) {
            val song = current_song

            if (song?.id == colour_song?.id) {
                return@LaunchedEffect
            }

            if (song != null) {
                val song_theme = song.ThemeColour.get(player.database)
                if (song_theme != null) {
                    setThemeColour(song_theme, false)
                    return@LaunchedEffect
                }
            }

            delay(ACCENT_CLEAR_WAIT_TIME_MS)

            if (song?.id != colour_song?.id) {
                onThumbnailLoaded(song, null)
            }
        }

        if (player.isPortrait()) {
            NowPlayingMainTabPortrait(top_bar, modifier)
        }
        else {
            NowPlayingMainTabLandscape(page_height, top_bar, modifier)
        }
    }
}
