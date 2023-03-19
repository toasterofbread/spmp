@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.theme.Theme
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
    val background_colour = Theme.current.background
    var accent_colour by remember { mutableStateOf(Color.Unspecified) }

    var playlist_rows_loaded: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(playlist.id) {
        playlist_rows_loaded = false

        thread {
            if (playlist.feed_layouts == null) {
                playlist.loadData()
            }

            runBlocking {
                withContext(Dispatchers.IO) { coroutineScope {
                    for (layout in playlist.feed_layouts!!) {
                        for (item in layout.items.withIndex()) {
                            launch {
                                item.value.loadData().onSuccess { new_item ->
                                    if (new_item != item.value) {
                                        synchronized(layout.items) {
                                            layout.items[item.index] = new_item!!
                                        }
                                    }
                                }
                            }
                        }
                    }
                }}

                playlist.feed_layouts!!.removeInvalid()
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

    val content_padding = 10.dp

    @Composable
    fun TopContent() {
        Spacer(Modifier.height(getStatusBarHeight()))

        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Theme.current.on_background, RoundedCornerShape(15.dp))
                .padding(10.dp)
        ) {
            Row(Modifier.height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Crossfade(playlist.getThumbnail(MediaItem.ThumbnailQuality.HIGH)) { thumbnail ->
                    if (thumbnail == null) {
                        CircularProgressIndicator(color = Theme.current.accent)
                    }
                    else {
                        if (accent_colour.isUnspecified) {
                            accent_colour = playlist.getDefaultThemeColour() ?: Theme.current.accent
                        }

                        Image(
                            thumbnail.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12))
                        )
                    }
                }

                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                    Text(playlist.title ?: "", fontSize = 25.sp)

                    if (playlist.artist != null) {
                        playlist.artist!!.PreviewLong(Theme.current.on_background_provider, playerProvider, true, Modifier)
                    }
                }
            }

            if (playlist.description != null) {
                Text(playlist.description!!, fontSize = 15.sp)
            }
        }

        // Action bar
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
                            labelColor = Theme.current.on_background,
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

        Row(Modifier
            .fillMaxWidth()
            .background(background_colour)
            .padding(start = 20.dp, bottom = 10.dp)
        ) {
            @Composable
            fun Btn(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
                OutlinedButton(onClick = onClick, modifier.height(45.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(icon, null, tint = accent_colour)
                        Text(text, softWrap = false, color = Theme.current.on_background)
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
    Crossfade(playlist_rows_loaded) { loaded ->
        if (!loaded) {
            Column {
                TopContent()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(background_colour)
                        .padding(content_padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent_colour)
                }
            }
        }
        else if (playlist.feed_layouts != null) {

            val first_layout = playlist.feed_layouts?.firstOrNull()
            LazyMediaItemLayoutColumn(
                playlist.feed_layouts!!,
                playerProvider,
                Modifier
                    .background(background_colour)
                    .fillMaxSize()
                    .padding(content_padding),
                onContinuationRequested = if (first_layout?.continuation == null) null else {{
                    thread {
                        first_layout.loadContinuation().getOrThrowHere()
                    }
                }},
                padding = PaddingValues(bottom = playerProvider().bottom_padding),
                loading_continuation = first_layout?.loading_continuation == true,
                continuation_alignment = Alignment.CenterHorizontally,
                topContent = { item { TopContent() } }
            )
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
                InfoValue("Name", playlist.title ?: "")
                InfoValue("Id", playlist.id)
                InfoValue("Url", playlist.url)
            }
        }
    )
}