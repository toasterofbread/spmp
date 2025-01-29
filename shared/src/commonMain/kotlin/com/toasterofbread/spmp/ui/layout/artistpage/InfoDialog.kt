package com.toasterofbread.spmp.ui.layout.artistpage

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.observeUrl
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.resources.stringResourceTODO
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.composekit.components.utils.composable.Marquee
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.notif_copied_x_to_clipboard

@Composable
fun ArtistInfoDialog(item: MediaItem, close: () -> Unit) {
    val player: PlayerState = LocalPlayerState.current

    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
        title = {
            Text(
                stringResourceTODO(
                    when (item) {
                        is Artist -> "Artist info"
                        is RemotePlaylist -> "Playlist info"
                        else -> throw NotImplementedError(item.getType().toString())
                    }
                )
            )
        },
        text = {
            @Composable
            fun InfoValue(name_key: String, value: String) {
                val name = stringResourceTODO(name_key)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                        Box(Modifier.fillMaxWidth()) {
                            Marquee {
                                Text(value, softWrap = false)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End) {
                        val clipboard: ClipboardManager = LocalClipboardManager.current
                        val notif_copied_x_to_clipboard: String = stringResource(Res.string.notif_copied_x_to_clipboard)

                        IconButton({
                            clipboard.setText(AnnotatedString(value))
                            player.context.sendToast(notif_copied_x_to_clipboard.replace("\$x", name.lowercase()))
                        }) {
                            Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                        }

                        if (player.context.canShare()) {
                            IconButton({
                                player.context.shareText(value)
                            }) {
                                Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                val item_title: String? by item.observeActiveTitle()
                InfoValue("Name", item_title ?: "")
                InfoValue("Id", item.id)
                InfoValue("Url", item.observeUrl())
            }
        }
    )
}
