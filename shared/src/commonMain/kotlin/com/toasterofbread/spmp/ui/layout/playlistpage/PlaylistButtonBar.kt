package com.toasterofbread.spmp.ui.layout.playlistpage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.util.composable.WidthShrinkText
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.uistrings.durationToString
import LocalPlayerState
import com.toasterofbread.spmp.model.mediaitem.observeUrl
import com.toasterofbread.spmp.platform.observeUiLanguage
import dev.toastbits.composekit.util.model.Locale
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.playlist_x_songs

@Composable
internal fun PlaylistAppPage.PlaylistButtonBar(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    var playlist_pinned: Boolean by playlist.observePinnedToHome()

    Crossfade(edit_in_progress, modifier) { editing ->
        if (editing) {
            PlaylistTopInfoEditButtons(Modifier.fillMaxWidth())
        }
        else {
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
                    val playlist_url: String = playlist.observeUrl()
                    IconButton({
                        player.context.shareText(
                            playlist_url,
                            playlist.getActiveTitle(player.database) ?: ""
                        )
                    }) {
                        Icon(Icons.Default.Share, null)
                    }
                }

                IconButton({ beginEdit() }) {
                    Icon(Icons.Default.Edit, null)
                }

                val playlist_items: List<Song>? by playlist.Items.observe(player.database)
                PlaylistInfoText(playlist_items, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun PlaylistAppPage.PlaylistInfoText(items: List<Song>?, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val db = player.database

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
    ) {
        if (items != null) {
            val item_count: Int = playlist.ItemCount.observe(db).value ?: items.size
            val total_duration: Long? by playlist.TotalDuration.observe(db)

            if (item_count > 0) {
                val ui_language: Locale by player.context.observeUiLanguage()
                val playlist_x_songs: String = stringResource(Res.string.playlist_x_songs)

                val text: String = remember(total_duration, item_count, ui_language) {
                    var duration: Long = total_duration ?: 0
                    var incomplete_duration: Boolean = false

                    if (duration == 0L) {
                        if (items.size < item_count) {
                            incomplete_duration = true
                        }

                        for (item in items) {
                            val item_duration: Long? = item.Duration.get(db)
                            if (item_duration != null) {
                                duration += item_duration
                            }
                            else {
                                incomplete_duration = true
                            }
                        }
                    }

                    val duration_text: String =
                        if (duration == 0L) ""
                        else (
                            durationToString(
                                duration,
                                ui_language.toTag(),
                                short = true
                            )
                            + (if (incomplete_duration) "+" else "")
                            + " • "
                        )

                    duration_text + playlist_x_songs.replace("\$x", item_count.toString())
                }

                WidthShrinkText(
                    text,
                    Modifier.fillMaxWidth().weight(1f),
                    alignment = TextAlign.Right,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Icon(
                if (playlist is RemotePlaylist) Icons.Outlined.Cloud else Icons.Outlined.Storage,
                null,
                Modifier.scale(0.75f)
            )
        }
    }
}
