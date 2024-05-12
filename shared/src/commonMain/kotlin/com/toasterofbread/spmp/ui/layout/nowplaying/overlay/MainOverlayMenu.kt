package com.toasterofbread.spmp.ui.layout.nowplaying.overlay

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import com.toasterofbread.spmp.model.mediaitem.MEDIA_ITEM_RELATED_CONTENT_ICON
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observePlayCount
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow.ColourpickCallback
import dev.toastbits.ytmkt.model.implementedOrNull
import kotlinx.coroutines.delay

class MainPlayerOverlayMenu(
    val setPlayerOverlayMenu: (PlayerOverlayMenu?) -> Unit,
    val requestColourPicker: (ColourpickCallback) -> Unit,
    val onColourSelected: (Color) -> Unit,
    val getScreenWidth: @Composable () -> Dp
): PlayerOverlayMenu() {

    override fun closeOnTap(): Boolean = true

    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val player: PlayerState = LocalPlayerState.current
        val download_manager = player.context.download_manager
        val song: Song = getSong()

        val song_artists: List<Artist>? by song.Artists.observe(player.database)

        val download_progress = remember { Animatable(0f) }
        var download_progress_target: Float by remember { mutableStateOf(0f) }
        var download_status: DownloadStatus? by remember { mutableStateOf(null) }

        LaunchedEffect(song.id) {
            download_status = null
            download_progress.snapTo(0f)
            download_progress_target = 0f

            download_status = download_manager.getDownload(song)
        }

        DisposableEffect(Unit) {
            val status_listener: PlayerDownloadManager.DownloadStatusListener = object : PlayerDownloadManager.DownloadStatusListener() {
                override fun onDownloadChanged(status: DownloadStatus) {
                    if (status.song.id == getSong().id) {
                        download_status = status
                    }
                }
            }
            download_manager.addDownloadStatusListener(status_listener)

            onDispose {
                download_manager.removeDownloadStatusListener(status_listener)
            }
        }

        OnChangedEffect(download_progress_target) {
            download_progress.animateTo(download_progress_target)
        }

        LaunchedEffect(Unit) {
            while (true) {
                if (download_status?.status == DownloadStatus.Status.DOWNLOADING || download_status?.status == DownloadStatus.Status.PAUSED) {
                    download_progress_target = download_manager.getDownload(getSong())!!.progress
                }
                delay(1500)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val button_colour: Color = player.theme.on_accent
            val button_size: Dp = 42.dp
            val button_modifier: Modifier = Modifier
                .background(
                    player.theme.accent,
                    CircleShape
                )
                .size(button_size)
                .padding(8.dp)

            song_artists?.firstOrNull()?.also { artist ->
                MediaItemPreviewLong(artist, contentColour = { Color.White })
            }

            var song_title: String? by song.observeActiveTitle()

            var edited_song_title by remember(song) { mutableStateOf(song_title ?: "") }
            OutlinedTextField(
                edited_song_title,
                onValueChange = { text ->
                    edited_song_title = text
                },
                label = { Text(getString("edit_\$x_title_dialog_title").replace("\$x", MediaItemType.SONG.getReadable())) },
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Filled.Close, null, Modifier.clickable { edited_song_title = "" })
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    song_title = edited_song_title
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = player.theme.accent,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedTrailingIconColor = Color.White,
                    unfocusedTrailingIconColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {} // Field interaction doesn't work without this for some reason
                    .appTextField()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                Box(
                    button_modifier.clickable {
                        edited_song_title = song.Title.get(player.database) ?: ""
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = button_colour)
                }

                Box(
                    button_modifier.clickable {
                        song.CustomTitle.set(edited_song_title, player.database)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Done, null, tint = button_colour)
                }
            }

            Spacer(Modifier.fillMaxHeight().weight(1f))

            val play_count: Int = song.observePlayCount(player.context) ?: 0
            Text(getString("mediaitem_play_count_\$x_short").replace("\$x", play_count.toString()))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    button_modifier
                        .clickable {
                            setPlayerOverlayMenu(
                                SongThemePlayerOverlayMenu(
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
                            setPlayerOverlayMenu(getLyricsMenu())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = button_colour)
                }

                val related_endpoint = player.context.ytapi.SongRelatedContent.implementedOrNull()
                if (related_endpoint != null) {
                    Box(
                        button_modifier
                            .clickable(
                                remember { MutableInteractionSource() },
                                null
                            ) {
                                setPlayerOverlayMenu(RelatedContentPlayerOverlayMenu(related_endpoint))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(MEDIA_ITEM_RELATED_CONTENT_ICON, null, tint = button_colour)
                    }
                }

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        button_modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clickable {
                                if (download_status?.status != DownloadStatus.Status.FINISHED && download_status?.status != DownloadStatus.Status.ALREADY_FINISHED) {
                                    player.onSongDownloadRequested(getSong())
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
                                        .background(button_colour, CircleShape), tint = player.theme.accent)
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