package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.api.LocalisedYoutubeString
import com.spectre7.spmp.model.LocalPlaylist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.MediaItemType
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.platform.getDefaultPaddingValues
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.addUnique
import kotlinx.coroutines.launch

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    close: () -> Unit
) {
    var layouts: List<MediaItemLayout> by remember { mutableStateOf(emptyList()) }
    val downloads: MutableList<DownloadStatus> = remember { mutableStateListOf() }
    val coroutine_scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        PlayerServiceHost.download_manager.getDownloads {
            downloads.addAll(it)
            layouts = generateLibraryLayouts(downloads)
        }

        val listener = object : PlayerDownloadManager.DownloadStatusListener() {
            override fun onDownloadAdded(status: DownloadStatus) {
                downloads.add(status)
                layouts = generateLibraryLayouts(downloads)
            }
            override fun onDownloadRemoved(id: String) {
                downloads.removeIf { it.id == id }
                layouts = generateLibraryLayouts(downloads)
            }
            override fun onDownloadChanged(status: DownloadStatus) {
                for (i in downloads.indices) {
                    if (downloads[i].id == status.id) {
                        downloads[i] = status
                        layouts = generateLibraryLayouts(downloads)
                    }
                }
            }
        }
        PlayerServiceHost.download_manager.addDownloadStatusListener(listener)

        onDispose {
            PlayerServiceHost.download_manager.removeDownloadStatusListener(listener)
        }
    }

    LazyColumn(
        modifier.run {
            if (!inline) padding(SpMp.context.getDefaultPaddingValues())
            else this
        },
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp * 2f)
    ) {
        if (!inline) {
            // Title bar
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Default.MusicNote, null)

                    Text(
                        getString("page_title_library"),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Theme.current.on_background
                        )
                    )

                    Spacer(Modifier.width(24.dp))
                }
            }
        }

        // Playlists
        item {
            Column(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(MediaItemType.PLAYLIST.getReadable(true), style = MaterialTheme.typography.headlineMedium)

                    IconButton({ coroutine_scope.launch {
                        val playlist = LocalPlaylist.createLocalPlaylist(SpMp.context)
                        println("created $playlist")
                    }}) {
                        Icon(Icons.Default.Add, null)
                    }
                }

                var local_playlists: MutableList<LocalPlaylist>? by remember { mutableStateOf(null) }

                LaunchedEffect(Unit) {
                    local_playlists = mutableStateListOf<LocalPlaylist>().apply { addAll(LocalPlaylist.getLocalPlaylists(SpMp.context).toMutableList()) }
                }
                DisposableEffect(Unit) {
                    val listener = object : LocalPlaylist.Listener {
                        override fun onAdded(playlist: LocalPlaylist) {
                            local_playlists?.add(playlist)
                        }
                        override fun onRemoved(index: Int, playlist: LocalPlaylist) {}
                    }
                    LocalPlaylist.addPlaylistsListener(listener)

                    onDispose {
                        LocalPlaylist.removePlaylistsListener(listener)
                    }
                }

                local_playlists?.also { playlists ->

                    val layout = remember {
                        MediaItemLayout(
                            null, null,
                            MediaItemLayout.Type.ROW,
                            playlists as MutableList<MediaItem>
                        )
                    }

                    layout.Layout(Modifier.fillMaxWidth())
                }
            }
        }

        items(layouts) { layout ->
            layout.Layout(Modifier.fillMaxWidth())
        }
    }
}

fun generateLibraryLayouts(downloads: List<DownloadStatus>): List<MediaItemLayout> {
    val top_songs = MediaItemLayout(LocalisedYoutubeString.temp("Top songs"), null, type = MediaItemLayout.Type.NUMBERED_LIST)
    val artists = MediaItemLayout(LocalisedYoutubeString.temp("Artists"), null, type = MediaItemLayout.Type.ROW)

    for (download in downloads) {
        if (download.progress < 1f) {
            continue
        }
        top_songs.items.add(download.song)

        download.song.artist?.also {
            artists.items.addUnique(it)
        }
    }
    top_songs.items.sortByDescending { it.registry_entry.play_count }

    return listOf(top_songs, artists)
}
