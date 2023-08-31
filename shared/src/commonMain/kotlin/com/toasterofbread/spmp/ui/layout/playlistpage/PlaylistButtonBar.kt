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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
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

    var playlist_pinned: Boolean by playlist.observePinnedToHome(player.context)

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
                
                Crossfade(playlist_pinned) { pinned ->
                    IconButton({ playlist_pinned = !pinned }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }

                if (player.context.canShare()) {
                    IconButton({ 
                        player.context.shareText(
                            playlist.getURL(player.context), 
                            playlist.Title.get(player.database) ?: ""
                        ) 
                    }) {
                        Icon(Icons.Default.Share, null)
                    }
                }

                IconButton({ setEditingInfo(true) }) {
                    Icon(Icons.Default.Edit, null)
                }

                val playlist_items: List<MediaItem>? by playlist.Items.observe(player.database)
                playlist_items?.also { items ->
                    PlaylistInfoText(playlist, items, Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlaylistInfoText(playlist: Playlist, items: List<MediaItem>, modifier: Modifier = Modifier) {
    val db = LocalPlayerState.current.context.database

    val item_count: Int = playlist.ItemCount.observe(db).value ?: items.size
    val total_duration: Long? by playlist.TotalDuration.observe(db)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (item_count > 0) {
            val text = remember(total_duration, item_count) {
                val duration_text = total_duration.let { duration ->
                    if (duration == null) ""
                    else durationToString(
                        duration,
                        short = true,
                        hl = SpMp.ui_language
                    ) + " • "
                }

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
