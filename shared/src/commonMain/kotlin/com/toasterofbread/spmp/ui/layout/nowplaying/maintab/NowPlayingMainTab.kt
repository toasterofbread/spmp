package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import SpMp
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.composeScope
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.toasterofbread.spmp.ui.layout.nowplaying.ThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.TopBar
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.getThemeColour
import kotlin.math.absoluteValue

const val NOW_PLAYING_MAIN_PADDING = 10f

internal const val MINIMISED_NOW_PLAYING_HORIZ_PADDING = 10f
internal const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200
internal const val NOW_PLAYING_TOP_BAR_HEIGHT: Int = 40
internal const val SEEK_BAR_GRADIENT_OVERFLOW_RATIO = 0.3f

@Composable
fun ColumnScope.NowPlayingMainTab(modifier: Modifier = Modifier) {
    val db = SpMp.context.database
    val player = LocalPlayerState.current

    val current_song: Song? = player.status.m_song
    val expansion = LocalNowPlayingExpansion.current

    var theme_colour by remember { mutableStateOf<Color?>(null) }
    fun setThemeColour(value: Color?) {
        theme_colour = value

        current_song?.id?.also { song_id ->
            db.mediaItemQueries.updateThemeColourById(theme_colour?.toArgb()?.toLong(), song_id)
        }
    }

    var seek_state by remember { mutableStateOf(-1f) }

    LaunchedEffect(theme_colour) {
        Theme.currentThumbnnailColourChanged(theme_colour)
    }

    fun onThumbnailLoaded(song: Song?, image: ImageBitmap?) {
        if (song != current_song) {
            return
        }

        if (song == null) {
            theme_colour = null
        }
        else {
            val song_theme = song.ThemeColour.get(db)
            if (song_theme != null) {
                theme_colour = song_theme
            }
            else {
                theme_colour = image?.getThemeColour()
            }
        }
    }

    LaunchedEffect(current_song) {
        onThumbnailLoaded(current_song, null)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar()

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
                onThumbnailLoaded = { song, image -> onThumbnailLoaded(song, image) },
                setThemeColour = { setThemeColour(it) },
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
