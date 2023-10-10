package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.composeScope
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.toasterofbread.spmp.ui.layout.nowplaying.ThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.getThemeColour
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

const val NOW_PLAYING_MAIN_PADDING = 10f

internal const val MINIMISED_NOW_PLAYING_HORIZ_PADDING: Float = 10f
internal const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200
internal const val SEEK_BAR_GRADIENT_OVERFLOW_RATIO: Float = 0.3f
private const val ACCENT_CLEAR_WAIT_TIME_MS: Long = 1000

@Composable
fun ColumnScope.NowPlayingMainTab(onTopBarHeightChanged: (Dp) -> Unit, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current

    val current_song: Song? by player.status.song_state
    val expansion = LocalNowPlayingExpansion.current

    var theme_colour by remember { mutableStateOf<Color?>(null) }
    var colour_song: Song? by remember { mutableStateOf(null) }

    var seek_state by remember { mutableStateOf(-1f) }

    fun setThemeColour(value: Color?, custom: Boolean) {
        theme_colour = value
        Theme.currentThumbnnailColourChanged(theme_colour)

        if (custom) {
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NowPlayingTopBar(onHeightChanged = onTopBarHeightChanged)

        val screen_width = player.screen_size.width

        composeScope {
            ThumbnailRow(
                Modifier
                    .padding(
                        top = MINIMISED_NOW_PLAYING_V_PADDING_DP.dp
                            * (1f - expansion.getBounded())
                            .coerceAtLeast(0f)
                    )
                    .height(
                        (expansion.getAbsolute() * (screen_width - (NOW_PLAYING_MAIN_PADDING.dp * 2)))
                            .coerceAtLeast(
                                MINIMISED_NOW_PLAYING_HEIGHT_DP.dp - (MINIMISED_NOW_PLAYING_V_PADDING_DP.dp * 2)
                            )
                    )
                    .width(
                        screen_width -
                            (2 * (MINIMISED_NOW_PLAYING_HORIZ_PADDING.dp + ((MINIMISED_NOW_PLAYING_HORIZ_PADDING.dp - NOW_PLAYING_MAIN_PADDING.dp) * expansion.getAbsolute())))
                    )
                    .weight(1f, false),
                onThumbnailLoaded = { song, image ->
                    onThumbnailLoaded(song, image)
                },
                setThemeColour = {
                    setThemeColour(it, true)
                },
                getSeekState = { seek_state }
            )
        }

        val controls_visible by remember { derivedStateOf { expansion.getAbsolute() > 0.0f } }
        if (controls_visible) {
            Controls(
                current_song,
                {
                    player.withPlayer {
                        seekTo((duration_ms * it).toLong())
                    }
                    seek_state = it
                },
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = 1f - (1f - expansion.getBounded()).absoluteValue
                    }
                    .padding(horizontal = NOW_PLAYING_MAIN_PADDING.dp)
            )
        }
    }
}
