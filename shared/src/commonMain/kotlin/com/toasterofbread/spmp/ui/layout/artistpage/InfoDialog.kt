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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.utils.composable.Marquee

@Composable
fun InfoDialog(item: MediaItem, close: () -> Unit) {
    val player = LocalPlayerState.current
    
    PlatformAlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text(getString("action_close"))
            }
        },
        title = { 
            Text(
                getStringTODO(
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
                val name = getStringTODO(name_key)
                
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
                        val clipboard = LocalClipboardManager.current
                        IconButton({
                            clipboard.setText(AnnotatedString(value))
                            player.context.sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", name.lowercase()))
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
                val item_title: String? by item.Title.observe(player.context.database)
                InfoValue("Name", item_title ?: "")
                InfoValue("Id", item.id)
                InfoValue("Url", item.getURL(player.context))
            }
        }
    )
}
