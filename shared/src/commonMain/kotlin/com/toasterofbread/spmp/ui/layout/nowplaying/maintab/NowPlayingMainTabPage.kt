package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.common.getThemeColour
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.isLargeFormFactor
import com.toasterofbread.spmp.platform.isPortrait
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import kotlinx.coroutines.delay

private const val ACCENT_CLEAR_WAIT_TIME_MS: Long = 1000

class NowPlayingMainTabPage: NowPlayingPage() {
    enum class Mode {
        PORTRAIT, LANDSCAPE, LARGE;

        fun getMinimisedPlayerHeight(): Dp =
            when (this) {
                LARGE -> 80.dp
                else -> 64.dp
            }

        fun getMinimisedPlayerVPadding(): Dp =
            when (this) {
                LARGE -> 10.dp
                else -> 7.dp
            }

        companion object {
            fun getCurrent(player: PlayerState): Mode =
                if (player.isPortrait()) PORTRAIT
                else if (player.isLargeFormFactor()) LARGE
                else LANDSCAPE
        }
    }

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

    override fun getPlayerBackgroundColourOverride(player: PlayerState): Color? {
        if (!player.isPortrait() && player.isLargeFormFactor()) {
            return player.theme.background.blendWith(player.getNPBackground(), player.expansion.getBounded())
        }
        return null
    }

    @Composable
    override fun Page(page_height: Dp, top_bar: NowPlayingTopBar, content_padding: PaddingValues, swipe_modifier: Modifier, modifier: Modifier) {
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
            NowPlayingMainTabPortrait(page_height, top_bar, content_padding, modifier)
        }
        else if (player.isLargeFormFactor()) {
            NowPlayingMainTabLarge(page_height, top_bar, content_padding, modifier)
        }
        else {
            NowPlayingMainTabLandscape(page_height, top_bar, content_padding, modifier)
        }
    }
}
