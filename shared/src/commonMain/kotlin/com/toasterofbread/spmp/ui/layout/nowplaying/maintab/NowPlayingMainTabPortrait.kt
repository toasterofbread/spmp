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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import dev.toastbits.composekit.components.platform.composable.composeScope
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import dev.toastbits.composekit.util.composable.copy
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_V_PADDING_DP
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.horizontal_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopBar
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.SmallThumbnailRow
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.RepeatButton
import kotlin.math.absoluteValue

internal const val MINIMISED_NOW_PLAYING_HORIZ_PADDING: Float = 10f
internal const val OVERLAY_MENU_ANIMATION_DURATION: Int = 200
internal const val SEEK_BAR_GRADIENT_OVERFLOW_RATIO: Float = 0.3f

@Composable
private fun BoxWithConstraintsScope.getThumbnailSize(): Dp {
    return minOf(maxWidth - (horizontal_padding * 2), maxHeight - 350.dp)
}

@Composable
internal fun NowPlayingMainTabPage.NowPlayingMainTabPortrait(
    page_height: Dp,
    top_bar: NowPlayingTopBar,
    content_padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val player: PlayerState = LocalPlayerState.current
    val density: Density = LocalDensity.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current

    val current_song: Song? by player.status.song_state

    BoxWithConstraints(modifier.clipToBounds()) {
        val top_padding: Dp = content_padding.calculateTopPadding()

        top_bar.DisplayTopBar(
            expansion = expansion,
            distance_to_page = 0.dp,
            container_modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = content_padding.calculateTopPadding())
        )

        Column(
            Modifier
                .fillMaxSize()
                .requiredHeight(maxHeight - top_bar.height)
                .padding(
                    content_padding.copy(
                        top = MINIMISED_NOW_PLAYING_V_PADDING_DP.dp
                    )
                )
                .offset {
                    IntOffset(
                        0,
                        if (top_bar.displaying)
                            with (density) {
                                (top_bar.height / 2).roundToPx()
                            }
                        else 0
                    )
                }
                .offset {
                    IntOffset(
                        0,
                        -(top_bar.height * (1f - expansion.get().coerceIn(0f..1f))).roundToPx()
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            composeScope {
                Spacer(Modifier.height(lerp(0.dp, top_padding, expansion.get().coerceIn(0f, 1f))))
            }

            val thumbnail_size: Dp = this@BoxWithConstraints.getThumbnailSize()
            val controls_height: Dp = this@BoxWithConstraints.maxHeight - thumbnail_size

            composeScope {
                SmallThumbnailRow(
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
            if (!controls_visible) {
                return@Column
            }

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
                val button_modifier: Modifier = Modifier.alpha(0.5f)
                val side_button_padding: Dp = 20.dp
                val show_shuffle_repeat_buttons: Boolean by player.settings.Player.SHOW_REPEAT_SHUFFLE_BUTTONS.observe()

                Controls(
                    current_song,
                    {
                        player.withPlayer {
                            if (duration_ms <= 0) {
                                return@withPlayer
                            }

                            seekTo((duration_ms * it).toLong())
                            seek_state = it
                        }
                    },
                    Modifier
                        .graphicsLayer {
                            alpha = 1f - (1f - expansion.getBounded()).absoluteValue
                        },
                    buttonRowStartContent = {
                        Box(
                            Modifier
                                .padding(10.dp)
                                .padding(end = side_button_padding)
                                .then(button_modifier)
                        ) {
                            NowPlayingMainTabActionButtons.LikeDislikeButton(current_song, button_modifier)
                        }
                    },
                    buttonRowEndContent = {
                        Box(
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            NowPlayingMainTabActionButtons.RadioButton(current_song, button_modifier.padding(start = side_button_padding).bounceOnClick())
                        }
                    },
                    artistRowStartContent = {
                        if (show_shuffle_repeat_buttons) {
                            RepeatButton({ player.getNPBackground() }, button_modifier)
                        }
                        else {
                            Spacer(Modifier.height(40.dp))
                        }
                    },
                    artistRowEndContent = {
                        if (show_shuffle_repeat_buttons) {
                            NowPlayingMainTabActionButtons.ShuffleButton(button_modifier)
                        }
                        else {
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                )

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val bottom_row_colour: Color = player.getNPOnBackground().copy(alpha = 0.5f)
                        var show_volume_slider: Boolean by remember { mutableStateOf(false) }

                        Row(
                            Modifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton({ show_volume_slider = !show_volume_slider }) {
                                Icon(Icons.Default.VolumeUp, null, tint = bottom_row_colour)
                            }

                            AnimatedVisibility(
                                show_volume_slider,
                                enter = expandHorizontally(expandFrom = Alignment.Start),
                                exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
                            ) {
                                VolumeSlider(bottom_row_colour, Modifier.fillMaxWidth().weight(1f))
                            }
                        }

                        IconButton({ player.expansion.scroll(1) }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = bottom_row_colour)
                        }

                        IconButton(
                            {
                                current_song?.let { song ->
                                    if (1f - expansion.getDisappearing() > 0f) {
                                        click_overrides.onMediaItemAltClicked(song, player.status.m_index, player)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.MoreHoriz, null, tint = bottom_row_colour)
                        }
                    }
                }
            }
        }
    }
}
