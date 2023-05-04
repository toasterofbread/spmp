@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

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
import com.spectre7.spmp.api.getOrReport
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.MediaItemWithLayouts
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.platform.PlatformAlertDialog
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.getString
import kotlinx.coroutines.*
import kotlin.concurrent.thread

@Composable
fun ArtistPlaylistPage(
    pill_menu: PillMenu,
    item: MediaItem,
    playerProvider: () -> PlayerViewContext,
    opened_layout: MediaItemLayout? = null,
    close: () -> Unit
) {
    require(item is MediaItemWithLayouts)
    require(item !is Artist || !item.for_song)

    var show_info by remember { mutableStateOf(false) }

    val gradient_size = 0.35f
    var accent_colour: Color? by remember { mutableStateOf(null) }

    LaunchedEffect(item.id) {
        if (opened_layout != null) {
            val view_more = opened_layout.view_more!!
            if (view_more.layout == null) {
                thread {
                    view_more.loadLayout().getOrReport("ArtistPlaylistPageLoad")
                }
            }
        }
        else if (item.feed_layouts == null) {
            thread {
                val result = item.loadData()
                result.fold(
                    { playlist ->
                        if (playlist == null) {
                            SpMp.error_manager.onError("ArtistPlaylistPageLoad", Exception("loadData result is null"))
                        }
                    },
                    { error ->
                        SpMp.error_manager.onError("ArtistPlaylistPageLoad", error)
                    }
                )
            }
        }
    }

    LaunchedEffect(item.canLoadThumbnail()) {
        item.getThumbnail(MediaItem.ThumbnailQuality.HIGH)
    }

//    LaunchedEffect(accent_colour) {
//        if (accent_colour != null) {
//            pill_menu.setBackgroundColourOverride(accent_colour)
//        }
//    }

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
                    thumbnail,
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
                        .brushBackground {
                            Brush.verticalGradient(
                                0f to Theme.current.background,
                                gradient_size to Color.Transparent
                            )
                        }
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
                        .brushBackground {
                            Brush.verticalGradient(
                                1f - gradient_size to Color.Transparent,
                                1f to Theme.current.background
                            )
                        }
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    TitleBar(
                        item,
                        Modifier
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
                        .background { Theme.current.background },
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
                                    containerColor = Theme.current.background,
                                    labelColor = Theme.current.on_background,
                                    leadingIconContentColor = accent_colour ?: Color.Unspecified
                                )
                            )
                        }
                    }

                    if (item is Artist) {
                        chip(getString("artist_chip_shuffle"), Icons.Outlined.Shuffle) { TODO() }
                    }

                    if (SpMp.context.canShare()) {
                        chip(getString("action_share"), Icons.Outlined.Share) { SpMp.context.shareText(item.url, item.title) }
                    }
                    if (SpMp.context.canOpenUrl()) {
                        chip(getString("artist_chip_open"), Icons.Outlined.OpenInNew) { SpMp.context.openUrl(item.url) }
                    }

                    chip(getString("artist_chip_details"), Icons.Outlined.Info) { show_info = !show_info }
                }
            }

            // Primary action bar
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background { Theme.current.background }
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

                    Btn(
                        getString("artist_chip_play"),
                        Icons.Outlined.PlayArrow,
                        Modifier
                            .fillMaxWidth(0.5f)
                            .weight(1f)
                    ) {
                        playerProvider().playMediaItem(item)
                    }

                    Spacer(Modifier.requiredWidth(20.dp))

                    Btn(
                        getString(if (item is Artist) "artist_chip_radio" else "artist_chip_shuffle"),
                        if (item is Artist) Icons.Outlined.Radio else Icons.Outlined.Shuffle,
                        Modifier
                            .fillMaxWidth(1f)
                            .weight(1f)
                    ) {
                        if (item is Artist) {
                            TODO()
                        }
                        else {
                            playerProvider().playMediaItem(item, shuffle = true)
                        }
                    }

                    if (item is Artist) {
                        ArtistSubscribeButton(item, { Theme.current.background }, { accent_colour })
                    }
                }
            }

            // Loaded items
            item {
                Crossfade(if (opened_layout != null) opened_layout.view_more!!.layout?.let { listOf(it) } else item.feed_layouts) { layouts ->
                    if (layouts == null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background { Theme.current.background }
                                .padding(content_padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent_colour ?: Color.Unspecified)
                        }
                    }
                    else {
                        Column(
                            Modifier
                                .background { Theme.current.background }
                                .fillMaxSize()
                                .padding(content_padding),
                            verticalArrangement = Arrangement.spacedBy(30.dp)
                        ) {
                            for (row in layouts) {
                                (row.type ?: MediaItemLayout.Type.GRID).Layout(
                                    if (opened_layout == null) row else row.copy(title = null, subtitle = null),
                                    playerProvider
                                )
                            }

                            val description = item.description
                            if (description?.isNotBlank() == true) {
                                DescriptionCard(description, { Theme.current.background }, { accent_colour }) { show_info = !show_info }
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
private fun TitleBar(item: MediaItem, modifier: Modifier = Modifier) {
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
                            item.editRegistry {
                                it.title = edited_title
                            }
                        }
                    }

                    val field_colour = Theme.current.on_accent
                    OutlinedTextField(
                        edited_title,
                        onValueChange = { text ->
                            edited_title = text
                        },
                        label = { Text(getStringTemp("Edit title")) },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Filled.Close, null, Modifier.clickable { edited_title = "" })
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            item.editRegistry {
                                it.title = edited_title
                            }
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
                                SpMp.context.vibrateShort()
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
fun ArtistSubscribeButton(
    artist: Artist,
    backgroundColourProvider: () -> Color,
    accentColourProvider: () -> Color?,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(artist) {
        thread {
            artist.updateSubscribed()
        }
    }

    Box(modifier) {
        Crossfade(artist.subscribed) { subscribed ->
            if (subscribed != null) {
                OutlinedIconButton(
                    {
                        artist.toggleSubscribe(
                            toggle_before_fetch = true,
                        ) { success, subscribing ->
                            if (!success) {
                                SpMp.context.sendToast(getStringTemp(
                                    if (subscribing) "Subscribing to ${artist.title} failed"
                                    else "Unsubscribing from ${artist.title} failed"
                                ))
                            }
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (subscribed) accentColourProvider() ?: Color.Unspecified else backgroundColourProvider(),
                        contentColor = if (subscribed) accentColourProvider()?.getContrasted() ?: Color.Unspecified else Theme.current.on_background
                    )
                ) {
                    Icon(if (subscribed) Icons.Outlined.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
                }
            }
        }
    }
    //        if (subscribed == null) {
    //            Spacer(Modifier.requiredWidth(20.dp))
    //        }
    //        else {
    //            Row {
    //                Spacer(Modifier.requiredWidth(10.dp))
    //            }
    //        }

}

@Composable
private fun DescriptionCard(description_text: String, backgroundColourProvider: () -> Color, accentColourProvider: () -> Color?, toggleInfo: () -> Unit) {
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
                        Text(getString("artist_info_label"), style = MaterialTheme.typography.labelLarge)
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, null)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = backgroundColourProvider(),
                        labelColor = Theme.current.on_background,
                        leadingIconContentColor = accentColourProvider() ?: Color.Unspecified
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
                Modifier
                    .onSizeChanged { size ->
                        if (size.height == small_text_height_px) {
                            can_expand = true
                        }
                    }
                    .animateContentSize()
                    .then(
                        if (expanded) Modifier else Modifier.height(200.dp)
                    ),
                Theme.current.on_background.setAlpha(0.8f),
                Theme.current.on_background,
                MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoDialog(item: MediaItem, close: () -> Unit) {
    PlatformAlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text("Close")
            }
        },
        title = { Text(getStringTemp(when (item) {
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
                            SpMp.context.sendToast("Copied ${name.lowercase()} to clipboard")
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
                InfoValue("Url", item.url)
            }
        }
    )
}
