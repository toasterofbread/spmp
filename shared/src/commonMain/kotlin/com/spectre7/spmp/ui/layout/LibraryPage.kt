package com.spectre7.spmp.ui.layout

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.api.LocalisedYoutubeString
import com.spectre7.spmp.model.mediaitem.AccountPlaylist
import com.spectre7.spmp.model.mediaitem.LocalPlaylist
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.theme.Theme
import kotlinx.coroutines.launch

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    outer_multiselect_context: MediaItemMultiSelectContext? = null,
    close: () -> Unit
) {
    val downloads: MutableList<DownloadStatus> = remember { mutableStateListOf() }
    val coroutine_scope = rememberCoroutineScope()
    val player = LocalPlayerState.current
    val multiselect_context = remember(outer_multiselect_context) { outer_multiselect_context ?: MediaItemMultiSelectContext {} }

    val heading_text_style = MaterialTheme.typography.headlineSmall

    DisposableEffect(Unit) {
        PlayerServiceHost.download_manager.getDownloads {
            downloads.addAll(it)
        }

        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: DownloadStatus) {
                downloads.add(status)
            }
            override fun onDownloadRemoved(id: String) {
                downloads.removeIf { it.id == id }
            }
            override fun onDownloadChanged(status: DownloadStatus) {
                for (i in downloads.indices) {
                    if (downloads[i].id == status.id) {
                        downloads[i] = status
                    }
                }
            }
        }
        PlayerServiceHost.download_manager.addDownloadStatusListener(listener)

        onDispose {
            PlayerServiceHost.download_manager.removeDownloadStatusListener(listener)
        }
    }

    Column(
        modifier.run {
            if (!inline) padding(horizontal = 20.dp, vertical = 10.dp)
            else this
        },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Title bar
        if (!inline) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null)

                Text(
                    getString("library_page_title"),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Theme.current.on_background
                    )
                )

                Spacer(Modifier.width(24.dp))
            }
        }

        AnimatedVisibility(outer_multiselect_context == null && multiselect_context.is_active) {
            multiselect_context.InfoDisplay()
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(30.dp),
            contentPadding = PaddingValues(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp * 2f)
        ) {
            // Playlists
            item {
                val ytm_auth = DataApi.ytm_auth

                Column(Modifier.fillMaxWidth().animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(getString("library_row_playlists"), style = heading_text_style)

                        Spacer(Modifier.fillMaxWidth().weight(1f))

                        if (ytm_auth.initialised) {
                            IconButton({ coroutine_scope.launch {
                                ytm_auth.loadOwnPlaylists()
                            } }) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }

                        IconButton({ coroutine_scope.launch {
                            val playlist = LocalPlaylist.createLocalPlaylist(SpMp.context)
                            player.openMediaItem(playlist)
                        }}) {
                            Icon(Icons.Default.Add, null)
                        }
                    }

                    val local_playlists = LocalPlaylist.rememberLocalPlaylistsListener()
                    if (local_playlists.isNotEmpty()) {
                        MediaItemGrid(
                            local_playlists,
                            Modifier.fillMaxWidth(),
                            rows = 1,
                            multiselect_context = multiselect_context
                        )
                    }
                    else {
                        Text(getString("library_playlists_empty"), Modifier.padding(top = 10.dp))
                    }

                    MediaItemGrid(
                        ytm_auth.own_playlists.map { AccountPlaylist.fromId(it) },
                        Modifier.fillMaxWidth(),
                        title = LocalisedYoutubeString.raw("Account playlists"),
                        rows = 1,
                        multiselect_context = multiselect_context
                    )
                }
            }

            // Songs
            item {
                Column(Modifier.fillMaxWidth().animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        getString("library_row_recent_songs"),
                        Modifier.align(Alignment.Start),
                        style = heading_text_style
                    )

                    if (downloads.isNotEmpty()) {
                        var shown_songs = 0
                        for (download in downloads) {
                            if (download.progress < 1f) {
                                continue
                            }

                            val song = download.song
                            song.PreviewLong(MediaItem.PreviewParams())

                            if (++shown_songs == 5) {
                                break
                            }
                        }
                    }
                    else {
                        Text("No songs downloaded", Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}
