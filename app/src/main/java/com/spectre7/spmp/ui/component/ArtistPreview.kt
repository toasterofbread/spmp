@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)

package com.spectre7.spmp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import com.spectre7.utils.setAlpha

@Composable
fun ArtistPreviewSquare(
    artist: Artist, 
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
                    player.onMediaItemCLicked(artist)
                },
                onLongClick = {
                    if (enable_long_press_menu) {
                        show_popup = true
                    }
                    else {
                        player.onMediaItemLongClicked(artist)
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
            media_item = artist,
            player = player,
            _thumb_size = 100.dp,
            thumb_shape = CircleShape,
            actions = longPressPopupActions
        )

        Text(
            artist.name,
            fontSize = 12.sp,
            color = content_colour,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistPreviewLong(
    artist: Artist, 
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
                    player.onMediaItemCLicked(artist)
                },
                onLongClick = {
                    if (enable_long_press_menu) {
                        show_popup = true
                    }
                    else {
                        player.onMediaItemLongClicked(artist)
                    }
                }
            )
    ) {
        LongPressIconMenu(
            showing = show_popup,
            onDismissRequest = {
                show_popup = false
            },
            media_item = artist,
            player = player,
            _thumb_size = 40.dp,
            thumb_shape = CircleShape,
            actions = longPressPopupActions
        )

        Column(Modifier.padding(8.dp)) {
            Text(
                artist.name,
                fontSize = 15.sp,
                color = content_colour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${artist.getFormattedSubscriberCount()} subscribers",
                fontSize = 12.sp,
                color = content_colour.setAlpha(0.5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

val longPressPopupActions: @Composable LongPressMenuActionProvider.(MediaItem) -> Unit = { artist ->
    if (artist !is Artist) {
        throw IllegalStateException()
    }

    ActionButton(Icons.Filled.PlayArrow, "Start radio") {
        // TODO
    }

    val queue_song = remember (PlayerServiceHost.service.active_queue_index) { PlayerServiceHost.service.getSong(PlayerServiceHost.service.active_queue_index) }
    if (queue_song != null) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(Icons.Filled.SubdirectoryArrowRight, "Start radio after",
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)) {
                    // TODO
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val button_padding = PaddingValues(0.dp)
                    val button_modifier = Modifier
                        .size(30.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .align(Alignment.CenterVertically)
                    val button_colours = ButtonDefaults.buttonColors(
                        containerColor = accent_colour,
                        contentColor = background_colour
                    )

                    ElevatedButton(
                        {
                            PlayerServiceHost.service.updateActiveQueueIndex(-1)
                        },
                        button_modifier,
                        contentPadding = button_padding,
                        colors = button_colours,
                    ) {
                        Icon(Icons.Filled.Remove, null)
                    }
                    ElevatedButton(
                        {
                            PlayerServiceHost.service.updateActiveQueueIndex(1)
                        },
                        button_modifier,
                        contentPadding = button_padding,
                        colors = button_colours
                    ) {
                        Icon(Icons.Filled.Add, null)
                    }
                }
            }

            Crossfade(queue_song, animationSpec = tween(100)) {
                it.PreviewLong(
                    content_colour,
                    null, null,
                    Modifier
                )
            }
        }
    }

    ActionButton(Icons.Filled.Person, "View artist") {
        player.openMediaItem(song.artist)
    }
}
