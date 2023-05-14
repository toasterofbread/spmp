@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.spectre7.spmp.ui.component

import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.platform.composable.platformClickable
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.utils.composable.WidthShrinkText
import com.spectre7.utils.getContrasted
import com.spectre7.utils.isDebugBuild

val SONG_THUMB_CORNER_ROUNDING = 10.dp

@Composable
fun SongPreviewSquare(
    song: Song, 
    params: MediaItem.PreviewParams,
    queue_index: Int? = null
) {
    val long_press_menu_data = remember(song, params.multiselect_context) {
        getSongLongPressMenuData(
            song,
            RoundedCornerShape(SONG_THUMB_CORNER_ROUNDING),
            queue_index = queue_index,
            multiselect_context = params.multiselect_context
        )
    }

    Column(
        params.modifier
            .platformClickable(
                onClick = {
                    if (params.multiselect_context?.is_active == true) {
                        params.multiselect_context.toggleItem(song)
                    }
                    else {
                        params.playerProvider().onMediaItemClicked(song)
                    }
                },
                onAltClick = {
                    params.playerProvider().showLongPressMenu(long_press_menu_data)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            song.Thumbnail(
                MediaItem.ThumbnailQuality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                params.contentColour
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
    params: MediaItem.PreviewParams,
    queue_index: Int? = null
) {
    val long_press_menu_data = remember(song, queue_index) {
        getSongLongPressMenuData(
            song,
            RoundedCornerShape(SONG_THUMB_CORNER_ROUNDING),
            queue_index = queue_index,
            multiselect_context = params.multiselect_context
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (params.multiselect_context?.is_active == true) {
                        params.multiselect_context.toggleItem(song)
                    }
                    else {
                        params.playerProvider().onMediaItemClicked(song)
                    }
                },
                onLongClick = {
                    params.playerProvider().showLongPressMenu(long_press_menu_data)
                }
            )
    ) {
        Box(Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min), contentAlignment = Alignment.Center) {
            song.Thumbnail(
                MediaItem.ThumbnailQuality.LOW,
                Modifier
                    .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                    .size(40.dp),
                params.contentColour
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
                @Composable
                fun InfoText(text: String) {
                    Text(
                        text,
                        Modifier.alpha(0.5f),
                        fontSize = 11.sp,
                        color = params.contentColour?.invoke() ?: Color.Unspecified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (params.show_type) {
                    InfoText(song.type.getReadable(false))
                }

                if (song.artist?.title != null) {
                    if (params.show_type) {
                        InfoText("\u2022")
                    }
                    InfoText(song.artist?.title!!)
                }
            }
        }
    }
}

fun getSongLongPressMenuData(
    song: Song,
    thumb_shape: Shape? = null,
    queue_index: Int? = null,
    multiselect_context: MediaItemMultiSelectContext? = null
): LongPressMenuData {
    return LongPressMenuData(
        song,
        thumb_shape,
        { SongLongPressMenuInfo(song, queue_index, it) },
        getString("lpm_long_press_actions"),
        multiselect_context = multiselect_context,
        multiselect_key = queue_index,
        sideButton = { modifier, background, _ ->
            LikeDislikeButton(song, modifier, { background.getContrasted() })
        }
    ) {
        SongLongPressPopupActions(it, queue_index)
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

    Item(Icons.Filled.Radio, getString("lpm_action_start_radio_long_press"))
    if (queue_index != null) {
        Item(Icons.Filled.SubdirectoryArrowRight, getString("lpm_action_play_after_long_press"))
    }

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )

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
        Item(Icons.Filled.Print, getString("lpm_action_print_info"), Modifier.clickable {
            println(song)
        })
    }
}

@Composable
private fun LongPressMenuActionProvider.SongLongPressPopupActions(song: MediaItem, queue_index: Int?) {
    require(song is Song)

    ActionButton(
        Icons.Filled.Radio, getString("lpm_action_start_radio"),
        onClick = {
            PlayerServiceHost.player.playSong(song)
        },
        onLongClick = if (queue_index == null) null else {{
            PlayerServiceHost.player.startRadioAtIndex(queue_index + 1, song, skip_first = true)
        }}
    )

    var active_queue_item: Song? by remember { mutableStateOf(null) }
    AnimatedVisibility(PlayerServiceHost.player.active_queue_index < PlayerServiceHost.status.m_queue_size) {
        if (PlayerServiceHost.player.active_queue_index < PlayerServiceHost.status.m_queue_size) {
            active_queue_item = PlayerServiceHost.player.getSong(PlayerServiceHost.player.active_queue_index)
        }

        Column {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val distance = PlayerServiceHost.player.active_queue_index - PlayerServiceHost.status.index + 1
                ActionButton(
                    Icons.Filled.SubdirectoryArrowRight,
                    getString(if (distance == 1) "lpm_action_play_after_1_song" else "lpm_action_play_after_x_songs").replace("\$x", distance.toString()),
                    fill_width = false,
                    onClick = {
                        PlayerServiceHost.player.addToQueue(
                            song,
                            PlayerServiceHost.player.active_queue_index + 1,
                            is_active_queue = true,
                            start_radio = false
                        )
                    },
                    onLongClick = {
                        PlayerServiceHost.player.addToQueue(
                            song,
                            PlayerServiceHost.player.active_queue_index + 1,
                            is_active_queue = true,
                            start_radio = true
                        )
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val button_modifier = Modifier
                        .size(30.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .align(Alignment.CenterVertically)

                    Surface(
                        button_modifier.combinedClickable(
                            remember { MutableInteractionSource() },
                            rememberRipple(),
                            onClick = {
                                PlayerServiceHost.player.updateActiveQueueIndex(-1)
                            },
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                PlayerServiceHost.player.active_queue_index = PlayerServiceHost.player.current_song_index
                            }
                        ),
                        color = accent_colour(),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Remove, null, tint = background_colour())
                    }

                    Surface(
                        button_modifier.combinedClickable(
                            remember { MutableInteractionSource() },
                            rememberRipple(),
                            onClick = {
                                PlayerServiceHost.player.updateActiveQueueIndex(1)
                            },
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                PlayerServiceHost.player.active_queue_index = PlayerServiceHost.player.song_count - 1
                            }
                        ),
                        color = accent_colour(),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Add, null, tint = background_colour())
                    }
                }
            }

            Crossfade(active_queue_item, animationSpec = tween(100)) {
                it?.PreviewLong(MediaItem.PreviewParams(
                    { playerProvider().copy(onClickedOverride = { item -> playerProvider().openMediaItem(item) }) },
                    contentColour = content_colour
                ))
            }
        }
    }

    ActionButton(Icons.Filled.Download, getString("lpm_action_download"), onClick = {
        PlayerServiceHost.download_manager.startDownload(song.id) { status: DownloadStatus ->
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
        ActionButton(Icons.Filled.Person, getString("lpm_action_go_to_artist"), onClick = {
            playerProvider().openMediaItem(song.artist!!)
        })
    }
}
