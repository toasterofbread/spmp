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
import com.spectre7.spmp.model.LocalPlaylist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.LongPressMenuActionProvider
import com.spectre7.utils.launchSingle

@Composable
fun PlaylistSelectMenu(modifier: Modifier = Modifier, show_cancel_button: Boolean = false, onFinished: (playlist: Playlist?, new: Boolean) -> Unit) {
    val player = LocalPlayerState.current
    val coroutine_scope = rememberCoroutineScope()

    val playlists = LocalPlaylist.rememberLocalPlaylistsListener()

    BackHandler {
        onFinished(null, false)
    }

    LazyColumn(modifier) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                // Create new
                LongPressMenuActionProvider.ActionButton(
                    Icons.Default.Add,
                    getString("playlist_create"),
                    fill_width = false,
                    onClick = {
                        coroutine_scope.launchSingle {
                            val playlist = LocalPlaylist.createLocalPlaylist(SpMp.context)
                            onFinished(playlist, true)
                        }
                    },
                    onAction = {}
                )

                // Cancel
                if (show_cancel_button) {
                    LongPressMenuActionProvider.ActionButton(
                        Icons.Default.Close,
                        getString("action_cancel"),
                        fill_width = false,
                        onClick = { onFinished(null, false) },
                        onAction = {}
                    )
                }
            }
        }

        items(playlists) { playlist ->
            CompositionLocalProvider(LocalPlayerState provides remember {
                player.copy(onClickedOverride = { playlist, _ ->
                    coroutine_scope.launchSingle {
                        check(playlist is Playlist)
                        onFinished(playlist, false)
                    }
                })
            }) {
                playlist.PreviewLong(MediaItem.PreviewParams())
            }
        }
    }
}
