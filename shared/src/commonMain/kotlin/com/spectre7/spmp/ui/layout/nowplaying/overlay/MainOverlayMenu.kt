package com.spectre7.spmp.ui.layout.nowplaying.overlay

import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_MAIN_PADDING
import com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics.LyricsOverlayMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.composable.OnChangedEffect
import kotlinx.coroutines.delay

class MainOverlayMenu(
    val setOverlayMenu: (OverlayMenu?) -> Unit,
    val requestColourPicker: ((Color?) -> Unit) -> Unit,
    val onColourSelected: (Color) -> Unit,
    val getScreenWidth: @Composable () -> Dp
): OverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Menu(
        songProvider: () -> Song,
        expansion: Float,
        openShutterMenu: (@Composable () -> Unit) -> Unit,
        close: () -> Unit,
        seek_state: Any,
        playerProvider: () -> PlayerViewContext
    ) {

        val download_progress = remember { Animatable(0f) }
        var download_progress_target: Float by remember { mutableStateOf(0f) }
        var download_status: DownloadStatus? by remember { mutableStateOf(null) }

        LaunchedEffect(songProvider().id) {
            download_status = null
            download_progress.snapTo(0f)
            download_progress_target = 0f

            PlayerServiceHost.download_manager.getDownload(songProvider()) {
                download_status = it
            }
        }

        DisposableEffect(Unit) {
            val status_listener = object : PlayerDownloadManager.DownloadStatusListener() {
                override fun onDownloadChanged(status: DownloadStatus) {
                    if (status.song == songProvider()) {
                        download_status = status
                    }
                }
            }
            PlayerServiceHost.download_manager.addDownloadStatusListener(status_listener)

            onDispose {
                PlayerServiceHost.download_manager.removeDownloadStatusListener(status_listener)
            }
        }

        OnChangedEffect(download_progress_target) {
            download_progress.animateTo(download_progress_target)
        }

        LaunchedEffect(Unit) {
            while (true) {
                if (download_status?.status == DownloadStatus.Status.DOWNLOADING || download_status?.status == DownloadStatus.Status.PAUSED) {
                    PlayerServiceHost.download_manager.getDownload(songProvider()) {
                        download_progress_target = it!!.progress
                    }
                }
                delay(1500)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val song = songProvider()

            val button_colour = Theme.current.on_accent
            val button_size = 42.dp
            val button_modifier = Modifier
                .background(
                    Theme.current.accent,
                    CircleShape
                )
                .size(button_size)
                .padding(8.dp)

            song.artist?.PreviewLong(MediaItem.PreviewParams(
                playerProvider,
                contentColour = { Color.White },
            ))

            var edited_song_title by remember(song) { mutableStateOf(song.title!!) }
            OutlinedTextField(
                edited_song_title,
                onValueChange = { text ->
                    edited_song_title = text
                },
                label = { Text(getStringTODO("Edit title")) },
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Filled.Close, null, Modifier.clickable { edited_song_title = "" })
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    song.editRegistry {
                        it.title = edited_song_title
                    }
                }),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = button_colour,
                    focusedLabelColor = button_colour,
                    cursorColor = button_colour
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {} // Field interaction doesn't work without this for some reason
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                Box(
                    button_modifier.clickable {
                        edited_song_title = song.original_title!!
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = button_colour)
                }

                Box(
                    button_modifier.clickable {
                        song.registry_entry.title = edited_song_title
                        song.saveRegistry()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Done, null, tint = button_colour)
                }
            }

            Spacer(Modifier
                .fillMaxHeight()
                .weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    button_modifier
                        .clickable {
                            setOverlayMenu(
                                PaletteSelectorOverlayMenu(
                                    songProvider()::getDefaultThemeColour,
                                    requestColourPicker,
                                    onColourSelected
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, null, tint = button_colour)
                }

                val screen_width = getScreenWidth()
                Box(
                    button_modifier
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { setOverlayMenu(
                                LyricsOverlayMenu(
                                    (screen_width - (NOW_PLAYING_MAIN_PADDING.dp * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                                )
                            ) },
                            onLongClick = {
                                SpMp.context.vibrateShort()
                                setOverlayMenu(
                                    LyricsOverlayMenu(
                                        (screen_width - (NOW_PLAYING_MAIN_PADDING.dp * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = button_colour)
                }

                Box(
                    button_modifier
                        .clickable(
                            remember { MutableInteractionSource() },
                            null
                        ) {
//                            setOverlayMenu(
//                                LyricsOverlayMenu(
//                                    (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
//                                )
//                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.BarChart, null, tint = button_colour)
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        button_modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clickable {
                                if (download_status?.status != DownloadStatus.Status.FINISHED && download_status?.status != DownloadStatus.Status.ALREADY_FINISHED) {
                                    PlayerServiceHost.download_manager.startDownload(songProvider().id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.align(Alignment.Center), tint = button_colour)
                        Crossfade(
                            when (download_status?.status) {
                                DownloadStatus.Status.PAUSED -> Icons.Filled.Pause
                                DownloadStatus.Status.FINISHED, DownloadStatus.Status.ALREADY_FINISHED -> Icons.Filled.Done
                                DownloadStatus.Status.CANCELLED -> Icons.Filled.Cancel
                                else -> null
                            }
                        ) { icon ->
                            if (icon != null) {
                                val offset = button_size * 0.2f
                                Icon(icon, null,
                                    Modifier
                                        .size(10.dp)
                                        .offset(offset, offset)
                                        .background(button_colour, CircleShape), tint = Theme.current.accent)
                            }
                        }
                    }

                    if (download_status?.status == DownloadStatus.Status.DOWNLOADING || download_status?.status == DownloadStatus.Status.PAUSED) {
                        CircularProgressIndicator(download_progress.value, Modifier.size(button_size), color = button_colour, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}