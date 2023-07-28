package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.observeAsState
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.getContrasted

@Composable
internal fun PlaylistTopInfo(
    playlist: Playlist,
    accent_colour: Color,
    editing_info: Boolean,
    setEditingInfo: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onThumbLoaded: (ImageBitmap) -> Unit,
) {
    val player = LocalPlayerState.current
    val density = LocalDensity.current
    val db = SpMp.context.database
    val shape = RoundedCornerShape(10.dp)
    val min_height = 120.dp

    var playlist_image_width: Float? by db.playlistQueries
        .imageWidthById(playlist.id)
        .observeAsState(
            { it.executeAsOneOrNull()?.image_width?.toFloat() },
            { db.playlistQueries.updateImageWidthById(it?.toDouble(), playlist.id) }
        )
    var playlist_title: String? by db.mediaItemQueries
        .titleById(playlist.id)
        .observeAsState(
            { it.executeAsOneOrNull()?.title },
            { db.mediaItemQueries.updateTitleById(it, playlist.id) }
        )

    var edited_title: String by remember { mutableStateOf("") }

    var split_position by remember(playlist) { mutableStateOf(playlist_image_width ?: 0f) }
    var width: Dp by remember(playlist) { mutableStateOf(0.dp) }
    var show_image by remember(playlist) { mutableStateOf(true) }

     LaunchedEffect(editing_info) {
        if (editing_info) {
            edited_title = playlist_title ?: ""
        }
    }

    Row(
        Modifier
            .height(IntrinsicSize.Max)
            .onSizeChanged {
                val width_dp = with(density) { it.width.toDp() }
                if (width == width_dp) {
                    return@onSizeChanged
                }
                width = width_dp

                if (split_position == 0f) {
                    split_position = min_height / width
                }
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var image_size by remember { mutableStateOf(IntSize.Zero) }

        // Playlist image
        AnimatedVisibility(show_image) {
            Box(
                Modifier
                    .heightIn(min = min_height)
                    .fillMaxWidth(split_position)
            ) {
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
                                if (!editing_info) {
                                    setEditingInfo(true)
                                    SpMp.context.vibrateShort()
                                }
                            }
                        ),
                    onLoaded = onThumbLoaded
                )
            }
        }

        // System insets spacing
        AnimatedVisibility(editing_info && !show_image) {
            SpMp.context.getSystemInsets()?.also { system_insets ->
                with(LocalDensity.current) {
                    Spacer(Modifier.width(system_insets.getLeft(this, LocalLayoutDirection.current).toDp()))
                }
            }
        }

        // Split position drag handle
        AnimatedVisibility(editing_info) {
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
                                    split_position = min_height / width
                                }
                            } else {
                                split_position = (split_position + (delta_dp / width)).coerceIn(0.1f, 0.9f)
                                if (split_position * width < min_height) {
                                    show_image = false
                                }
                            }

                            playlist_image_width = split_position
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, null)
            }
        }

        // Main info column
        Column(Modifier.height(with(LocalDensity.current) { image_size.height.toDp().coerceAtLeast(min_height) })) {

            // Title text
            Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                Crossfade(editing_info) { editing ->
                    if (!editing) {
                        Text(
                            playlist_title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.platformClickable(
                                onClick = {},
                                onAltClick = {
                                    setEditingInfo(true)
                                }
                            )
                        )
                    } else {
                        val colour = LocalContentColor.current
                        val line_padding = with(LocalDensity.current) { 5.dp.toPx() }
                        val line_width = with(LocalDensity.current) { 1.dp.toPx() }

                        BasicTextField(
                            edited_title,
                            {
                                edited_title = it.replace("\n", "")
                                playlist_title = edited_title.trim()
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
                { player.playMediaItem(playlist) },
                Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent_colour,
                    contentColor = accent_colour.getContrasted()
                ),
                shape = shape
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Text(getString("playlist_chip_play"))
            }
        }
    }
}
