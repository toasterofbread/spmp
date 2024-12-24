package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.getThemeColour
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import kotlinx.coroutines.delay

private const val ACCENT_CLEAR_WAIT_TIME_MS: Long = 1000
private const val NARROW_PLAYER_MAX_SIZE_DP: Float = 120f

fun FormFactor.getMinimisedPlayerHeight(): Dp =
    when (this) {
        FormFactor.PORTRAIT -> 64.dp
        FormFactor.LANDSCAPE ->
            if (Platform.DESKTOP.isCurrent()) 80.dp
            else 70.dp
    }

fun FormFactor.getMinimisedPlayerVPadding(): Dp =
    when (this) {
        FormFactor.PORTRAIT -> 7.dp
        FormFactor.LANDSCAPE ->
            if (Platform.DESKTOP.isCurrent()) 10.dp
            else 8.dp
    }

class NowPlayingMainTabPage: NowPlayingPage() {
    private var theme_colour by mutableStateOf<Color?>(null)
    private var colour_song: Song? by mutableStateOf(null)
    var seek_state by mutableStateOf(-1f)

    private lateinit var player: PlayerState
    private val current_song: Song? get() = player.status.m_song

    fun setThemeColour(value: Color?, custom: Boolean) {
        theme_colour = value
        player.theme.onContextualColourChanged(theme_colour)

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

    override fun getPlayerBackgroundColourOverride(player: PlayerState): Color? {
//        if (Platform.DESKTOP.isCurrent()) {
//            return player.theme.accent.blendWith(player.theme.background, 0.05f)
//        }
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

        BoxWithConstraints(modifier) {
            if (maxWidth <= NARROW_PLAYER_MAX_SIZE_DP.dp) {
                NowPlayingMainTabNarrow(page_height, top_bar, content_padding, true)
            }
            else if (maxHeight <= NARROW_PLAYER_MAX_SIZE_DP.dp) {
                NowPlayingMainTabNarrow(page_height, top_bar, content_padding, false)
            }
            else {
                when (player.form_factor) {
                    FormFactor.PORTRAIT -> NowPlayingMainTabPortrait(page_height, top_bar, content_padding, Modifier.fillMaxWidth())
                    FormFactor.LANDSCAPE -> NowPlayingMainTabLarge(page_height, top_bar, content_padding, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
