package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerDownloadManager
import com.toasterofbread.spmp.platform.rememberSongDownloads
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.artistpage.LocalArtistPage
import com.toasterofbread.composekit.utils.composable.EmptyListCrossfade

class LibraryArtistsPage(context: AppContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        Icons.Default.Groups

    override fun enableSorting(): Boolean = false

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
    ) {
        val player = LocalPlayerState.current

        val downloads: List<PlayerDownloadManager.DownloadStatus> by rememberSongDownloads()
        var sorted_artists: List<Pair<ArtistRef, Int>> by remember { mutableStateOf(emptyList()) }

        with(library_page) {
            LaunchedEffect(downloads, search_filter, sort_type, reverse_sort) {

                val filter = if (search_filter?.isNotEmpty() == true) search_filter else null
                val artists: MutableList<Pair<ArtistRef, Int>> = mutableListOf()

                for (download in downloads) {
                    if (download.progress != 1f) {
                        continue
                    }

                    val artist: ArtistRef = download.song.Artist.get(player.database) ?: continue
                    val artist_index: Int = artists.indexOfFirst { it.first == artist }

                    if (artist_index == -1) {
                        val exclude = filter != null && artist.getActiveTitle(player.database)?.contains(filter, true) != true
                        artists.add(Pair(artist, if (exclude) -1 else 1))
                        continue
                    }

                    val current_artist: Pair<ArtistRef, Int> = artists[artist_index]
                    if (current_artist.second == -1) {
                        continue
                    }

                    if (filter != null && artist.getActiveTitle(player.database)?.contains(filter, true) != true) {
                        artists[artist_index] = current_artist.copy(second = -1)
                    }
                    else {
                        artists[artist_index] = current_artist.copy(second = current_artist.second + 1)
                    }
                }

                sorted_artists =
                    sort_type.sortItems(
                        artists.filter { it.second != -1 },
                        player.database,
                        reverse_sort
                    ) {
                        it.first
                    }
            }
        }

        CompositionLocalProvider(LocalPlayerState provides remember { player.copy(onClickedOverride = { item, index ->
            player.openAppPage(
                object : AppPageWithItem() {
                    override val item: MediaItemHolder = item
                    override val state: AppPageState = player.app_page_state

                    private var previous_item: MediaItemHolder? by mutableStateOf(null)

                    override fun onOpened(from_item: MediaItemHolder?) {
                        super.onOpened(from_item)
                        previous_item = from_item
                    }

                    @Composable
                    override fun ColumnScope.Page(
                        multiselect_context: MediaItemMultiSelectContext,
                        modifier: Modifier,
                        content_padding: PaddingValues,
                        close: () -> Unit,
                    ) {
                        LocalArtistPage(item.item as Artist, previous_item = previous_item?.item, content_padding = content_padding)
                    }
                }
            )
        }) }) {
            Column(modifier) {
                EmptyListCrossfade(sorted_artists) { artists ->
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = content_padding,
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        if (artists == null) {
                            item {
                                Text(
                                    if (library_page.search_filter != null) getString("library_no_items_match_filter")
                                    else getString("library_no_local_artists"),
                                    Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else {
                            itemsIndexed(artists, { _, item -> item }) { index, artist_data ->
                                val (artist, song_count) = artist_data

                                MediaItemPreviewLong(
                                    artist,
                                    multiselect_context = multiselect_context,
                                    multiselect_key = index,
                                    show_type = false,
                                    show_play_count = true,
                                    modifier = Modifier.height(75.dp),
                                    font_size = 18.sp,
                                    title_lines = 3,
                                    getExtraInfo = {
                                        listOf(
                                            getString("artist_\$x_songs")
                                                .replace("\$x", song_count.toString())
                                        )
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
