package com.toasterofbread.spmp.ui.layout.apppage.library

import LocalPlayerState
import SpMp
import SpMp.isDebugBuild
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.toastbits.composekit.components.utils.composable.LoadActionIconButton
import dev.toastbits.composekit.components.utils.composable.spanItem
import dev.toastbits.composekit.components.utils.composable.RowOrColumnScope
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.mediaitem.layout.getDefaultMediaItemPreviewSize
import com.toasterofbread.spmp.model.mediaitem.layout.getMediaItemPreviewSquareAdditionalHeight
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.toRemotePlaylistData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitempreview.MEDIA_ITEM_PREVIEW_SQUARE_LINE_HEIGHT_SP
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.endpoint.LikedAlbumsEndpoint
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.library_no_liked_albums

internal class LibraryAlbumsPage(context: AppContext): LibrarySubPage(context) {
    override fun getIcon(): ImageVector =
        Icons.Default.Album

    override fun isHidden(): Boolean =
        SpMp.player_state.context.ytapi.user_auth_state == null

    private var load_error: Throwable? by mutableStateOf(null)
    private var liked_albums: List<RemotePlaylistData>? by mutableStateOf(null)
    private var loaded: Boolean by mutableStateOf(true)

    @Composable
    override fun Page(
        library_page: LibraryAppPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        showing_alt_content: Boolean,
        modifier: Modifier
    ) {
        val player: PlayerState = LocalPlayerState.current
        val auth_state: ApiAuthenticationState = player.context.ytapi.user_auth_state ?: return

        val load_endpoint: LikedAlbumsEndpoint = auth_state.LikedAlbums
        if (!load_endpoint.isImplemented()) {
            return
        }

        val sorted_liked_albums: List<RemotePlaylistData> =
            library_page.sort_type.sortAndFilterItems(liked_albums ?: emptyList(), library_page.search_filter, player.database, library_page.reverse_sort)

        val item_spacing: Dp = 15.dp

        LaunchedEffect(Unit) {
            load_error = null
            liked_albums = null
            loaded = false
        }

        LazyVerticalGrid(
            GridCells.Adaptive(100.dp),
            modifier,
            contentPadding = content_padding,
            verticalArrangement = Arrangement.spacedBy(item_spacing),
            horizontalArrangement = Arrangement.spacedBy(item_spacing)
        ) {
            spanItem {
                LibraryPageTitle(PlaylistType.ALBUM.getReadable(true))
            }

            load_error?.also { error ->
                spanItem {
                    ErrorInfoDisplay(error, isDebugBuild(), Modifier.fillMaxWidth()) {
                        load_error = null
                    }
                }
            }

            PlaylistItems(
                sorted_liked_albums,
                multiselect_context
            )
        }
    }

    @Composable
    override fun RowOrColumnScope.SideContent(showing_alt_content: Boolean) {
        val player: PlayerState = LocalPlayerState.current
        val auth_state: ApiAuthenticationState = player.context.ytapi.user_auth_state ?: return

        val load_endpoint: LikedAlbumsEndpoint = auth_state.LikedAlbums
        if (!load_endpoint.isImplemented()) {
            return
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            LoadActionIconButton(
                {
                    load_endpoint.getLikedAlbums().fold(
                        { albums ->
                            load_error = null
                            liked_albums = albums.map { it.toRemotePlaylistData() }
                        },
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

    private fun LazyGridScope.PlaylistItems(
        items: List<Playlist>,
        multiselect_context: MediaItemMultiSelectContext? = null
    ) {
        if (items.isEmpty() && loaded) {
            spanItem {
                Text(stringResource(Res.string.library_no_liked_albums), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
}
