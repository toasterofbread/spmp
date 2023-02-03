@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.spectre7.spmp.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.utils.setAlpha

@Composable
fun SongPreviewSquare(
    song: Song, 
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
            media_item = song,
            player = player,
            _thumb_size = 100.dp,
            thumb_shape = RoundedCornerShape(10),
            actions = longPressPopupActions
        )

        Text(
            song.title,
            fontSize = 12.sp,
            color = content_colour,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongPreviewLong(
    song: Song, 
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
        if (!song.loaded) {
            CircularProgressIndicator(color = content_colour)
        }
        else {
            LongPressIconMenu(
                showing = show_popup,
                onDismissRequest = {
                    show_popup = false
                },
                media_item = song,
                player = player,
                _thumb_size = 40.dp,
                thumb_shape = RoundedCornerShape(20),
                actions = longPressPopupActions
            )

            Column(
                Modifier
                    .padding(10.dp)
                    .fillMaxWidth(0.9f)) {
                Text(
                    song.title,
                    fontSize = 15.sp,
                    color = content_colour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    song.artist.name,
                    fontSize = 11.sp,
                    color = content_colour.setAlpha(0.5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

val longPressPopupActions: @Composable LongPressMenuActionProvider.(MediaItem) -> Unit = { song ->
    if (song !is Song) {
        throw IllegalStateException()
    }

    ActionButton(Icons.Filled.Radio, "Start radio") {
        PlayerServiceHost.service.playSong(song)
    }

    val queue_song = remember (PlayerServiceHost.service.active_queue_index) { PlayerServiceHost.service.getSong(PlayerServiceHost.service.active_queue_index) }
    if (queue_song != null) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(Icons.Filled.SubdirectoryArrowRight, "Play after",
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)) {
                    PlayerServiceHost.service.addToQueue(song, PlayerServiceHost.service.active_queue_index + 1, true, TODO("Start radio on long press"))
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

    ActionButton(Icons.Filled.Download, "Download") { TODO() }

    ActionButton(Icons.Filled.Person, "Go to artist") {
        player.openMediaItem(song.artist)
    }
}
