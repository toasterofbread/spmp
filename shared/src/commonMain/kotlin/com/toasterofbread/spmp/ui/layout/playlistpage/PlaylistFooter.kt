package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.getContrasted
import kotlinx.coroutines.launch

@Composable
fun PlaylistFooter(playlist: Playlist, loading: Boolean, modifier: Modifier = Modifier) {
    val db = LocalPlayerState.current.context.database
    val continuation: MediaItemLayout.Continuation? by playlist.Continuation.observe(db)
    val coroutine_scope = rememberCoroutineScope()

    Crossfade(
        Pair(loading, continuation),
        modifier
    ) {
        val (playlist_loading, playlist_continuation) = it

        if (loading || playlist_continuation != null) {
            Box(Modifier.fillMaxSize().heightIn(min = 50.dp), contentAlignment = Alignment.Center) {
                if (playlist_continuation != null) {
                    Button({
                        coroutine_scope.launch {
                            MediaItemLoader.loadPlaylist(playlist.getEmptyData(), db, playlist_continuation)
                        }
                    }) {
                        if (playlist_loading) {
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
