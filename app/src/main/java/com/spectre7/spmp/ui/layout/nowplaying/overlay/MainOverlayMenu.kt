package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
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
import androidx.palette.graphics.Palette
import com.spectre7.spmp.*
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.NOW_PLAYING_MAIN_PADDING
import com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics.LyricsOverlayMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.OnChangedEffect
import com.spectre7.utils.spacedByEnd
import com.spectre7.utils.vibrateShort
import kotlinx.coroutines.delay

class MainOverlayMenu(
    val setOverlayMenu: (OverlayMenu?) -> Unit,
    val themePaletteProvider: () -> Palette?,
    val requestColourPicker: ((Color?) -> Unit) -> Unit,
    val onColourSelected: (Color) -> Unit,
    val screen_width_dp: Dp
): OverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @OptIn(ExperimentalFoundationApi::class)
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
        var download_status: PlayerDownloadService.DownloadStatus? by remember { mutableStateOf(null) }

        LaunchedEffect(songProvider().id) {
            download_status = null
            download_progress.snapTo(0f)
            download_progress_target = 0f

            PlayerServiceHost.download_manager.getSongDownloadStatus(songProvider().id) {
                download_status = it
            }
        }

        DisposableEffect(Unit) {
            val status_listener = object : PlayerDownloadManager.DownloadStatusListener() {
                override fun onSongDownloadStatusChanged(
                    song_id: String,
                    status: PlayerDownloadService.DownloadStatus,
                ) {
                    if (song_id == songProvider().id) {
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
                if (download_status == PlayerDownloadService.DownloadStatus.DOWNLOADING || download_status == PlayerDownloadService.DownloadStatus.PAUSED) {
                    PlayerServiceHost.download_manager.getSongDownloadProgress(songProvider().id) {
                        download_progress_target = it
                    }
                }
                delay(1500)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            song.artist?.PreviewLong(
                content_colour = { Color.White },
                playerProvider,
                true,
                Modifier
            )

            var edited_song_title by remember(song.title) { mutableStateOf(song.title!!) }
            OutlinedTextField(
                edited_song_title,
                onValueChange = { text ->
                    edited_song_title = text
                },
                label = { Text("Edit title") },
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Filled.Close, null, Modifier.clickable { edited_song_title = "" })
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    song.registry_entry.title = edited_song_title
                    song.saveRegistry()
                }),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = button_colour,
                    focusedLabelColor = button_colour,
                    cursorColor = button_colour
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { throw IllegalStateException() } // Field interaction doesn't work without this for some reason
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedByEnd(10.dp)) {
                Box(
                    button_modifier.clickable {
                        song.registry_entry.title = song.original_title!!
                        song.saveRegistry()
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
                                    themePaletteProvider,
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

                Box(
                    button_modifier
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { setOverlayMenu(
                                LyricsOverlayMenu(
                                    (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                                )
                            ) },
                            onLongClick = {
                                vibrateShort()
                                setOverlayMenu(
                                    LyricsOverlayMenu(
                                        (screen_width_dp - (NOW_PLAYING_MAIN_PADDING * 2) - (15.dp * expansion * 2)).value * 0.9.dp
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = button_colour)
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        button_modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clickable {
                                PlayerServiceHost.download_manager.startDownload(songProvider().id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.align(Alignment.Center), tint = button_colour)
                        Crossfade(
                            when (download_status) {
                                PlayerDownloadService.DownloadStatus.PAUSED -> Icons.Filled.Pause
                                PlayerDownloadService.DownloadStatus.FINISHED, PlayerDownloadService.DownloadStatus.ALREADY_FINISHED -> Icons.Filled.Done
                                PlayerDownloadService.DownloadStatus.CANCELLED -> Icons.Filled.Cancel
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

                    if (download_status == PlayerDownloadService.DownloadStatus.DOWNLOADING || download_status == PlayerDownloadService.DownloadStatus.PAUSED) {
                        CircularProgressIndicator(download_progress.value, Modifier.size(button_size), color = button_colour, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}