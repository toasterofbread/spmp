package com.toasterofbread.spmp.ui.layout.playlistpage

import SpMp.isDebugBuild
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.radio.RadioContinuation
import kotlinx.coroutines.launch
import LocalPlayerState
import dev.toastbits.ytmkt.radio.BuiltInRadioContinuation
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.playlist_empty

@Composable
fun PlaylistAppPage.PlaylistFooter(
    items: List<MediaItem>?,
    accent_colour: Color,
    loading: Boolean,
    load_error: Throwable?,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)?,
    onContinue: ((BuiltInRadioContinuation) -> Unit)?
) {
    val player: PlayerState = LocalPlayerState.current
    val remote_playlist: RemotePlaylist? = playlist as? RemotePlaylist
    val continuation: BuiltInRadioContinuation? = remote_playlist?.Continuation?.observe(player.database)?.value

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
                    isDebugBuild(),
                    Modifier.fillMaxWidth(),
                    expanded_content_modifier = Modifier.height(500.dp),
                    message = "Playlist load failed",
                    onDismiss = null,
                    onRetry = onRetry,
                    getAccentColour = { accent_colour }
                )
            }
            false -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.playlist_empty))
                }
            }
            is RadioContinuation, true -> {
                onContinue?.also { onContinue ->
                    Box(Modifier.fillMaxSize().heightIn(min = 50.dp), contentAlignment = Alignment.Center) {
                        if (state is BuiltInRadioContinuation) {
                            Button({
                                onContinue(state)
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
