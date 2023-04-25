@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.spectre7.spmp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.*
import com.spectre7.utils.*

@Composable
fun SongPreviewSquare(
    song: Song, 
    params: MediaItem.PreviewParams,
    queue_index: Int? = null
) {
    val long_press_menu_data = remember(song) {
        getSongLongPressMenuData(song, RoundedCornerShape(10.dp), queue_index = queue_index)
    }

    Column(
        params.modifier
            .platformClickable(
                onClick = {
                    params
                        .playerProvider()
                        .onMediaItemClicked(song)
                },
                onAltClick = {
                    params
                        .playerProvider()
                        .showLongPressMenu(long_press_menu_data)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            song.Thumbnail(
                MediaItem.ThumbnailQuality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                params.content_colour
            )
        }

        Text(
            song.title ?: "",
            fontSize = 12.sp,
            color = params.content_colour(),
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
        getSongLongPressMenuData(song, RoundedCornerShape(20), queue_index = queue_index)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = params.modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    params
                        .playerProvider()
                        .onMediaItemClicked(song)
                },
                onLongClick = {
                    params
                        .playerProvider()
                        .showLongPressMenu(long_press_menu_data)
                }
            )
    ) {
        song.Thumbnail(
            MediaItem.ThumbnailQuality.LOW,
            Modifier
                .longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu)
                .size(40.dp),
            params.content_colour
        )

        Column(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Text(
                song.title ?: "",
                fontSize = 15.sp,
                color = params.content_colour(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                @Composable
                fun InfoText(text: String) {
                    Text(
                        text,
                        fontSize = 11.sp,
                        color = params.content_colour().setAlpha(0.5f),
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
    queue_index: Int? = null
): LongPressMenuData {
    return LongPressMenuData(
        song,
        thumb_shape,
        { SongLongPressMenuInfo(song, queue_index, it) },
        getStringTemp("Long press actions"),
        actions = {
            SongLongPressPopupActions(it, queue_index)
        },
        sideButton = { modifier, background, _ ->
            LikeDislikeButton(song, modifier, { background.getContrasted() })
        }
    )
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

    Item(Icons.Filled.Radio, getStringTemp("Start radio at song position in queue"))
    Item(Icons.Filled.SubdirectoryArrowRight, getStringTemp("Start radio after X song(s)"))
    Item(Icons.Filled.Download, getStringTemp("Configure download"))

    Spacer(
        Modifier
            .fillMaxHeight()
            .weight(1f)
    )

    Row(Modifier.requiredHeight(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("ID: ")
        Text(song.id,
            Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        SpMp.context.CopyShareButtons { song.id }
    }

    if (queue_index != null) {
        Text("Queue index: $queue_index")
    }

    if (isDebugBuild()) {
        Item(Icons.Filled.Print, "Print info", Modifier.clickable {
            println(song)
        })
    }
}

@Composable
private fun LongPressMenuActionProvider.SongLongPressPopupActions(song: MediaItem, queue_index: Int?) {
    require(song is Song)

    ActionButton(
        Icons.Filled.Radio, "Start radio", 
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
                ActionButton(Icons.Filled.SubdirectoryArrowRight, "Play after $distance song(s)",
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
                    content_colour = content_colour
                ))
            }
        }
    }

    ActionButton(Icons.Filled.Download, "Download", onClick = {
        PlayerServiceHost.download_manager.startDownload(song.id) { status: DownloadStatus ->
            when (status.status) {
                DownloadStatus.Status.FINISHED -> SpMp.context.sendToast("Download completed")
                DownloadStatus.Status.ALREADY_FINISHED -> SpMp.context.sendToast("Already downloaded")
                DownloadStatus.Status.CANCELLED -> SpMp.context.sendToast("Download was cancelled")

                // IDLE, DOWNLOADING, PAUSED
                else -> {
                    SpMp.context.sendToast("Already downloading")
                }
            }
        }
    })

    if (song.artist != null) {
        ActionButton(Icons.Filled.Person, "Go to artist", onClick = {
            playerProvider().openMediaItem(song.artist!!)
        })
    }
}
