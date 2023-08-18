package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

@Composable
fun LibraryPage.LibrarySongsPage(
    content_padding: PaddingValues,
    multiselect_context: MediaItemMultiSelectContext,
    downloads: List<PlayerDownloadManager.DownloadStatus>,
    openTab: (LibraryPage.Tab) -> Unit,
    modifier: Modifier = Modifier,
    onSongClicked: (songs: List<Song>, song: Song, index: Int) -> Unit
) {
    val player = LocalPlayerState.current

    var sorted_songs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(search_filter, sort_option, reverse_sort, downloads) {
        val filter = if (search_filter?.isNotEmpty() == true) search_filter else null
        val filtered = downloads.mapNotNull {  download ->
            if (download.progress != 1f) return@mapNotNull null

            if (filter != null && download.song.Title.get(player.database)?.contains(filter, true) != true) {
                return@mapNotNull null
            }

            return@mapNotNull download.song
        }

        sorted_songs = sort_option.sortItems(filtered, player.database, reverse_sort)
    }

    Column(modifier) {
        CompositionLocalProvider(LocalPlayerState provides remember { player.copy(onClickedOverride = { item, index ->
            onSongClicked(sorted_songs, item as Song, index!!)
        }) }) {
            var current_songs by remember { mutableStateOf(sorted_songs) }
            LaunchedEffect(sorted_songs) {
                if (sorted_songs.isNotEmpty()) {
                    current_songs = sorted_songs
                }
            }

            Crossfade(sorted_songs.isEmpty()) { is_empty ->
                if (is_empty) {
                    Box(Modifier.fillMaxWidth().padding(content_padding), contentAlignment = Alignment.TopCenter) {
                        Text(getStringTODO(
                            if (search_filter != null) "No items match filter"
                            else                       "No items"
                        ))
                    }
                }
                else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = content_padding,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                getString("library_tab_local_songs"),
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineMedium,
                                softWrap = false
                            )
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
