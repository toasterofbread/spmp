package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.LocalPlaylist
import com.toasterofbread.spmp.model.mediaitem.MediaItemPreviewParams
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.utils.addUnique

@Composable
fun PlaylistSelectMenu(
    selected: SnapshotStateList<Playlist>,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    val local_playlists = LocalPlaylist.rememberLocalPlaylistsListener()
    val account_playlists = Api.ytm_auth.own_playlists
    var loading by remember { mutableStateOf(false) }
    val coroutine_scope = rememberCoroutineScope()

    fun refreshAccountPlaylists() {
        coroutine_scope.launchSingle {
            if (!Api.ytm_auth.own_playlists_loaded) {
                loading = true
                val result = Api.ytm_auth.loadOwnPlaylists()
                result.onFailure { error ->
                    SpMp.context.sendToast(error.toString())
                }
            }
            loading = false
        }
    }

    LaunchedEffect(Api.ytm_auth) {
        refreshAccountPlaylists()
    }

    CompositionLocalProvider(LocalPlayerState provides remember {
        player.copy(onClickedOverride = { item, _ ->
            check(item is Playlist)
            
            val index = selected.indexOf(item)
            if (index != -1) {
                selected.removeAt(index)
            }
            else {
                selected.add(item)
            }
        })
    }) {
        SwipeRefresh(
            loading,
            { refreshAccountPlaylists() },
            modifier
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(local_playlists) { playlist ->
                    PlaylistItem(selected, playlist)
                }
                items(account_playlists) { playlist ->
                    PlaylistItem(selected, AccountPlaylist.fromId(playlist))
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(selected: SnapshotStateList<Playlist>, playlist: Playlist) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Checkbox(
            selected.contains(playlist),
            { checked ->
                if (checked) {
                    selected.addUnique(playlist)
                }
                else {
                    selected.remove(playlist)
                }
            },
            colors = CheckboxDefaults.colors(
                uncheckedColor = LocalContentColor.current,
                checkedColor = Theme.current.accent,
                checkmarkColor = Theme.current.on_accent
            )
        )
        playlist.PreviewLong(MediaItemPreviewParams())
    }
}
