package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MEDIA_ITEM_RELATED_CONTENT_ICON
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.observeAsState
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.OnChangedEffect
import kotlinx.coroutines.delay

class MainOverlayMenu(
    val setOverlayMenu: (OverlayMenu?) -> Unit,
    val requestColourPicker: ((Color?) -> Unit) -> Unit,
    val onColourSelected: (Color) -> Unit,
    val getScreenWidth: @Composable () -> Dp
): OverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val song = getSong()
        val player = LocalPlayerState.current
        val db = SpMp.context.database

        val download_progress = remember { Animatable(0f) }
        var download_progress_target: Float by remember { mutableStateOf(0f) }
        var download_status: DownloadStatus? by remember { mutableStateOf(null) }

        LaunchedEffect(song.id) {
            download_status = null
            download_progress.snapTo(0f)
            download_progress_target = 0f

            player.download_manager.getDownload(song) {
                download_status = it
            }
        }

        DisposableEffect(Unit) {
            val status_listener = object : PlayerDownloadManager.DownloadStatusListener() {
                override fun onDownloadChanged(status: DownloadStatus) {
                    if (status.song == getSong()) {
                        download_status = status
                    }
                }
            }
            player.download_manager.addDownloadStatusListener(status_listener)

            onDispose {
                player.download_manager.removeDownloadStatusListener(status_listener)
            }
        }

        OnChangedEffect(download_progress_target) {
            download_progress.animateTo(download_progress_target)
        }

        LaunchedEffect(Unit) {
            while (true) {
                if (download_status?.status == DownloadStatus.Status.DOWNLOADING || download_status?.status == DownloadStatus.Status.PAUSED) {
                    player.download_manager.getDownload(getSong()) {
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
            val button_colour = Theme.on_accent
            val button_size = 42.dp
            val button_modifier = Modifier
                .background(
                    Theme.accent,
                    CircleShape
                )
                .size(button_size)
                .padding(8.dp)

            val artist = song.artist
            if (artist != null) {
                MediaItemPreviewLong(artist, contentColour = { Color.White })
            }

            var song_title by db.mediaItemQueries
                .titleById(song.id)
                .observeAsState(
                    { it.executeAsOne().title },
                    { db.mediaItemQueries.updateTitleById(it, song.id) }
                )

            var edited_song_title by remember(song) { mutableStateOf(song_title ?: "") }
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
                    song_title = edited_song_title
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
                        song_title = edited_song_title
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
                        .clickable { 
                            setOverlayMenu(getLyricsMenu())
                        },
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
                            setOverlayMenu(RelatedContentOverlayMenu()) 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(MEDIA_ITEM_RELATED_CONTENT_ICON, null, tint = button_colour)
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        button_modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clickable {
                                if (download_status?.status != DownloadStatus.Status.FINISHED && download_status?.status != DownloadStatus.Status.ALREADY_FINISHED) {
                                    player.download_manager.startDownload(getSong().id)
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
                                        .background(button_colour, CircleShape), tint = Theme.accent)
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