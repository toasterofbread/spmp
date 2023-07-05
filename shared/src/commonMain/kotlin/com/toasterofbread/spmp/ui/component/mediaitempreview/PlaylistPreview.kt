package com.toasterofbread.spmp.ui.component.mediaitempreview

import LocalPlayerState
import SpMp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.api.getOrReport
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.mediaitem.mediaItemPreviewInteraction
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.longpressmenu.longPressMenuIcon
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.WidthShrinkText
import com.toasterofbread.utils.isDebugBuild
import com.toasterofbread.utils.setAlpha
import kotlinx.coroutines.launch

@Composable
fun PlaylistPreviewSquare(
    playlist: Playlist,
    params: MediaItemPreviewParams
) {
    val long_press_menu_data = remember(playlist) {
        getPlaylistLongPressMenuData(playlist, multiselect_context = params.multiselect_context)
    }
    MediaItemPreviewSquare(playlist, params, long_press_menu_data)
}

@Composable
fun PlaylistPreviewLong(
    playlist: Playlist,
    params: MediaItemPreviewParams
) {
    val long_press_menu_data = remember(playlist) {
        getPlaylistLongPressMenuData(playlist, multiselect_context = params.multiselect_context)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier.mediaItemPreviewInteraction(playlist, long_press_menu_data)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            playlist.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(40.dp),
                contentColourProvider = params.contentColour
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
                color = params.contentColour?.invoke() ?: LocalContentColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                @Composable
                fun InfoText(text: String) {
                    Text(
                        text,
                        fontSize = 11.sp,
                        color = params.contentColour?.invoke() ?: LocalContentColor.current.setAlpha(0.5f),
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
        getString("lpm_long_press_actions"),
        multiselect_context = multiselect_context
    )
}

@Composable
fun LongPressMenuActionProvider.PlaylistLongPressMenuActions(playlist: MediaItem) {
    require(playlist is Playlist)

    val player = LocalPlayerState.current
    val coroutine_context = rememberCoroutineScope()

    ActionButton(
        Icons.Default.PlayArrow, getString("lpm_action_play"),
        onClick = {
            player.playMediaItem(playlist)
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

    if (playlist.is_editable == true) {
        ActionButton(Icons.Default.Delete, getString("playlist_delete"), onClick = { coroutine_context.launch {
            playlist.deletePlaylist().getOrReport("deletePlaylist")
        } })
    }
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
        Spacer(Modifier.height(25.dp))
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
