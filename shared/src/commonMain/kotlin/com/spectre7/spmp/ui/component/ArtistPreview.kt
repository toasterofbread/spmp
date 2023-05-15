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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.mediaItemPreviewInteraction
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.ArtistSubscribeButton

const val ARTIST_THUMB_CORNER_ROUNDING = 50

@Composable
fun ArtistPreviewSquare(
    artist: Artist,
    params: MediaItem.PreviewParams
) {
    val long_press_menu_data = remember(artist) {
        getArtistLongPressMenuData(artist, multiselect_context = params.multiselect_context)
    }

    Column(
        params.modifier.mediaItemPreviewInteraction(artist, params.playerProvider, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            artist.Thumbnail(
                MediaItem.ThumbnailQuality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(artist, Modifier.fillMaxSize())
            }
        }

        Text(
            artist.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
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
        getArtistLongPressMenuData(artist, multiselect_context = params.multiselect_context)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier.mediaItemPreviewInteraction(artist, params.playerProvider, long_press_menu_data)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            artist.Thumbnail(
                MediaItem.ThumbnailQuality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(40.dp)
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(artist, Modifier.fillMaxSize())
            }
        }

        Column(Modifier.padding(8.dp)) {
            Text(
                artist.title ?: "",
                fontSize = 15.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                artist.getReadableSubscriberCount(),
                Modifier.alpha(0.5f),
                fontSize = 12.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getArtistLongPressMenuData(
    artist: Artist,
    thumb_shape: Shape? = RoundedCornerShape(ARTIST_THUMB_CORNER_ROUNDING),
    multiselect_context: MediaItemMultiSelectContext? = null
): LongPressMenuData {
    return LongPressMenuData(
        artist,
        thumb_shape,
        multiselect_context = multiselect_context,
        sideButton = { modifier, background, accent ->
            ArtistSubscribeButton(
                artist,
//                { background.getContrasted() },
//                { accent },
                modifier = modifier
            )
        }
    ) {
        ArtistLongPressPopupActions(it)
    }
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
                it.PreviewLong(MediaItem.PreviewParams(playerProvider, contentColour = content_colour))
            }
        }
    }

    ActionButton(Icons.Filled.Person, "View artist", onClick = {
        playerProvider().openMediaItem(artist)
    })
}
