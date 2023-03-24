@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.MediaItemWithLayouts
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun ArtistPlaylistPage(
    pill_menu: PillMenu,
    item: MediaItem,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    require(item is MediaItemWithLayouts)
    require(item !is Artist || !item.for_song)

    var show_info by remember { mutableStateOf(false) }

    val share_intent = remember(item.url, item.title) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, item.title)
            putExtra(Intent.EXTRA_TEXT, item.url)
            type = "text/plain"
        }, null)
    }
    val open_intent: Intent? = remember(item.url) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
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

    LaunchedEffect(item.id) {
        thread {
            if (item.feed_layouts == null) {
                item.loadData()
            }
        }
    }

    LaunchedEffect(item.canLoadThumbnail()) {
        item.getThumbnail(MediaItem.ThumbnailQuality.HIGH)
    }

    LaunchedEffect(accent_colour) {
        if (accent_colour != null) {
            pill_menu.setBackgroundColourOverride(accent_colour)
        }
    }

    if (show_info) {
        InfoDialog(item) { show_info = false }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {

        // Thumbnail
        Crossfade(item.getThumbnail(MediaItem.ThumbnailQuality.HIGH)) { thumbnail ->
            if (thumbnail != null) {
                if (accent_colour == null) {
                    accent_colour = item.getDefaultThemeColour() ?: Theme.current.accent
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
                    TitleBar(item, Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .padding(bottom = 20.dp)
                    )
                }
            }

            val content_padding = 10.dp

            // Secondary action bar
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

            // Primary action bar
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(background_colour)
                        .padding(start = 20.dp, bottom = 10.dp)) {
                    @Composable
                    fun Btn(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
                        OutlinedButton(onClick = onClick, modifier.height(45.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, null, tint = accent_colour ?: Color.Unspecified)
                                Text(text, softWrap = false, color = Theme.current.on_background)
                            }
                        }
                    }

                    Btn(getString(R.string.artist_chip_play), Icons.Outlined.PlayArrow,
                        Modifier
                            .fillMaxWidth(0.5f)
                            .weight(1f)) { TODO() }
                    Spacer(Modifier.requiredWidth(20.dp))
                    Btn(getString(R.string.artist_chip_radio), Icons.Outlined.Radio,
                        Modifier
                            .fillMaxWidth(1f)
                            .weight(1f)) { TODO() }

                    if (item is Artist) {
                        ArtistSubscribeButton(item, background_colour, accent_colour)
                    }
                }
            }

            // Loaded items
            item {
                Crossfade(item.feed_layouts) { layouts ->
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

                            val description = item.description
                            if (description?.isNotBlank() == true) {
                                DescriptionCard(description, background_colour, accent_colour) { show_info = !show_info }
                            }

                            Spacer(Modifier.requiredHeight(50.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitleBar(item: MediaItem, modifier: Modifier = Modifier) {
    var editing_title by remember { mutableStateOf(false) }
    Crossfade(editing_title) { editing ->
        Box(modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.BottomCenter) {
            if (editing) {
                var edited_title by remember(item) { mutableStateOf(item.title!!) }

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedByEnd(10.dp)) {
                        @Composable
                        fun Action(icon: ImageVector, action: () -> Unit) {
                            Box(
                                Modifier
                                    .background(Theme.current.accent, CircleShape)
                                    .size(42.dp)
                                    .padding(8.dp)
                                    .clickable(onClick = action),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = Theme.current.on_accent)
                            }
                        }

                        Action(Icons.Filled.Close) { editing_title = false }
                        Action(Icons.Filled.Refresh) { edited_title = item.original_title!! }
                        Action(Icons.Filled.Done) {
                            item.registry_entry.title = edited_title
                            item.saveRegistry()
                        }
                    }

                    val field_colour = Theme.current.on_accent
                    OutlinedTextField(
                        edited_title,
                        onValueChange = { text ->
                            edited_title = text
                        },
                        label = { Text(getString("Edit title")) },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Filled.Close, null, Modifier.clickable { edited_title = "" })
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            item.registry_entry.title = edited_title
                            item.saveRegistry()
                            editing_title = false
                        }),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = field_colour,
                            focusedLabelColor = field_colour,
                            cursorColor = field_colour
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            } else {
                WidthShrinkText(
                    item.title ?: "",
                    Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                vibrateShort()
                                editing_title = true
                            }
                        )
                        .fillMaxWidth(),
                    style = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 40.sp,
                    )
                )
            }
        }
    }
}

@Composable
private fun ArtistSubscribeButton(artist: Artist, background_colour: Color, accent_colour: Color?) {
    LaunchedEffect(artist) {
        thread {
            artist.updateSubscribed()
        }
    }

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

@Composable
private fun DescriptionCard(description_text: String, background_colour: Color, accent_colour: Color?, toggleInfo: () -> Unit) {
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
                    toggleInfo,
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
                description_text,
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

@Composable
private fun InfoDialog(item: MediaItem, close: () -> Unit) {
    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text("Close")
            }
        },
        title = { Text(getString(when (item) {
            is Artist -> "Artist info"
            is Playlist -> "Playlist info"
            else -> throw NotImplementedError(item.type.toString())
        })) },
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
                InfoValue("Name", item.title ?: "")
                InfoValue("Id", item.id)
                InfoValue("Url", item.url)
            }
        }
    )
}
