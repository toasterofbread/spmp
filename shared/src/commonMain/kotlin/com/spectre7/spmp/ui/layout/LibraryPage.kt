package com.spectre7.spmp.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.api.LocalisedYoutubeString
import com.spectre7.spmp.platform.PlayerDownloadManager
import com.spectre7.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.mainpage.MINIMISED_NOW_PLAYING_HEIGHT
import com.spectre7.spmp.ui.layout.mainpage.PlayerViewContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.addUnique

@Composable
fun LibraryPage(
    pill_menu: PillMenu,
    playerProvider: () -> PlayerViewContext,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    close: () -> Unit
) {
    var layouts: List<MediaItemLayout> by remember { mutableStateOf(emptyList()) }
    val downloads: MutableList<DownloadStatus> = remember { mutableStateListOf() }

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
        modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp * 2f)
    ) {
        if (!inline) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        getStringTODO("Library"),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Theme.current.on_background
                        )
                    )
                }
            }
        }

        items(layouts) { layout ->
            layout.Layout(playerProvider, Modifier.fillMaxWidth())
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
