package com.spectre7.spmp.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.spectre7.spmp.model.*
import com.spectre7.utils.setAlpha

@Composable
fun PlaylistPreviewSquare(
    playlist: Playlist,
    params: MediaItem.PreviewParams
) {
    val long_press_menu_data = remember(playlist) {
        getPlaylistLongPressMenuData(playlist, multiselect_context = params.multiselect_context)
    }

    Column(
        params.modifier.mediaItemPreviewInteraction(playlist, params.playerProvider, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            playlist.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(playlist, Modifier.fillMaxSize())
            }
        }

        Text(
            playlist.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
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
    params: MediaItem.PreviewParams
) {
    val long_press_menu_data = remember(playlist) {
        getPlaylistLongPressMenuData(playlist, multiselect_context = params.multiselect_context)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier.mediaItemPreviewInteraction(playlist, params.playerProvider, long_press_menu_data)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            playlist.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(40.dp),
                params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(playlist, Modifier.fillMaxSize())
            }
        }

        Column(
            Modifier.padding(10.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                playlist.title ?: "",
                fontSize = 15.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                @Composable
                fun InfoText(text: String) {
                    Text(
                        text,
                        fontSize = 11.sp,
                        color = params.contentColour?.invoke() ?: Color.Unspecified.setAlpha(0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (params.show_type) {
                    InfoText(playlist.playlist_type.getReadable(false))
                }

                if (playlist.artist?.title != null) {
                    if (params.show_type) {
                        InfoText("\u2022")
                    }
                    InfoText(playlist.artist?.title!!)
                }
            }
        }
    }
}

fun getPlaylistLongPressMenuData(
    playlist: Playlist,
    thumb_shape: Shape? = RoundedCornerShape(10.dp),
    multiselect_context: MediaItemMultiSelectContext? = null
): LongPressMenuData {
    return LongPressMenuData(
        playlist,
        thumb_shape,
        { PlaylistLongPressMenuInfo(playlist, it) },
        multiselect_context = multiselect_context
    ) {
        PlaylistLongPressPopupActions(it)
    }
}

@Composable
private fun LongPressMenuActionProvider.PlaylistLongPressPopupActions(playlist: MediaItem) {
    require(playlist is Playlist)

    ActionButton(
        Icons.Default.PlayArrow, getString("lpm_action_play"),
        onClick = {
            TODO() // Play
        }
    )

    ActionButton(
        Icons.Default.Shuffle, getString("lpm_action_shuffle_playlist"),
        onClick = {
            TODO() // Shuffle
        }
    )

    ActiveQueueIndexAction(
        { distance ->
            getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString()) 
        },
        onClick = { active_queue_index ->
            TODO() // Insert at position
        },
        onLongClick = { active_queue_index ->
            TODO() // Insert shuffled at position
        }
    )

    ActionButton(Icons.Default.QueueMusic, getString("lpm_action_open_playlist"), onClick = {
        playerProvider().openMediaItem(playlist)
    })
}

@Composable
private fun ColumnScope.PlaylistLongPressMenuInfo(playlist: Playlist, accent_colour: Color) {
    @Composable
    fun Item(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent_colour)
            WidthShrinkText(text, fontSize = 15.sp)
        }
    }
    @Composable
    fun Item() {
        Spacer(Modifier.height(60.dp)) // TODO
    }

    Item() // Play
    Item() // Shuffle

    Item(Icons.Default.SubdirectoryArrowRight, getString("lpm_action_play_shuffled_after_x_songs"))

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )

    Row(Modifier.requiredHeight(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            getString("lpm_info_id").replace("\$id", playlist.id),
            Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        SpMp.context.CopyShareButtons { playlist.id }
    }

    if (isDebugBuild()) {
        Item(Icons.Default.Print, getString("lpm_action_print_info"), Modifier.clickable {
            println(playlist)
        })
    }
}
