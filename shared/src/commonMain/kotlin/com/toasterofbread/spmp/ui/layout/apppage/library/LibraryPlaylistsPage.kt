package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyVerticalGrid
import com.toasterofbread.composekit.utils.composable.LoadActionIconButton
import com.toasterofbread.composekit.utils.composable.spanItem
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.getMediaItemPreviewSquareAdditionalHeight
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.library.rememberLocalPlaylists
import com.toasterofbread.spmp.model.mediaitem.playlist.*
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.CreateAccountPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.implementedOrNull

internal class LibraryPlaylistsPage(context: AppContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        MediaItemType.PLAYLIST_REM.getIcon()

    override fun canShowAltContent(): Boolean = true

    private var load_error: Throwable? by mutableStateOf(null)

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        showing_alt_content: Boolean,
        modifier: Modifier
    ) {
        val player: PlayerState = LocalPlayerState.current
        val api: YoutubeApi = player.context.ytapi

        val show_likes_playlist: Boolean by BehaviourSettings.Key.SHOW_LIKES_PLAYLIST.rememberMutableState()

        val local_playlists: List<LocalPlaylistData> = MediaItemLibrary.rememberLocalPlaylists(player.context) ?: emptyList()
        val account_playlists: List<RemotePlaylistRef>? = api.user_auth_state?.own_channel?.let { own_channel ->
            rememberOwnedPlaylists(own_channel, player.context)
        }

        val sorted_local_playlists = library_page.sort_type.sortAndFilterItems(local_playlists, library_page.search_filter, player.database, library_page.reverse_sort)
        val sorted_account_playlists = account_playlists?.let { playlists ->
            val filtered: List<RemotePlaylistRef> = if (show_likes_playlist) playlists else playlists.filter { it.id != "VLLM" }
            library_page.sort_type.sortAndFilterItems(filtered, library_page.search_filter, player.database, library_page.reverse_sort)
        }

        val item_spacing: Dp = 15.dp

        LaunchedEffect(Unit) {
            load_error = null
        }

        LaunchedEffect(showing_alt_content) {
            if (showing_alt_content && account_playlists.isNullOrEmpty()) {
                val auth_state: YoutubeApi.UserAuthState = player.context.ytapi.user_auth_state ?: return@LaunchedEffect

                val load_endpoint: AccountPlaylistsEndpoint = auth_state.AccountPlaylists
                if (load_endpoint.isImplemented()) {
                    val result = load_endpoint.getAccountPlaylists()
                    load_error = result.exceptionOrNull()
                }
            }
        }

        ScrollBarLazyVerticalGrid(
            GridCells.Adaptive(100.dp),
            modifier = modifier,
            contentPadding = content_padding,
            verticalArrangement = Arrangement.spacedBy(item_spacing),
            horizontalArrangement = Arrangement.spacedBy(item_spacing)
        ) {
            spanItem {
                LibraryPageTitle(MediaItemType.PLAYLIST_LOC.getReadable(true))
            }

            load_error?.also { error ->
                spanItem {
                    ErrorInfoDisplay(error, isDebugBuild(), Modifier.fillMaxWidth()) {
                        load_error = null
                    }
                }
            }

            if (showing_alt_content) {
                PlaylistItems(
                    sorted_account_playlists ?: emptyList(),
                    multiselect_context
                )
            }
            else {
                PlaylistItems(
                    sorted_local_playlists,
                    multiselect_context
                )
            }
        }
    }

    @Composable
    override fun SideContent(showing_alt_content: Boolean) {
        val player: PlayerState = LocalPlayerState.current
        val auth_state: YoutubeApi.UserAuthState? = player.context.ytapi.user_auth_state

        val load_endpoint: AccountPlaylistsEndpoint? = auth_state?.AccountPlaylists?.implementedOrNull()
        val create_endpoint: CreateAccountPlaylistEndpoint? = auth_state?.CreateAccountPlaylist?.implementedOrNull()

        AnimatedVisibility(showing_alt_content && load_endpoint != null) {
            LoadActionIconButton(
                {
                    val result = load_endpoint?.getAccountPlaylists()
                    load_error = result?.exceptionOrNull()
                }
            ) {
                Icon(Icons.Default.Refresh, null)
            }
        }

        AnimatedVisibility(!showing_alt_content || create_endpoint != null) {
            LoadActionIconButton({
                if (!showing_alt_content) {
                    MediaItemLibrary.createLocalPlaylist(player.context)
                        .onFailure {
                            load_error = it
                        }
                }
                else if (create_endpoint != null) {
                    MediaItemLibrary.createOwnedPlaylist(auth_state, create_endpoint)
                        .onFailure {
                            load_error = it
                        }
                }
            }) {
                Icon(Icons.Default.Add, null)
            }
        }
    }
}

private fun LazyGridScope.PlaylistItems(
    items: List<Playlist>,
    multiselect_context: MediaItemMultiSelectContext? = null
) {
    if (items.isEmpty()) {
        spanItem {
            Text(getString("library_no_playlists"), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }

    items(items) { playlist ->
        MediaItemPreviewSquare(
            playlist,
            Modifier.size(
                getDefaultMediaItemPreviewSize(false)
                + DpSize(0.dp, getMediaItemPreviewSquareAdditionalHeight(2, MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP.sp))
            ),
            multiselect_context = multiselect_context
        )
    }

    spanItem {
        Spacer(Modifier.height(15.dp))
    }
}
