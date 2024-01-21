package com.toasterofbread.spmp.ui.layout.playlistpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField

@Composable
internal fun PlaylistPage.ThumbnailSelectionDialog(
    close: () -> Unit
) {
    var url_input_mode: Boolean by remember { mutableStateOf(false) }
    var url_input: String by remember(url_input_mode) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = close,
        confirmButton = {
            Row {
                Button(close) {
                    Text(getString("action_cancel"))
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                if (edited_image_url != null) {
                    IconButton({
                        setEditedImageUrl(null)
                        close()
                    }) {
                        Icon(Icons.Default.Refresh, null)
                    }
                }

                IconButton({
                    url_input_mode = !url_input_mode
                }) {
                    Crossfade(url_input_mode) { selecting_url ->
                        Icon(
                            if (selecting_url) Icons.Default.Image
                            else Icons.Default.Link,
                            null
                        )
                    }
                }

                AnimatedVisibility(url_input_mode) {
                    IconButton({
                        setEditedImageUrl(url_input)
                        close()
                    }) {
                        Icon(Icons.Default.Done, null)
                    }
                }
            }
        },
        title = {
            Text(getString("playlist_select_image"), style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Crossfade(url_input_mode) { selecting_url ->
                if (selecting_url) {
                    TextField(
                        url_input,
                        {
                            url_input = it
                        },
                        Modifier.appTextField(),
                        label = {
                            Text(getString("playlist_image_url_field_label"))
                        }
                    )
                }
                else {
                    val items: List<MediaItem>? by playlist.Items.observe(player.database)
                    if (items.isNullOrEmpty()) {
                        Text(getString("playlist_empty"), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    else {
                        CompositionLocalProvider(LocalPlayerState provides remember {
                            player.copy(
                                onClickedOverride = { item, _ ->
                                    setEditedImageUrl(
                                        item.ThumbnailProvider.get(player.database)
                                            ?.getThumbnailUrl(MediaItemThumbnailProvider.Quality.HIGH)
                                    )
                                    close()
                                }
                            )
                        }) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(items ?: emptyList()) { item ->
                                    MediaItemPreviewLong(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
