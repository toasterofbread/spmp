package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.top_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.ThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.composekit.platform.composable.composeScope
import kotlin.math.absoluteValue

internal const val MINIMISED_NOW_PLAYING_HORIZ_PADDING: Float = 10f
internal const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200
internal const val SEEK_BAR_GRADIENT_OVERFLOW_RATIO: Float = 0.3f

private fun BoxWithConstraintsScope.getThumbnailSize(): Dp {
    return minOf(maxWidth - (horizontal_padding * 2), maxHeight - 350.dp)
}

@Composable
internal fun NowPlayingMainTabPage.NowPlayingMainTabPortrait(top_bar: NowPlayingTopBar, content_padding: PaddingValues, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current

    val current_song: Song? by player.status.song_state

    BoxWithConstraints(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            composeScope {
                Spacer(Modifier.height(lerp(0.dp, top_padding, expansion.get().coerceIn(0f, 1f))))
            }

            top_bar.NowPlayingTopBar()

            val thumbnail_size: Dp = this@BoxWithConstraints.getThumbnailSize()
            val controls_height: Dp = this@BoxWithConstraints.maxHeight - thumbnail_size

            composeScope {
                ThumbnailRow(
                    Modifier
                        .size(
                            width = lerp(this@BoxWithConstraints.maxWidth - (MINIMISED_NOW_PLAYING_HORIZ_PADDING.dp * 2), thumbnail_size, expansion.getAbsolute()),
                            height = lerp(MINIMISED_NOW_PLAYING_HEIGHT_DP.dp - (MINIMISED_NOW_PLAYING_V_PADDING_DP.dp * 2), thumbnail_size, expansion.getAbsolute())
                        ),
                    horizontal_arrangement = Arrangement.SpaceEvenly,
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
                Column(
                    Modifier
                        .padding(
                            top = 30.dp,
                            bottom = bottom_padding,
                            start = horizontal_padding,
                            end = horizontal_padding
                        )
                        .height(controls_height),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Controls(
                        current_song,
                        {
                            player.withPlayer {
                                seekTo((duration_ms * it).toLong())
                            }
                            seek_state = it
                        },
                        Modifier
                            .graphicsLayer {
                                alpha = 1f - (1f - expansion.getBounded()).absoluteValue
                            }
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val bottom_row_colour = player.getNPOnBackground().copy(alpha = 0.5f)
                        var show_volume_slider by remember { mutableStateOf(false) }

                        IconButton({ show_volume_slider = !show_volume_slider }) {
                            Icon(Icons.Default.VolumeUp, null, tint = bottom_row_colour)
                        }

                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.CenterStart) {
                            this@Row.AnimatedVisibility(
                                show_volume_slider,
                                enter = expandHorizontally(expandFrom = Alignment.Start),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
                            ) {
                                VolumeSlider(bottom_row_colour)
                            }
                        }
                    }
                }
            }
        }
    }
}
