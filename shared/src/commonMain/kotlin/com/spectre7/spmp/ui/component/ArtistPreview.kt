@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)

package com.spectre7.spmp.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.ArtistSubscribeButton
import com.spectre7.utils.getContrasted
import com.spectre7.utils.setAlpha

@Composable
fun ArtistPreviewSquare(
    artist: Artist,
    params: MediaItem.PreviewParams,
    thumb_size: DpSize = DpSize(100.dp, 100.dp)
) {
    val long_press_menu_data = remember(artist) {
        getArtistLongPressMenuData(artist)
    }

    Column(
        params.modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    params
                        .playerProvider()
                        .onMediaItemClicked(artist)
                },
                onLongClick = {
                    params
                        .playerProvider()
                        .showLongPressMenu(long_press_menu_data)
                }
            )
            .aspectRatio(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        artist.Thumbnail(MediaItem.ThumbnailQuality.LOW,
            Modifier
                .size(thumb_size)
                .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu))

        Text(
            artist.title ?: "",
            fontSize = 12.sp,
            color = params.content_colour(),
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistPreviewLong(
    artist: Artist,
    params: MediaItem.PreviewParams
) {
    val long_press_menu_data = remember(artist) {
        getArtistLongPressMenuData(artist)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    params
                        .playerProvider()
                        .onMediaItemClicked(artist)
                },
                onLongClick = {
                    params
                        .playerProvider()
                        .showLongPressMenu(long_press_menu_data)
                }
            )
    ) {
        artist.Thumbnail(MediaItem.ThumbnailQuality.LOW,
            Modifier
                .size(40.dp)
                .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu))

        Column(Modifier.padding(8.dp)) {
            Text(
                artist.title ?: "",
                fontSize = 15.sp,
                color = params.content_colour(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${artist.getFormattedSubscriberCount()} subscribers",
                fontSize = 12.sp,
                color = params.content_colour().setAlpha(0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getArtistLongPressMenuData(
    artist: Artist,
    thumb_shape: Shape? = CircleShape
): LongPressMenuData {
    return LongPressMenuData(
        artist,
        thumb_shape,
        actions = {
            ArtistLongPressPopupActions(it)
        },
        sideButton = { modifier, background, accent ->
            ArtistSubscribeButton(
                artist,
                background_colour = background.getContrasted(),
                accent_colour = accent,
                modifier = modifier
            )
        }
    )
}

@Composable
private fun LongPressMenuActionProvider.ArtistLongPressPopupActions(artist: MediaItem) {
    require(artist is Artist)

    ActionButton(Icons.Filled.PlayArrow, "Start radio", onClick = {
        TODO()
    })

    val queue_song = remember (PlayerServiceHost.player.active_queue_index) { PlayerServiceHost.player.getSong(PlayerServiceHost.player.active_queue_index) }
    if (queue_song != null) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(Icons.Filled.SubdirectoryArrowRight, "Start radio after",
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = {
                        TODO()
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val button_padding = PaddingValues(0.dp)
                    val button_modifier = Modifier
                        .size(30.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .align(Alignment.CenterVertically)
                    val button_colours = ButtonDefaults.buttonColors(
                        containerColor = accent_colour(),
                        contentColor = background_colour()
                    )

                    ElevatedButton(
                        {
                            PlayerServiceHost.player.updateActiveQueueIndex(-1)
                        },
                        button_modifier,
                        contentPadding = button_padding,
                        colors = button_colours,
                    ) {
                        Icon(Icons.Filled.Remove, null)
                    }
                    ElevatedButton(
                        {
                            PlayerServiceHost.player.updateActiveQueueIndex(1)
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
                it.PreviewLong(MediaItem.PreviewParams(playerProvider, content_colour = content_colour))
            }
        }
    }

    ActionButton(Icons.Filled.Person, "View artist", onClick = {
        playerProvider().openMediaItem(artist)
    })
}
