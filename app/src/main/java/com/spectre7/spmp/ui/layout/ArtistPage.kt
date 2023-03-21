@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun ArtistPage(
    pill_menu: PillMenu,
    artist: Artist,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    check(!artist.for_song)

    var show_info by remember { mutableStateOf(false) }

    val share_intent = remember(artist.url, artist.title) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, artist.title)
            putExtra(Intent.EXTRA_TEXT, artist.url)
            type = "text/plain"
        }, null)
    }
    val open_intent: Intent? = remember(artist.url) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artist.url))
        if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
            null
        }
        else {
            intent
        }
    }

    val gradient_size = 0.35f
    val background_colour = Theme.current.background
    var accent_colour: Color? by remember { mutableStateOf(null) }

    LaunchedEffect(artist.id) {
        thread {
            if (artist.feed_layouts == null) {
                artist.loadData()
            }
            artist.updateSubscribed()
        }
    }

    LaunchedEffect(artist.canLoadThumbnail()) {
        artist.getThumbnail(MediaItem.ThumbnailQuality.HIGH)
    }

    LaunchedEffect(accent_colour) {
        if (accent_colour != null) {
            pill_menu.setBackgroundColourOverride(accent_colour)
        }
    }

    if (show_info) {
        InfoDialog(artist) { show_info = false }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {

        // Artist image
        Crossfade(artist.getThumbnail(MediaItem.ThumbnailQuality.HIGH)) { thumbnail ->
            if (thumbnail != null) {
                if (accent_colour == null) {
                    accent_colour = artist.getDefaultThemeColour() ?: Theme.current.accent
                }

                Image(
                    thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            Brush.verticalGradient(
                                0f to background_colour,
                                gradient_size to Color.Transparent
                            )
                        )
                )
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
                        Text(artist.title ?: "", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp, softWrap = false)
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
                                    Icon(icon, null, tint = accent_colour ?: Color.Unspecified)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = background_colour,
                                    labelColor = Theme.current.on_background,
                                    leadingIconContentColor = accent_colour ?: Color.Unspecified
                                )
                            )
                        }
                    }

                    chip(getString(R.string.artist_chip_shuffle), Icons.Outlined.Shuffle) { TODO() }
                    chip(getString(R.string.action_share), Icons.Outlined.Share) { MainActivity.context.startActivity(share_intent) }
                    chip(getString(R.string.artist_chip_open), Icons.Outlined.OpenInNew) { MainActivity.context.startActivity(open_intent) }
                    chip(getString(R.string.artist_chip_details), Icons.Outlined.Info) { show_info = !show_info }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().background(background_colour).padding(start = 20.dp, bottom = 10.dp)) {
                    @Composable
                    fun Btn(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
                        OutlinedButton(onClick = onClick, modifier.height(45.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, null, tint = accent_colour ?: Color.Unspecified)
                                Text(text, softWrap = false, color = Theme.current.on_background)
                            }
                        }
                    }

                    Btn(getString(R.string.artist_chip_play), Icons.Outlined.PlayArrow, Modifier.fillMaxWidth(0.5f).weight(1f)) { TODO() }
                    Spacer(Modifier.requiredWidth(20.dp))
                    Btn(getString(R.string.artist_chip_radio), Icons.Outlined.Radio, Modifier.fillMaxWidth(1f).weight(1f)) { TODO() }

                    Crossfade(artist.subscribed) { subscribed ->
                        if (subscribed == null) {
                            Spacer(Modifier.requiredWidth(20.dp))
                        }
                        else {
                            Row {
                                Spacer(Modifier.requiredWidth(10.dp))
                                OutlinedIconButton(
                                    {
                                        artist.toggleSubscribe(
                                            toggle_before_fetch = true,
                                            notify_failure = true
                                        )
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (subscribed) accent_colour ?: Color.Unspecified else background_colour,
                                        contentColor = if (subscribed) accent_colour?.getContrasted() ?: Color.Unspecified else Theme.current.on_background
                                    )
                                ) {
                                    Icon(if (subscribed) Icons.Outlined.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
                                }
                            }
                        }
                    }
                }
            }

            // Loaded items
            item {
                Crossfade(artist.feed_layouts) { layouts ->
                    if (layouts == null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(background_colour)
                                .padding(content_padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent_colour ?: Color.Unspecified)
                        }
                    }
                    else {
                        Column(
                            Modifier
                                .background(background_colour)
                                .fillMaxSize()
                                .padding(content_padding),
                            verticalArrangement = Arrangement.spacedBy(30.dp)
                        ) {
                            for (row in layouts) {
                                MediaItemGrid(MediaItemLayout(row.title, null, items = row.items), playerProvider)
                            }

                            val description = artist.description
                            if (description?.isNotBlank() == true) {

                                var expanded by remember { mutableStateOf(false) }
                                var can_expand by remember { mutableStateOf(false) }
                                val small_text_height = 200.dp
                                val small_text_height_px = with ( LocalDensity.current ) { small_text_height.toPx().toInt() }

                                ElevatedCard(
                                    Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = Theme.current.on_background.setAlpha(0.05f)
                                    )
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            AssistChip(
                                                { show_info = !show_info },
                                                {
                                                    Text(getString(R.string.artist_info_label), style = MaterialTheme.typography.labelLarge)
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.Info, null)
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = background_colour,
                                                    labelColor = Theme.current.on_background,
                                                    leadingIconContentColor = accent_colour ?: Color.Unspecified
                                                )
                                            )

                                            if (can_expand) {
                                                NoRipple {
                                                    IconButton(
                                                        { expanded = !expanded }
                                                    ) {
                                                        Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,null)
                                                    }
                                                }
                                            }
                                        }

                                        LinkifyText(
                                            description,
                                            Theme.current.on_background.setAlpha(0.8f),
                                            Theme.current.on_background,
                                            MaterialTheme.typography.bodyMedium,
                                            Modifier
                                                .onSizeChanged { size ->
                                                    if (size.height == small_text_height_px) {
                                                        can_expand = true
                                                    }
                                                }
                                                .animateContentSize()
                                                .then(
                                                    if (expanded) Modifier else Modifier.height(200.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.requiredHeight(50.dp))
                        }
                    }
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
                InfoValue("Name", artist.title ?: "")
                InfoValue("Id", artist.id)
                InfoValue("Url", artist.url)
            }
        }
    )
}
