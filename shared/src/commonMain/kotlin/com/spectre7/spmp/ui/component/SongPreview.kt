package com.spectre7.spmp.ui.component

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.PlaylistSelectMenu
import com.spectre7.utils.composable.WidthShrinkText
import com.spectre7.utils.getContrasted
import com.spectre7.utils.isDebugBuild
import com.spectre7.utils.launchSingle

val SONG_THUMB_CORNER_ROUNDING = 10.dp

@Composable
fun SongPreviewSquare(
    song: Song,
    params: MediaItemPreviewParams,
    queue_index: Int? = null
) {
    val long_press_menu_data = remember(song, params.multiselect_context) {
        getSongLongPressMenuData(
            song,
            multiselect_key = queue_index,
            multiselect_context = params.multiselect_context
        )
    }

    Column(
        params.modifier.mediaItemPreviewInteraction(song, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            song.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(song, Modifier.fillMaxSize(), key = queue_index)
            }
        }

        Text(
            song.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongPreviewLong(
    song: Song,
    params: MediaItemPreviewParams,
    queue_index: Int? = null
) {
    val long_press_menu_data = remember(song, queue_index) {
        getSongLongPressMenuData(
            song,
            multiselect_key = queue_index,
            multiselect_context = params.multiselect_context
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier
            .fillMaxWidth()
            .mediaItemPreviewInteraction(song, long_press_menu_data)
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            song.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(40.dp),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(song, Modifier.fillMaxSize(), key = queue_index)
            }
        }

        Column(
            Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                song.title ?: "",
                fontSize = 15.sp,
                color = params.contentColour?.invoke() ?: Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (params.show_type) {
                    InfoText(song.type.getReadable(false), params)
                }

                if (song.artist?.title != null) {
                    if (params.show_type) {
                        InfoText("\u2022", params)
                    }
                    InfoText(song.artist?.title!!, params)
                }
            }
        }
    }
}

@Composable
private fun InfoText(text: String, params: MediaItemPreviewParams) {
    Text(
        text,
        Modifier.alpha(0.5f),
        fontSize = 11.sp,
        color = params.contentColour?.invoke() ?: Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

fun getSongLongPressMenuData(
    song: Song,
    thumb_shape: Shape? = RoundedCornerShape(SONG_THUMB_CORNER_ROUNDING),
    multiselect_key: Int? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
): LongPressMenuData {
    return LongPressMenuData(
        song,
        thumb_shape,
        { SongLongPressMenuInfo(song, multiselect_key, it) },
        getString("lpm_long_press_actions"),
        multiselect_context = multiselect_context,
        multiselect_key = multiselect_key,
        sideButton = { modifier, background, _ ->
            LikeDislikeButton(song, modifier) { background.getContrasted() }
        }
    ) { item, spacing ->
        SongLongPressPopupActions(item, spacing, multiselect_key)
    }
}

@Composable
private fun LongPressMenuActionProvider.SongLongPressPopupActions(song: MediaItem, spacing: Dp, queue_index: Int?) {
    require(song is Song)

    val density = LocalDensity.current
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    var height: Dp? by remember { mutableStateOf(null) }
    var adding_to_playlist by remember { mutableStateOf(false) }

    Crossfade(adding_to_playlist) { playlist_interface ->
        if (!playlist_interface) {
            Column(
                Modifier.onSizeChanged { height = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                LPMActions(song, queue_index) { adding_to_playlist = true }
            }
        }
        else {
            Column(
                Modifier
                    .border(1.dp, LocalContentColor.current, RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .then(
                        height?.let { Modifier.height(it) } ?: Modifier
                    )
            ) {
                val selected_playlists = remember { mutableStateListOf<Playlist>() }
                PlaylistSelectMenu(selected_playlists)
                
                val button_colours = IconButtonDefaults.iconButtonColors(
                    containerColor = Theme.current.accent,
                    contentColor = Theme.current.on_accent
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapedIconButton({ adding_to_playlist = false }, colors = button_colours) {
                        Icon(Icons.Default.Close, null)
                    }

                    Spacer(Modifier.fillMaxWidth().weight(1f))

                    ShapedIconButton(
                        {
                            coroutine_scope.launch {
                                val playlist = LocalPlaylist.createLocalPlaylist(SpMp.context)
                                selected_playlists.add(playlist)
                            }
                        },
                        colors = button_colours
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                    ShapedIconButton(
                        {
                            if (selected_playlists.isNotEmpty()) {
                                coroutine_scope.launch {
                                    for (playlist in selected_playlists) {
                                        playlist.addItem(song)
                                        playlist.saveItems()
                                    }
                                    SpMp.context.sendToast(getString("toast_playlist_added"))
                                }

                                onAction()

                                if (selected_playlists.size == 1) {
                                    player.openMediaItem(selected_playlists.first())
                                }
                            }

                            adding_to_playlist = false
                        },
                        colors = button_colours
                    ) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun LongPressMenuActionProvider.LPMActions(song: Song, queue_index: Int?, openPlaylistInterface: () -> Unit) {
    val player = LocalPlayerState.current

    ActionButton(
        Icons.Default.Radio, getString("lpm_action_radio"),
        onClick = {
            player.player.playSong(song)
        },
        onLongClick = if (queue_index == null) null else {{
            player.player.startRadioAtIndex(queue_index + 1, song, skip_first = true)
        }}
    )

    ActiveQueueIndexAction(
        { distance ->
            getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString())
        },
        onClick = { active_queue_index ->
            player.player.addToQueue(
                song,
                active_queue_index + 1,
                is_active_queue = Settings.KEY_LPM_INCREMENT_PLAY_AFTER.get(),
                start_radio = false
            )
        },
        onLongClick = { active_queue_index ->
            player.player.addToQueue(
                song,
                active_queue_index + 1,
                is_active_queue = Settings.KEY_LPM_INCREMENT_PLAY_AFTER.get(),
                start_radio = true
            )
        }
    )

    ActionButton(Icons.Default.PlaylistAdd, getString("song_add_to_playlist"), onClick = openPlaylistInterface, onAction = {})

    ActionButton(Icons.Default.Download, getString("lpm_action_download"), onClick = {
        player.download_manager.startDownload(song.id) { status: DownloadStatus ->
            when (status.status) {
                DownloadStatus.Status.FINISHED -> SpMp.context.sendToast(getString("notif_download_finished"))
                DownloadStatus.Status.ALREADY_FINISHED -> SpMp.context.sendToast(getString("notif_download_already_finished"))
                DownloadStatus.Status.CANCELLED -> SpMp.context.sendToast(getString("notif_download_cancelled"))

                // IDLE, DOWNLOADING, PAUSED
                else -> {
                    SpMp.context.sendToast(getString("notif_download_already_downloading"))
                }
            }
        }
    })

    if (song.artist != null) {
        ActionButton(Icons.Default.Person, getString("lpm_action_go_to_artist"), onClick = {
            player.openMediaItem(song.artist!!)
        })
    }
}

@Composable
private fun ColumnScope.SongLongPressMenuInfo(song: Song, queue_index: Int?, accent_colour: Color) {
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

    if (queue_index != null) {
        Item(Icons.Default.Radio, getString("lpm_action_radio_at_song_pos"))
    }
    else {
        Item()
    }

    val player = LocalPlayerState.current
    if (player.player.active_queue_index < player.status.m_song_count) {
        Item(Icons.Default.SubdirectoryArrowRight, getString("lpm_action_radio_after_x_songs"))
    }
    else {
        Item()
    }

    Spacer(Modifier.fillMaxHeight().weight(1f))

    Row(Modifier.requiredHeight(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            getString("lpm_info_id").replace("\$id", song.id),
            Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        SpMp.context.CopyShareButtons { song.id }
    }

    if (queue_index != null) {
        Text(getString("lpm_info_queue_index").replace("\$index", queue_index.toString()))
    }

    if (isDebugBuild()) {
        Item(Icons.Default.Print, getString("lpm_action_print_info"), Modifier.clickable {
            println(song)
        })
    }
}
