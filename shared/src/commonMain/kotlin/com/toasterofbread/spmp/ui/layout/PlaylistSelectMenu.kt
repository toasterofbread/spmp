package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.platform.composable.SwipeRefresh
import dev.toastbits.composekit.util.addUnique
import dev.toastbits.composekit.util.platform.launchSingle
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.library.rememberLocalPlaylists
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.rememberOwnedPlaylists
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import dev.toastbits.composekit.theme.core.onAccent

@Composable
fun PlaylistSelectMenu(
    selected: SnapshotStateList<Playlist>,
    auth_state: ApiAuthenticationState?,
    modifier: Modifier = Modifier
) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }

    val local_playlists: List<Playlist> = MediaItemLibrary.rememberLocalPlaylists(player.context) ?: emptyList()
    val account_playlists: List<Playlist>? = auth_state?.own_channel_id?.let { channel_id ->
        rememberOwnedPlaylists(channel_id, player.context)
    }

    fun refreshAccountPlaylists() {
        val playlists_endpoint = auth_state?.AccountPlaylists
        if (playlists_endpoint?.isImplemented() != true) {
            return
        }

        coroutine_scope.launchSingle {
            loading = true
            val result = playlists_endpoint.getAccountPlaylists()
            result.onFailure { error ->
                player.context.sendToast(error.toString())
            }
            loading = false
        }
    }

    LaunchedEffect(auth_state) {
        refreshAccountPlaylists()
    }

    CompositionLocalProvider(
        LocalPlayerClickOverrides provides LocalPlayerClickOverrides.current.copy(
            onClickOverride = { item, _ ->
                check(item is Playlist)

                val index = selected.indexOf(item)
                if (index != -1) {
                    selected.removeAt(index)
                }
                else {
                    selected.add(item)
                }
            }
        )
    ) {
        SwipeRefresh(
            loading,
            { refreshAccountPlaylists() },
            modifier,
            swipe_enabled = auth_state?.AccountPlaylists?.isImplemented() == true
        ) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(local_playlists) { playlist ->
                    PlaylistItem(selected, playlist)
                }
                items(account_playlists ?: emptyList()) { playlist ->
                    PlaylistItem(selected, playlist)
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(selected: SnapshotStateList<Playlist>, playlist: Playlist) {
    val player = LocalPlayerState.current

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
                checkedColor = player.theme.accent,
                checkmarkColor = player.theme.onAccent
            )
        )
        MediaItemPreviewLong(playlist)
    }
}
