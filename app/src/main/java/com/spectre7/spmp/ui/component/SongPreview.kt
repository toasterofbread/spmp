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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerDownloadService
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.utils.sendToast
import com.spectre7.utils.setAlpha
import com.spectre7.utils.vibrateShort
import java.io.File

@Composable
fun SongPreviewSquare(
    song: Song, 
    content_colour: () -> Color,
    playerProvider: () -> PlayerViewContext,
    enable_long_press_menu: Boolean = true,
    modifier: Modifier = Modifier
) {
    val long_press_menu_data = remember(song) { LongPressMenuData(
        song,
        RoundedCornerShape(10),
        songLongPressPopupActions
    ) }

    Column(
        modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    playerProvider().onMediaItemClicked(song)
                },
                onLongClick = {
                    playerProvider().showLongPressMenu(long_press_menu_data)
                }
            )
            .aspectRatio(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        song.Thumbnail(MediaItem.ThumbnailQuality.LOW,
            Modifier
                .size(100.dp)
                .longPressMenuIcon(long_press_menu_data, enable_long_press_menu))

        Text(
            song.title ?: "",
            fontSize = 12.sp,
            color = content_colour(),
            maxLines = 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongPreviewLong(
    song: Song,
    content_colour: () -> Color,
    playerProvider: () -> PlayerViewContext,
    enable_long_press_menu: Boolean = true,
    modifier: Modifier = Modifier
) {
    val long_press_menu_data = remember(song) { LongPressMenuData(
        song,
        RoundedCornerShape(20),
        songLongPressPopupActions
    ) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(10.dp, 0.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    playerProvider().onMediaItemClicked(song)
                },
                onLongClick = {
                    playerProvider().showLongPressMenu(long_press_menu_data)
                }
            )
    ) {
        song.Thumbnail(MediaItem.ThumbnailQuality.LOW,
            Modifier
                .size(40.dp)
                .longPressMenuIcon(long_press_menu_data, enable_long_press_menu))

        Column(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(0.9f)) {
            Text(
                song.title ?: "",
                fontSize = 15.sp,
                color = content_colour(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                song.artist?.title ?: "",
                fontSize = 11.sp,
                color = content_colour().setAlpha(0.5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

val songLongPressPopupActions: @Composable LongPressMenuActionProvider.(MediaItem) -> Unit = { song ->
    if (song !is Song) {
        throw IllegalStateException()
    }

    ActionButton(Icons.Filled.Radio, "Start radio", onClick = {
        PlayerServiceHost.service.playSong(song)
    })

    var active_queue_item: Song? by remember { mutableStateOf(null) }
    AnimatedVisibility(PlayerServiceHost.service.active_queue_index < PlayerServiceHost.status.m_queue.size) {
        if (PlayerServiceHost.service.active_queue_index < PlayerServiceHost.status.m_queue.size) {
            active_queue_item = PlayerServiceHost.status.m_queue[PlayerServiceHost.service.active_queue_index]
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {

                val distance = PlayerServiceHost.service.active_queue_index - PlayerServiceHost.status.index + 1
                ActionButton(Icons.Filled.SubdirectoryArrowRight, "Play after $distance song(s)",
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = {
                        PlayerServiceHost.service.addToQueue(
                            song,
                            PlayerServiceHost.service.active_queue_index + 1,
                            is_active_queue = true,
                            start_radio = false
                        )
                    },
                    onLongClick = {
                        PlayerServiceHost.service.addToQueue(
                            song,
                            PlayerServiceHost.service.active_queue_index + 1,
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
                                PlayerServiceHost.service.updateActiveQueueIndex(-1)
                            },
                            onLongClick = {
                                vibrateShort()
                                PlayerServiceHost.service.active_queue_index = PlayerServiceHost.player.currentMediaItemIndex
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
                                PlayerServiceHost.service.updateActiveQueueIndex(1)
                            },
                            onLongClick = {
                                vibrateShort()
                                PlayerServiceHost.service.active_queue_index = PlayerServiceHost.player.mediaItemCount - 1
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
                it?.PreviewLong(
                    content_colour,
                    { playerProvider().copy(onClickedOverride = { item -> playerProvider().openMediaItem(item) }) },
                    true,
                    Modifier
                )
            }
        }
    }

    ActionButton(Icons.Filled.Download, "Download", onClick = {
        PlayerServiceHost.download_manager.startDownload(song.id) { file: File?, status: PlayerDownloadService.DownloadStatus ->
            when (status) {
                PlayerDownloadService.DownloadStatus.FINISHED -> sendToast("Download completed")
                PlayerDownloadService.DownloadStatus.ALREADY_FINISHED -> sendToast("Already downloaded")
                PlayerDownloadService.DownloadStatus.CANCELLED -> sendToast("Download was cancelled")

                // IDLE, DOWNLOADING, PAUSED
                else -> {
                    sendToast("Already downloading")
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
