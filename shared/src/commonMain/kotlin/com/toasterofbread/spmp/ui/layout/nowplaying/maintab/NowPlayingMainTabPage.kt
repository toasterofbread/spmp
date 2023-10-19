package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.isPortrait
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.utils.common.getThemeColour
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
