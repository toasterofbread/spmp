package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.PlayerDownloadManager.DownloadStatus
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.mainpage.MainPage
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.thenIf
import com.toasterofbread.spmp.ui.layout.prefspage.rememberYtmAuthItem

enum class LibrarySubPage { SONGS }

fun LibrarySubPage?.getReadable(): String =
    getString(when (this) {
        null -> "library_main_page_title"
        LibrarySubPage.SONGS -> "library_songs_page_title"
    })

fun LibrarySubPage?.getIcon(): ImageVector =
    when (this) {
        null -> Icons.Default.LibraryMusic
        LibrarySubPage.SONGS -> Icons.Default.MusicNote
    }

class LibraryMainPage(): MainPage() {
    @Composable
    override fun Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier, 
        content_padding: PaddingValues, 
        close: () -> Unit
    ) {
        LibraryPage(content_padding, modifier, multiselect_context, close)
    }
}

@Composable
fun LibraryPage(
    content_padding: PaddingValues,
    modifier: Modifier = Modifier,
    outer_multiselect_context: MediaItemMultiSelectContext? = null,
    close: () -> Unit
) {
    val player = LocalPlayerState.current
    val multiselect_context = remember(outer_multiselect_context) { outer_multiselect_context ?: MediaItemMultiSelectContext {} }

    val downloads: MutableList<DownloadStatus> = remember { mutableStateListOf() }
    var subpage: LibrarySubPage? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        player.download_manager.getDownloads {
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
        player.download_manager.addDownloadStatusListener(listener)

        onDispose {
            player.download_manager.removeDownloadStatusListener(listener)
        }
    }

    BackHandler(subpage != null) {
        subpage = null
    }

    Crossfade(subpage, Modifier.fillMaxSize()) { page ->
        when (page) {
            null -> LibraryMainPage(
                content_padding,
                multiselect_context,
                downloads,
                { subpage = it },
            ) { songs, song, index ->
                onSongClicked(songs, player, song, index)
            }

            LibrarySubPage.SONGS -> LibrarySongsPage(
                content_padding,    
                multiselect_context,
                downloads,
                { subpage = it }
            ) { songs, song, index ->
                onSongClicked(songs, player, song, index)
            }
        }
    }
}

private fun onSongClicked(songs: List<Song>, player: PlayerState, song: Song, index: Int) {
    player.withPlayer {
        // TODO Config
        val ADD_BEFORE = 2
        val ADD_AFTER = 7

        val add_songs = songs
            .mapIndexedNotNull { song_index, song ->
                if (song_index < index && index - song_index > ADD_BEFORE) {
                    return@mapIndexedNotNull null
                }

                if (song_index > index && song_index - index > ADD_AFTER) {
                    return@mapIndexedNotNull null
                }

                song
            }

        val song_index = minOf(ADD_BEFORE, index)
        assert(add_songs[song_index] == song)

        clearQueue(save = false)
        addMultipleToQueue(add_songs)
        seekToSong(song_index)
    }
}
