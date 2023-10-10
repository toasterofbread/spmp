package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.MediaItemTitleEditDialog
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.utils.common.getValue
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.Marquee

@Composable
internal fun Controls(
    song: Song?,
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    val song_title: String? by song?.observeActiveTitle()
    val song_artist_title: String? by song?.Artist?.observePropertyActiveTitle()

    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        show_title_edit_dialog = false
    }

    if (show_title_edit_dialog && song != null) {
        MediaItemTitleEditDialog(song) { show_title_edit_dialog = false }
    }

    Spacer(Modifier.requiredHeight(30.dp))

    Box(
        modifier,
        contentAlignment = Alignment.TopCenter
    ) {

        @Composable
        fun PlayerButton(
            image: ImageVector,
            size: Dp = 60.dp,
            enabled: Boolean = true,
            onClick: () -> Unit
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(
                        onClick = onClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        enabled = enabled
                    )
                    .alpha(if (enabled) 1.0f else 0.5f)
            ) {
                val painter = rememberVectorPainter(image)

                Canvas(
                    Modifier
                        .requiredSize(size)
                        // https://stackoverflow.com/a/67820996
                        .graphicsLayer { alpha = 0.99f }
                ) {
                    with(painter) {
                        draw(this@Canvas.size)
                    }

                    val gradient_end = this@Canvas.size.width * 1.7f
                    drawRect(
                        Brush.linearGradient(
                            listOf(player.getNPOnBackground(), player.getNPBackground()),
                            end = Offset(gradient_end, gradient_end)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(25.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

                Marquee(Modifier.fillMaxWidth()) {
                    Text(
                        song_title ?: "",
                        fontSize = 20.sp,
                        color = player.getNPOnBackground(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        // TODO Using ellipsis makes this go weird, no clue why
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .platformClickable(
                                onAltClick = {
                                    show_title_edit_dialog = !show_title_edit_dialog
                                    player.context.vibrateShort()
                                }
                            )
                    )
                }

                Text(
                    song_artist_title ?: "",
                    fontSize = 12.sp,
                    color = player.getNPOnBackground(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .platformClickable(
                            onClick = {
                                val artist: Artist? = song?.Artist?.get(player.database)
                                if (artist?.isForItem() == false) {
                                    player.onMediaItemClicked(artist)
                                }
                            },
                            onAltClick = {
                                val artist: Artist? = song?.Artist?.get(player.database)
                                if (artist?.isForItem() == false) {
                                    player.onMediaItemLongClicked(artist)
                                    player.context.vibrateShort()
                                }
                            }
                        )
                )
            }

            SeekBar(seek)

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Previous
                PlayerButton(
                    Icons.Rounded.SkipPrevious,
                    enabled = player.status.m_has_previous,
                    size = 60.dp
                ) {
                    player.controller?.seekToPrevious()
                }

                // Play / pause
                PlayerButton(
                    if (player.status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    enabled = song != null,
                    size = 75.dp
                ) {
                    player.controller?.playPause()
                }

                // Next
                PlayerButton(
                    Icons.Rounded.SkipNext,
                    enabled = player.status.m_has_next,
                    size = 60.dp
                ) {
                    player.controller?.seekToNext()
                }
            }

            Spacer(Modifier.fillMaxHeight().weight(1f))

            val bottom_row_colour = player.getNPOnBackground().setAlpha(0.5f)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var volume_slider_visible by remember { mutableStateOf(false) }
                Row(
                    Modifier
                        .weight(1f, false)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        { volume_slider_visible = !volume_slider_visible }
                    ) {
                        Icon(Icons.Filled.VolumeUp, null, tint = bottom_row_colour)
                    }

                    AnimatedVisibility(volume_slider_visible) {
                        VolumeSlider(bottom_row_colour)
                    }
                }

                AnimatedVisibility(!volume_slider_visible, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                    Spacer(Modifier.width(48.dp))
                }
            }
        }
    }
}

@Composable
private fun VolumeSlider(colour: Color, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    SliderValueHorizontal(
        value = player.status.m_volume,
        onValueChange = {
            player.controller?.volume = it
        },
        thumbSizeInDp = DpSize(12.dp, 12.dp),
        track = { a, b, c, d, e -> DefaultTrack(a, b, c, d, e, colour.setAlpha(0.5f), colour) },
        thumb = { a, b, c, d, e -> DefaultThumb(a, b, c, d, e, colour, 1f) },
        modifier = modifier
    )
}
