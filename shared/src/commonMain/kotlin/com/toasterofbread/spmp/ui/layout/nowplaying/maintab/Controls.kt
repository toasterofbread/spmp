package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.MediaItemTitleEditDialog
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.composable.Marquee
import com.toasterofbread.composekit.utils.modifier.bounceOnClick

private const val TITLE_FONT_SIZE_SP: Float = 21f
private const val ARTIST_FONT_SIZE_SP: Float = 12f

@Composable
internal fun Controls(
    song: Song?,
    seek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    button_row_arrangement: Arrangement.Horizontal = Arrangement.Center,
    disable_text_marquees: Boolean = false,
    vertical_arrangement: Arrangement.Vertical = Arrangement.spacedBy(20.dp),
    font_size_multiplier: Float = 1f,
    text_align: TextAlign = TextAlign.Center,
    buttonRowStartContent: @Composable RowScope.() -> Unit = {},
    buttonRowEndContent: @Composable RowScope.() -> Unit = {},
    artistRowStartContent: @Composable RowScope.() -> Unit = {},
    artistRowEndContent: @Composable RowScope.() -> Unit = {}
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
                .bounceOnClick()
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

    Column(modifier, verticalArrangement = vertical_arrangement) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Marquee(Modifier.fillMaxWidth(), disable = disable_text_marquees) {
                Text(
                    song_title ?: "",
                    fontSize = TITLE_FONT_SIZE_SP.sp * font_size_multiplier,
                    color = player.getNPOnBackground(),
                    textAlign = text_align,
                    maxLines = 1,
                    softWrap = false,
                    // Using ellipsis makes this go weird, no clue why
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                artistRowStartContent()
                Spacer(Modifier)
                Text(
                    song_artist_title ?: "",
                    fontSize = ARTIST_FONT_SIZE_SP.sp * font_size_multiplier,
                    color = player.getNPOnBackground(),
                    textAlign = text_align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
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
                Spacer(Modifier)
                artistRowEndContent()
            }
        }

        SeekBar(Modifier.fillMaxWidth(), seek = seek)

        Row(
            horizontalArrangement = button_row_arrangement,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            buttonRowStartContent()

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

            buttonRowEndContent()
        }
    }
}
