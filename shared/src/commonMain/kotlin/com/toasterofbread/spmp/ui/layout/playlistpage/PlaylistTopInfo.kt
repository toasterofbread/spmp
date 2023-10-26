package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.toastercomposetools.platform.composable.platformClickable
import com.toasterofbread.toastercomposetools.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.toastercomposetools.utils.common.getContrasted

private const val PLAYLIST_IMAGE_MIN_HEIGHT_DP: Float = 120f

@Composable
internal fun PlaylistPage.PlaylistTopInfo(items: List<Pair<MediaItem, Int>>?, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current
    val db = player.database

    val shape = RoundedCornerShape(10.dp)

    val playlist_title: String? by playlist.observeActiveTitle()
    val playlist_image_width: Float? by playlist.ImageWidth.observe(db)

    var split_position by remember(playlist) { mutableStateOf(playlist_image_width ?: 0f) }
    var width: Dp by remember(playlist) { mutableStateOf(0.dp) }

    var show_image by remember(playlist) { mutableStateOf(true) }
    LaunchedEffect(split_position, width) {
        show_image = split_position * width >= PLAYLIST_IMAGE_MIN_HEIGHT_DP.dp
    }
    
    Row(
        modifier
            .height(IntrinsicSize.Max)
            .onSizeChanged {
                val width_dp = with(density) { it.width.toDp() }
                if (width == width_dp) {
                    return@onSizeChanged
                }
                width = width_dp

                if (split_position == 0f) {
                    split_position = PLAYLIST_IMAGE_MIN_HEIGHT_DP.dp / width
                }
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var image_size by remember { mutableStateOf(IntSize.Zero) }

        // Playlist image
        AnimatedVisibility(show_image) {
            Box(
                Modifier
                    .heightIn(min = PLAYLIST_IMAGE_MIN_HEIGHT_DP.dp)
                    .fillMaxWidth(split_position)
            ) {
                val provider_override =
                    if (edit_in_progress)
                        edited_image_url?.let { image_url ->
                            MediaItemThumbnailProvider.fromImageUrl(image_url)
                        } ?: playlist.ThumbnailProvider.get(player.database)
                    else null

                playlist.Thumbnail(
                    MediaItemThumbnailProvider.Quality.HIGH,
                    Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .clip(shape)
                        .onSizeChanged {
                            image_size = it
                        }
                        .platformClickable(
                            onClick = {},
                            onAltClick = {
                                if (!edit_in_progress) {
                                    beginEdit()
                                    player.context.vibrateShort()
                                }
                            }
                        ),
                    onLoaded = ::onThumbnailLoaded,
                    provider_override = provider_override
                )
            }
        }

        // System insets spacing
        AnimatedVisibility(edit_in_progress && !show_image) {
            player.context.getSystemInsets()?.also { system_insets ->
                with(LocalDensity.current) {
                    Spacer(Modifier.width(system_insets.getLeft(this, LocalLayoutDirection.current).toDp()))
                }
            }
        }

        // Split position drag handle
        AnimatedVisibility(edit_in_progress) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .border(Dp.Hairline, LocalContentColor.current, shape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val delta_dp = with(density) { delta.toDp() }
                            if (!show_image) {
                                if (delta_dp > 0.dp) {
                                    show_image = true
                                    split_position = PLAYLIST_IMAGE_MIN_HEIGHT_DP.dp / width
                                }
                            }
                            else {
                                split_position = (split_position + (delta_dp / width)).coerceIn(0.1f, 0.9f)
                            }

                            if (split_position * width < PLAYLIST_IMAGE_MIN_HEIGHT_DP.dp) {
                                edited_image_width = -1f
                            }
                            else {
                                edited_image_width = split_position
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, null)
            }
        }

        // Main info column
        Column(Modifier.height(with(LocalDensity.current) { image_size.height.toDp().coerceAtLeast(PLAYLIST_IMAGE_MIN_HEIGHT_DP.dp) })) {

            // Title text
            Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                Crossfade(edit_in_progress) { editing ->
                    if (!editing) {
                        Text(
                            playlist_title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.platformClickable(
                                onClick = {},
                                onAltClick = {
                                    beginEdit()
                                }
                            )
                        )
                    }
                    else {
                        val colour = LocalContentColor.current
                        val line_padding = with(LocalDensity.current) { 5.dp.toPx() }
                        val line_width = with(LocalDensity.current) { 1.dp.toPx() }

                        BasicTextField(
                            edited_title,
                            {
                                edited_title = it.replace("\n", "")
                            },
                            Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawLine(
                                        colour,
                                        center + Offset(-size.width / 2f, (size.height / 2f) + line_padding),
                                        center + Offset(size.width / 2f, (size.height / 2f) + line_padding),
                                        strokeWidth = line_width
                                    )
                                },
                            textStyle = LocalTextStyle.current.copy(color = colour),
                            cursorBrush = SolidColor(colour)
                        )
                    }
                }
            }

            // Play button
            Button(
                {
                    if (!items.isNullOrEmpty()) {
                        player.playMediaItem(playlist)
                    }
                },
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = getAccentColour(),
                    contentColor = getAccentColour().getContrasted()
                ),
                shape = shape
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Text(getString("playlist_chip_play"))
            }
        }
    }
}
