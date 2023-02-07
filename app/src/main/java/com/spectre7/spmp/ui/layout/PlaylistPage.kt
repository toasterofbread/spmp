@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.*
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun PlaylistPage(
    pill_menu: PillMenu,
    playlist: Playlist,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    var show_info by remember { mutableStateOf(false) }

    val share_intent = remember(playlist.url, playlist.title) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, playlist.title)
            putExtra(Intent.EXTRA_TEXT, playlist.url)
            type = "text/plain"
        }, null)
    }
    val open_intent: Intent? = remember(playlist.url) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playlist.url))
        if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
            null
        }
        else {
            intent
        }
    }

    val gradient_size = 0.35f
    val background_colour = MainActivity.theme.getBackground(false)
    var accent_colour by remember { mutableStateOf(Color.Unspecified) }

    var playlist_rows_loaded: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(playlist.id) {
        playlist_rows_loaded = false

        thread {
            runBlocking {
                withContext(Dispatchers.IO) { coroutineScope {
                    for (layout in playlist.feed_layouts) {
                        for (item in layout.items.withIndex()) {
                            launch {
                                val new_item = item.value.loadData()
                                if (new_item != item.value) {
                                    synchronized(layout.items) {
                                        layout.items[item.index] = new_item
                                    }
                                }
                            }
                        }
                    }
                }}

                playlist.feed_layouts.removeInvalid()
                playlist_rows_loaded = true
            }
        }
    }

    LaunchedEffect(accent_colour) {
        if (!accent_colour.isUnspecified) {
            pill_menu.setBackgroundColourOverride(accent_colour)
        }
    }

    BackHandler(onBack = close)

    if (show_info) {
        InfoDialog(playlist) { show_info = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 20.dp + getStatusBarHeight()),
        verticalArrangement = Arrangement.Top
    ) {

        ElevatedCard(Modifier.fillMaxWidth().height(150.dp)) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Crossfade(playlist.getThumbnail(MediaItem.ThumbnailQuality.HIGH)) { thumbnail ->
                    if (thumbnail == null) {
                        CircularProgressIndicator(color = MainActivity.theme.getAccent())
                    }
                    else {
                        if (accent_colour.isUnspecified) {
                            accent_colour = playlist.getDefaultThemeColour() ?: MainActivity.theme.getAccent()
                        }

                        Image(
                            thumbnail.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12))
                        )
                    }
                }

                Column {
                    Text(playlist.title)

                    if (playlist.artist != null) {
                        playlist.artist!!.PreviewLong(MainActivity.theme.getOnBackgroundProvider(false), playerProvider, true, Modifier)
                    }
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {

            // Image spacing
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .background(
                            Brush.verticalGradient(
                                1f - gradient_size to Color.Transparent,
                                1f to background_colour
                            )
                        )
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Marquee(false) {
                        Text(playlist.title, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp, softWrap = false)
                    }
                }
            }

            val content_padding = 10.dp

            // Action bar
            item {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .background(background_colour),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = content_padding)
                ) {

                    fun chip(text: String, icon: ImageVector, onClick: () -> Unit) {
                        item {
                            ElevatedAssistChip(
                                onClick,
                                { Text(text, style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = {
                                    Icon(icon, null, tint = accent_colour)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = background_colour,
                                    labelColor = MainActivity.theme.getOnBackground(false),
                                    leadingIconContentColor = accent_colour
                                )
                            )
                        }
                    }

                    chip(getString(R.string.playlist_chip_shuffle), Icons.Outlined.Shuffle) { TODO() }
                    chip(getString(R.string.action_share), Icons.Outlined.Share) { MainActivity.context.startActivity(share_intent) }
                    chip(getString(R.string.playlist_chip_open), Icons.Outlined.OpenInNew) { MainActivity.context.startActivity(open_intent) }
                    chip(getString(R.string.playlist_chip_details), Icons.Outlined.Info) { show_info = !show_info }
                }
            }

            item {
                Row(Modifier
                    .fillMaxWidth()
                    .background(background_colour)
                    .padding(start = 20.dp, bottom = 10.dp)) {
                    @Composable
                    fun Btn(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
                        OutlinedButton(onClick = onClick, modifier.height(45.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, null, tint = accent_colour)
                                Text(text, softWrap = false, color = MainActivity.theme.getOnBackground(false))
                            }
                        }
                    }

                    Btn(getString(R.string.playlist_chip_play), Icons.Outlined.PlayArrow,
                        Modifier
                            .fillMaxWidth(0.5f)
                            .weight(1f)) { TODO() }
                    Spacer(Modifier.requiredWidth(20.dp))
                    Btn(getString(R.string.playlist_chip_radio), Icons.Outlined.Radio,
                        Modifier
                            .fillMaxWidth(1f)
                            .weight(1f)) { TODO() }
                }
            }

            // Loaded items
            item {
                Crossfade(playlist_rows_loaded) { loaded ->
                    if (!loaded) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(background_colour)
                                .padding(content_padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent_colour)
                        }
                    }
                    else {
                        MediaItemLayoutColumn(
                            playlist.feed_layouts,
                            playerProvider,
                            Modifier
                                .background(background_colour)
                                .fillMaxSize()
                                .padding(content_padding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(playlist: Playlist, close: () -> Unit) {
    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text("Close")
            }
        },
        title = { Text("Playlist info") },
        text = {
            @Composable
            fun InfoValue(name: String, value: String) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                        Box(Modifier.fillMaxWidth()) {
                            Marquee(false) {
                                Text(value, softWrap = false)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End) {
                        val clipboard = LocalClipboardManager.current
                        IconButton({
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
                        IconButton({
                            MainActivity.context.startActivity(share_intent)
                        }) {
                            Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                InfoValue("Name", playlist.title)
                InfoValue("Id", playlist.id)
                InfoValue("Url", playlist.url)
            }
        }
    )
}