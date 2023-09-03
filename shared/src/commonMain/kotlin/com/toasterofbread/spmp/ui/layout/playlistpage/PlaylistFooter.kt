package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.launch

@Composable
fun PlaylistPage.PlaylistFooter(
    items: List<Pair<MediaItem, Int>>?,
    loading: Boolean,
    load_error: Throwable?,
    modifier: Modifier = Modifier
) {
    val remote_playlist: RemotePlaylist? = playlist as? RemotePlaylist
    val continuation: MediaItemLayout.Continuation? = remote_playlist?.Continuation?.observe(player.context.database)?.value

    Crossfade(
        if (load_error != null) load_error
        else if (continuation != null) continuation
        else if (loading) true
        else if (items?.isEmpty() == true) false
        else null,
        modifier
    ) { state ->
        when (state) {
            is Throwable -> {
                ErrorInfoDisplay(
                    state,
                    Modifier.fillMaxWidth(),
                    expanded_modifier = Modifier.height(500.dp),
                    message = "Playlist load failed"
                )
            }
            false -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(getString("playlist_empty"))
                }
            }
            is MediaItemLayout.Continuation, true -> {
                remote_playlist?.also { remote ->
                    Box(Modifier.fillMaxSize().heightIn(min = 50.dp), contentAlignment = Alignment.Center) {
                        if (state is MediaItemLayout.Continuation) {
                            Button({
                                coroutine_scope.launch {
                                    MediaItemLoader.loadRemotePlaylist(remote.getEmptyData(), player.context, state)
                                }
                            }) {
                                if (loading) {
                                    SubtleLoadingIndicator()
                                }
                                else {
                                    Icon(Icons.Default.KeyboardArrowDown, null)
                                }
                            }
                        }
                        else {
                            SubtleLoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}
