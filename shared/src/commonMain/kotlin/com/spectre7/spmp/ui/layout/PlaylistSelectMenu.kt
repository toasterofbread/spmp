package com.spectre7.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.mediaitem.LocalPlaylist
import com.spectre7.spmp.model.mediaitem.MediaItemPreviewParams
import com.spectre7.spmp.model.mediaitem.Playlist
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.LongPressMenuActionProvider
import com.spectre7.utils.launchSingle

@Composable
fun PlaylistSelectMenu(
    selected: SnapshotStateList<Playlist>,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current

    val local_playlists = LocalPlaylist.rememberLocalPlaylistsListener()
    val account_playlists = Api.ytm_auth.own_playlists

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
        LazyColumn(modifier) {
            items(local_playlists) { playlist ->
                PlaylistItem(playlist)
            }
            items(account_playlists) { playlist ->
                PlaylistItem(playlist)
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
            }
        )
        playlist.PreviewLong(MediaItemPreviewParams())
    }
}
