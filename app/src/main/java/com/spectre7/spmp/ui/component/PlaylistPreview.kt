package com.spectre7.spmp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.ui.layout.PlayerViewContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistPreviewSquare(
    playlist: Playlist,
    content_colour: Color,
    player: PlayerViewContext,
    enable_long_press_menu: Boolean = true,
    modifier: Modifier = Modifier
) {
    val long_press_menu_data = remember(playlist) { LongPressMenuData(
        playlist,
        RoundedCornerShape(10),
        { } // TODO
    ) }

    Column(
        modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    player.onMediaItemClicked(playlist)
                },
                onLongClick = {
                    player.showLongPressMenu(long_press_menu_data)
                }
            )
            .aspectRatio(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        playlist.Thumbnail(MediaItem.ThumbnailQuality.LOW,
            Modifier
                .size(100.dp)
                .longPressMenuIcon(long_press_menu_data, enable_long_press_menu))

        Text(
            playlist.title,
            fontSize = 12.sp,
            color = content_colour,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistPreviewLong(
    playlist: Playlist, 
    content_colour: Color, 
    player: PlayerViewContext,
    enable_long_press_menu: Boolean = true,
    modifier: Modifier = Modifier
) {
    val long_press_menu_data = remember(playlist) { LongPressMenuData(
        playlist,
        RoundedCornerShape(10),
        { } // TODO
    ) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    player.onMediaItemClicked(playlist)
                },
                onLongClick = {
                    player.showLongPressMenu(long_press_menu_data)
                }
            )
    ) {
        playlist.Thumbnail(MediaItem.ThumbnailQuality.LOW, Modifier.size(40.dp).longPressMenuIcon(long_press_menu_data, enable_long_press_menu))

        Column(Modifier.padding(8.dp)) {
            Text(
                playlist.title,
                fontSize = 15.sp,
                color = content_colour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Text(
            //     "${playlist.getFormattedSubscriberCount()} subscribers",
            //     fontSize = font_size * 0.75,
            //     color = colour.setAlpha(0.5),
            //     maxLines = 1,
            //     overflow = TextOverflow.Ellipsis
            // )
        }
    }
}
