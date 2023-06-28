package com.spectre7.spmp.ui.layout.artistpage

import androidx.compose.foundation.layout.*
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.mediaitem.Playlist
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.utils.composable.Marquee

@Composable
fun InfoDialog(item: MediaItem, close: () -> Unit) {
    PlatformAlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text(getString("action_close"))
            }
        },
        title = { Text(getStringTODO(when (item) {
            is Artist -> "Artist info"
            is Playlist -> "Playlist info"
            else -> throw NotImplementedError(item.type.toString())
        })) },
        text = {
            @Composable
            fun InfoValue(name_key: String, value: String) {
                val name = getStringTODO(name_key)
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)) {
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
                            SpMp.context.sendToast(getString("notif_copied_x_to_clipboard").replace("\$x", name.lowercase()))
                        }) {
                            Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                        }

                        if (SpMp.context.canShare()) {
                            IconButton({
                                SpMp.context.shareText(value)
                            }) {
                                Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                InfoValue("Name", item.title ?: "")
                InfoValue("Id", item.id)
                InfoValue("Url", item.url.toString())
            }
        }
    )
}
