package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.durationToString
import com.toasterofbread.utils.composable.WidthShrinkText

@Composable
internal fun PlaylistButtonBar(
    playlist: Playlist,
    accent_colour: Color,
    editing_info: Boolean,
    setEditingInfo: (Boolean) -> Unit
) {
    val player = LocalPlayerState.current

    Crossfade(editing_info) { editing ->
        if (editing) {
            TopInfoEditButtons(playlist, accent_colour, Modifier.fillMaxWidth()) {
                setEditingInfo(false)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ player.playMediaItem(playlist, true) }) {
                    Icon(Icons.Default.Shuffle, null)
                }
                Crossfade(playlist.pinned_to_home) { pinned ->
                    IconButton({ playlist.setPinnedToHome(!pinned) }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }
                playlist.url?.also { url ->
                    if (SpMp.context.canShare()) {
                        IconButton({ SpMp.context.shareText(url, playlist.title!!) }) {
                            Icon(Icons.Default.Share, null)
                        }
                    }
                }

                IconButton({ setEditingInfo(true) }) {
                    Icon(Icons.Default.Edit, null)
                }

                playlist.items?.also {
                    PlaylistInfoText(playlist, it, Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlaylistInfoText(playlist: Playlist, items: List<MediaItem>, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val item_count = playlist.item_count ?: items.size
        if (item_count > 0) {
            val text = remember(playlist.total_duration, item_count) {
                val duration_text =
                    if (playlist.total_duration == null) ""
                    else durationToString(
                        playlist.total_duration!!,
                        short = true,
                        hl = SpMp.ui_language
                    ) + " • "

                duration_text + getString("playlist_x_songs").replace("\$x", item_count.toString())
            }

            WidthShrinkText(
                text,
                Modifier.fillMaxWidth(),
                alignment = TextAlign.Right,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
