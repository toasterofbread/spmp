package com.toasterofbread.spmp.ui.layout.library

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.model.mediaitem.playlist.createLocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.rememberLocalPlaylists
import com.toasterofbread.spmp.model.mediaitem.playlist.rememberOwnedPlaylists
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.launchSingle
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class LibraryPlaylistsPage: LibrarySubPage {
    override fun getIcon(): ImageVector =
        MediaItemType.PLAYLIST_REM.getIcon()

    override fun getTitle(): String =
        getString("library_tab_playlists")

    @Composable
    override fun Page(
        library_page: LibraryPage,
        content_padding: PaddingValues,
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier
    ) {
        val player = LocalPlayerState.current
        val api = player.context.ytapi

        val load_coroutine_scope = rememberCoroutineScope()
        val coroutine_scope = rememberCoroutineScope()

        var loading: Boolean by remember { mutableStateOf(false) }
        var load_error: Throwable? by remember { mutableStateOf(null) }

        val local_playlists: List<LocalPlaylistData> = rememberLocalPlaylists(player.context) ?: emptyList()
        val account_playlists: List<RemotePlaylistRef> = api.user_auth_state?.own_channel?.let { own_channel ->
            rememberOwnedPlaylists(own_channel, player.context)
        } ?: emptyList()

        suspend fun loadAccountPlaylists() {
            coroutineContext.job.invokeOnCompletion {
                loading = false
            }
            loading = true
            load_error = null

            val endpoint = api.user_auth_state?.AccountPlaylists
            if (endpoint != null) {
                val result = endpoint.getAccountPlaylists()
                load_error = result.exceptionOrNull()
            }
        }

        LaunchedEffect(api.user_auth_state?.AccountPlaylists) {
            if (account_playlists.isNotEmpty()) {
                return@LaunchedEffect
            }

            load_coroutine_scope.launchSingle {
                loadAccountPlaylists()
            }
        }

        val item_spacing: Dp = 15.dp
        LazyVerticalGrid(
            GridCells.Adaptive(100.dp),
            modifier,
            contentPadding = content_padding,
            verticalArrangement = Arrangement.spacedBy(item_spacing),
            horizontalArrangement = Arrangement.spacedBy(item_spacing)
        ) {
            PlaylistItems(
                "Local playlists",
                local_playlists,
                multiselect_context,
                cornerContent = {
                    IconButton({
                        coroutine_scope.launch {
                            createLocalPlaylist(player.context)
                        }
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            )

            PlaylistItems(
                "Account playlists",
                account_playlists,
                multiselect_context,
                belowHeaderContent =
                    load_error?.let { error ->
                        {
                            ErrorInfoDisplay(
                                error,
                                onDismiss = {
                                    load_error = null
                                }
                            )
                        }
                    },
                getLoading = {
                    loading
                },
                cornerContent = {
                    AnimatedVisibility(!loading) {
                        IconButton({
                            load_coroutine_scope.launchSingle {
                                loadAccountPlaylists()
                            }
                        }) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    }
                }
            )
        }
    }
}

fun LazyGridScope.spanItem(key: Any? = null, contentType: Any? = null, content: @Composable LazyGridItemScope.() -> Unit) {
    item(
        key,
        { GridItemSpan(Int.MAX_VALUE) },
        contentType,
        content
    )
}

private fun LazyGridScope.PlaylistItems(
    title: String,
    items: List<Playlist>,
    multiselect_context: MediaItemMultiSelectContext? = null,
    belowHeaderContent: (@Composable () -> Unit)? = null,
    cornerContent: (@Composable () -> Unit)? = null,
    getLoading: (() -> Boolean)? = null
) {
    spanItem {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                Modifier.padding(bottom = 5.dp).fillMaxWidth().weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )

            Box(contentAlignment = Alignment.Center) {

                val loading = getLoading?.invoke() == true
                this@Row.AnimatedVisibility(
                    loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SubtleLoadingIndicator()
                }

                cornerContent?.invoke()
            }
        }
    }

    if (belowHeaderContent != null) {
        spanItem {
            belowHeaderContent()
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
