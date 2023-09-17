package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.mainpage.PlayerState
import com.toasterofbread.utils.composable.EmptyListCrossfade

class LibrarySongsPage(context: PlatformContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        MediaItemType.SONG.getIcon()

    @Composable
    override fun Page(
        library_page: LibraryPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier
    ) {
        val player = LocalPlayerState.current
        val downloads: List<PlayerDownloadManager.DownloadStatus> = rememberSongDownloads()

        var sorted_songs: List<Song> by remember { mutableStateOf(emptyList()) }

        with(library_page) {
            LaunchedEffect(downloads, search_filter, sort_type, reverse_sort) {
                val filter = if (search_filter?.isNotEmpty() == true) search_filter else null
                val filtered = downloads.mapNotNull {  download ->
                    if (download.progress != 1f) return@mapNotNull null

                    if (filter != null && download.song.getActiveTitle(player.database)?.contains(filter, true) != true) {
                        return@mapNotNull null
                    }

                    return@mapNotNull download.song
                }

                sorted_songs = sort_type.sortItems(filtered, player.database, reverse_sort)
            }
        }

        CompositionLocalProvider(LocalPlayerState provides remember { player.copy(onClickedOverride = { item, index ->
            onSongClicked(sorted_songs, player, item as Song, index!!)
        }) }) {
            Column(modifier) {
                EmptyListCrossfade(sorted_songs) { current_songs ->
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = content_padding,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (current_songs == null) {
                            item {
                                Text(
                                    if (library_page.search_filter != null) getString("library_no_items_match_filter")
                                    else getString("library_no_local_songs"),
                                    Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else {
                            item {
                                InfoRow(current_songs, Modifier.fillMaxWidth())
                            }

                            itemsIndexed(current_songs, { _, item -> item.id }) { index, song ->
                                MediaItemPreviewLong(
                                    song,
                                    multiselect_context = multiselect_context,
                                    multiselect_key = index,
                                    show_type = false,
                                    show_play_count = true,
                                    getExtraInfo = {
                                        val duration_string = remember(song.id) {
                                            song.Duration.get(player.database)?.let { duration ->
                                                durationToString(duration, true)
                                            }
                                        }

                                        listOfNotNull(duration_string)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(songs: List<Song>, modifier: Modifier = Modifier) {
    if (songs.isEmpty()) {
        return
    }

    val player = LocalPlayerState.current

    var total_duration_string: String? by remember { mutableStateOf(null) }
    LaunchedEffect(songs) {
        total_duration_string = null

        val duration = songs.sumOf {
            it.Duration.get(player.database) ?: 0
        }
        if (duration == 0L) {
            return@LaunchedEffect
        }

        total_duration_string = durationToString(duration)
    }

    Row(modifier) {
        Text(getString("library_\$x_songs").replace("\$x", songs.size.toString()))

        Spacer(Modifier.fillMaxWidth().weight(1f))

        total_duration_string?.also { duration ->
            Text(duration)
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
