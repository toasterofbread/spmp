package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
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
import dev.toastbits.composekit.components.platform.composable.ScrollBarLazyColumn
import dev.toastbits.composekit.components.utils.composable.LoadActionIconButton
import dev.toastbits.composekit.components.utils.composable.RowOrColumnScope
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.artist.toArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.rememberSongDownloads
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.artistpage.LocalArtistPage
import dev.toastbits.composekit.components.utils.composable.crossfade.EmptyListAndDataCrossfade
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.`artist_$x_songs`
import spmp.shared.generated.resources.library_no_items_match_filter
import spmp.shared.generated.resources.library_no_liked_artists
import spmp.shared.generated.resources.library_no_local_artists

class LibraryArtistsPage(context: AppContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        Icons.Default.Groups

    override fun enableSorting(): Boolean = false
    override fun canShowAltContent(): Boolean = true

    private var liked_artists: List<Artist>? by mutableStateOf(null)
    private var load_error: Throwable? by mutableStateOf(null)
    private var loaded: Boolean by mutableStateOf(false)

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        showing_alt_content: Boolean,
        modifier: Modifier,
    ) {
        val player: PlayerState = LocalPlayerState.current
        val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

        val downloads: List<DownloadStatus> by rememberSongDownloads()
        var sorted_artists: List<Pair<ArtistRef, Int>> by remember { mutableStateOf(emptyList()) }

        val sorted_liked_artists: List<Artist>? = liked_artists?.let {
            library_page.sort_type.sortAndFilterItems(it, library_page.search_filter, player.database, library_page.reverse_sort)
        }

        LaunchedEffect(Unit) {
            liked_artists = null
            load_error = null
            loaded = false
        }

        with(library_page) {
            LaunchedEffect(downloads, search_filter, sort_type, reverse_sort) {
                val filter = if (search_filter?.isNotEmpty() == true) search_filter else null
                val artists: MutableList<Pair<ArtistRef, Int>> = mutableListOf()

                for (download in downloads) {
                    if (download.progress != 1f) {
                        continue
                    }

                    val artist: ArtistRef = download.song.Artists.get(player.database)?.firstOrNull() ?: continue
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

        CompositionLocalProvider(LocalPlayerClickOverrides provides click_overrides.copy(
            onClickOverride = { item, index ->
                if (showing_alt_content) {
                    click_overrides.onMediaItemClicked(item, player)
                    return@copy
                }

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
                            LocalArtistPage(
                                item.item as Artist,
                                previous_item = previous_item?.item,
                                content_padding = content_padding,
                                multiselect_context = multiselect_context
                            )
                        }
                    }
                )
            }
        )) {
            EmptyListAndDataCrossfade(
                if (showing_alt_content) sorted_liked_artists?.map { Pair(it, 0) } ?: emptyList() else sorted_artists,
                loaded
            ) { artists, loaded ->
                multiselect_context.CollectionToggleButton(
                    artists?.mapIndexed { index, item -> Pair(item.first, index) } ?: emptyList(),
                    show = false
                )

                ScrollBarLazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = content_padding,
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    item {
                        LibraryPageTitle(stringResource(MediaItemType.ARTIST.getReadable(true)))
                    }

                    if (artists == null) {
                        val error: Throwable? = load_error
                        if (error != null) {
                            item {
                                ErrorInfoDisplay(
                                    error,
                                    onDismiss = {
                                        load_error = null
                                    }
                                )
                            }
                        }
                        else {
                            val text: StringResource? =
                                if (library_page.search_filter != null) Res.string.library_no_items_match_filter
                                else if (showing_alt_content) if (loaded) Res.string.library_no_liked_artists else null
                                else Res.string.library_no_local_artists

                            if (text != null) {
                                item {
                                    Text(
                                        stringResource(text),
                                        Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    else {
                        itemsIndexed(artists, { _, item -> item }) { index, artist_data ->
                            val (artist, item_count) = artist_data

                            MediaItemPreviewLong(
                                artist,
                                Modifier.height(75.dp).fillMaxWidth(),
                                multiselect_context = multiselect_context,
                                show_type = false,
                                show_play_count = true,
                                font_size = 18.sp,
                                title_lines = 3,
                                getExtraInfo = {
                                    listOf(
                                        stringResource(Res.string.`artist_$x_songs`).replace("\$x", item_count.toString())
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun RowOrColumnScope.SideContent(showing_alt_content: Boolean) {
        val player: PlayerState = LocalPlayerState.current
        val auth_state: ApiAuthenticationState? =
            if (showing_alt_content) player.context.ytapi.user_auth_state
            else null

        val liked_artists_endpoint = auth_state?.LikedArtists ?: return
        if (liked_artists_endpoint.isImplemented()) {
            LoadActionIconButton(
                {
                    liked_artists_endpoint.getLikedArtists().fold(
                        { liked_artists = it.map { it.toArtistData() } },
                        { load_error = it }
                    )
                    loaded = true
                },
                loadOnLaunch = true
            ) {
                Icon(Icons.Default.Refresh, null)
            }
        }
    }
}
