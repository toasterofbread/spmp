package com.spectre7.spmp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var show_popup by remember { mutableStateOf(false) }

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
                    if (enable_long_press_menu) {
                        show_popup = true
                    }
                    else {
                        player.onMediaItemLongClicked(playlist)
                    }
                }
            )
            .aspectRatio(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = playlist,
            player = player,
            _thumb_size = 100.dp,
            thumb_shape = RoundedCornerShape(10),
            actions = { TODO() }
        )

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
    var show_popup by remember { mutableStateOf(false) }

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
                    if (enable_long_press_menu) {
                        show_popup = true
                    }
                    else {
                        player.onMediaItemLongClicked(playlist)
                    }
                }
            )
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = playlist,
            player = player,
            _thumb_size = 40.dp,
            thumb_shape = RoundedCornerShape(10),
            actions = { TODO() }
        )

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
