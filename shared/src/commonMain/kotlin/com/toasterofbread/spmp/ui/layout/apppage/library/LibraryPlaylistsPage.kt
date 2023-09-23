package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.library.rememberLocalPlaylists
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.createOwnedPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.rememberOwnedPlaylists
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.CreateAccountPlaylistEndpoint
import com.toasterofbread.utils.composable.LoadActionIconButton
import com.toasterofbread.utils.composable.spanItem

internal class LibraryPlaylistsPage(context: PlatformContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        MediaItemType.PLAYLIST_REM.getIcon()

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier
    ) {
        val player = LocalPlayerState.current
        val api = player.context.ytapi

        var load_error: Throwable? by remember { mutableStateOf(null) }

        val local_playlists: List<LocalPlaylistData> = MediaItemLibrary.rememberLocalPlaylists(player.context) ?: emptyList()
        val account_playlists: List<RemotePlaylistRef>? = api.user_auth_state?.own_channel?.let { own_channel ->
            rememberOwnedPlaylists(own_channel, player.context)
        }

        val sorted_local_playlists = library_page.sort_type.sortAndFilterItems(local_playlists, library_page.search_filter, player.database, library_page.reverse_sort)
        val sorted_account_playlists = account_playlists?.let { playlists ->
            library_page.sort_type.sortAndFilterItems(playlists, library_page.search_filter, player.database, library_page.reverse_sort)
        }

        val item_spacing: Dp = 15.dp
        val auth_state: YoutubeApi.UserAuthState? = player.context.ytapi.user_auth_state

        LazyVerticalGrid(
            GridCells.Adaptive(100.dp),
            modifier,
            contentPadding = content_padding,
            verticalArrangement = Arrangement.spacedBy(item_spacing),
            horizontalArrangement = Arrangement.spacedBy(item_spacing)
        ) {
            load_error?.also { error ->
                spanItem {
                    ErrorInfoDisplay(error, Modifier.fillMaxWidth()) {
                        load_error = null
                    }
                }
            }

            PlaylistItems(
                "Local playlists",
                sorted_local_playlists,
                multiselect_context,
                cornerContent = {
                    LoadActionIconButton({
                        MediaItemLibrary.createLocalPlaylist(player.context)
                            .onFailure {
                                load_error = it
                            }
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            )

            if (sorted_account_playlists != null) {
                PlaylistItems(
                    "Account playlists",
                    sorted_account_playlists,
                    multiselect_context,
                    cornerContent = {
                        Row {
                            val load_endpoint = auth_state?.AccountPlaylists
                            if (load_endpoint?.isImplemented() == true) {
                                LoadActionIconButton(
                                    {
                                        val result = load_endpoint.getAccountPlaylists()
                                        load_error = result.exceptionOrNull()
                                    },
                                    load_on_launch = account_playlists.isEmpty()
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                }
                            }

                            val create_endpoint: CreateAccountPlaylistEndpoint? = auth_state?.CreateAccountPlaylist

                            if (create_endpoint?.isImplemented() == true) {
                                LoadActionIconButton({
                                    MediaItemLibrary.createOwnedPlaylist(auth_state, create_endpoint)
                                        .onFailure {
                                            load_error = it
                                        }
                                }) {
                                    Icon(Icons.Default.Add, null)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun LazyGridScope.PlaylistItems(
    title: String,
    items: List<Playlist>,
    multiselect_context: MediaItemMultiSelectContext? = null,
    cornerContent: (@Composable () -> Unit)? = null
) {
    spanItem {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                Modifier.padding(bottom = 5.dp).fillMaxWidth().weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )

            cornerContent?.invoke()
        }
    }

    if (items.isEmpty()) {
        spanItem {
            Text(getString("library_no_playlists"), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }

    items(items) { playlist ->
        MediaItemPreviewSquare(playlist, multiselect_context = multiselect_context)
    }

    spanItem {
        Spacer(Modifier.height(15.dp))
    }
}
