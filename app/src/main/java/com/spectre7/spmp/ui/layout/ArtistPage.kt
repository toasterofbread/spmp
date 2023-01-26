@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.sendToast

@Composable
fun ArtistPage(pill_menu: PillMenu, artist: Artist) {
    var show_info by remember { mutableStateOf(false) }
    val share_intent = remember(artist.url, artist.name) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, artist.name)
            putExtra(Intent.EXTRA_TEXT, artist.url)
            type = "text/plain"
        }, null)
    }

    LaunchedEffect(Unit) {
        pill_menu.addExtraAction {
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Share
                ) {
                    MainActivity.context.startActivity(share_intent)
                }
            }
        }
        pill_menu.addExtraAction {
            if (it == 1) {
                ActionButton(
                    Icons.Filled.Info
                ) {
                    show_info = true
                }
            }
        }
    }

    if (show_info) {
        InfoDialog(artist) { show_info = false }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Image(
            painter = rememberAsyncImagePainter(artist.getThumbUrl(true)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(artist.name, fontSize = 40.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(artist: Artist, close: () -> Unit) {
    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text("Close")
            }
        },
        title = { Text("Artist info") },
        text = {
            @Composable
            fun InfoValue(name: String, value: String) {
                Column {
                    Text(name, style = MaterialTheme.typography.labelLarge)
                    
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(value)

                        OutlinedCard {
                            Row {
                                val clipboard = LocalClipboardManager.current
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(value))
                                    sendToast("Copied ${name.lowercase()} to clipboard")
                                }) {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                                }

                                val share_intent = Intent.createChooser(Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, value)
                                    type = "text/plain"
                                }, null)
                                IconButton(onClick = {
                                    MainActivity.context.startActivity(share_intent)
                                }) {
                                    Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            InfoValue("Name", artist.name)
            InfoValue("Id", artist.id)
            InfoValue("Url", artist.url)
        }
    )
}