@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.composable.PlatformAlertDialog
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.*
import com.spectre7.utils.modifier.background
import com.spectre7.utils.modifier.brushBackground
import kotlinx.coroutines.*
import kotlin.concurrent.thread

private const val ARTIST_IMAGE_SCROLL_MODIFIER = 0.25f

@Composable
fun ArtistPage(
    pill_menu: PillMenu,
    item: Artist,
    previous_item: MediaItem? = null,
    close: () -> Unit
) {
    require(!item.is_for_item)

    var show_info by remember { mutableStateOf(false) }
    val multiselect_context = remember { MediaItemMultiSelectContext() {} }
    val player = LocalPlayerState.current

    val gradient_size = 0.35f
    var accent_colour: Color? by remember { mutableStateOf(null) }

    LaunchedEffect(item.id) {
        if (item.feed_layouts == null) {
            val result = item.loadData()
            result.fold(
                { playlist ->
                    if (playlist == null) {
                        SpMp.error_manager.onError("ArtistPageLoad", Exception("loadData result is null"))
                    }
                },
                { error ->
                    SpMp.error_manager.onError("ArtistPageLoad", error)
                }
            )
        }
    }

    LaunchedEffect(item, item.canLoadThumbnail()) {
        item.loadThumbnail(MediaItemThumbnailProvider.Quality.HIGH)
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

        val lazy_column_state = rememberLazyListState()

        // Thumbnail
        Crossfade(item.getThumbnail(MediaItemThumbnailProvider.Quality.HIGH)) { thumbnail ->
            if (thumbnail != null) {
                if (accent_colour == null) {
                    accent_colour = Theme.current.makeVibrant(item.getDefaultThemeColour() ?: Theme.current.accent)
                }

                Image(
                    thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .offset {
                            IntOffset(0, (lazy_column_state.firstVisibleItemScrollOffset * -ARTIST_IMAGE_SCROLL_MODIFIER).toInt())
                        }
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

        LazyColumn(Modifier.fillMaxSize(), lazy_column_state) {

            val content_padding = PaddingValues(horizontal = 10.dp)
            val background_modifier = Modifier.background(Theme.current.background_provider)

            val play_button_size = 55.dp
            val filter_bar_height = 32.dp

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
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    TitleBar(
                        item,
                        Modifier
                            .offset {
                                IntOffset(0, (lazy_column_state.firstVisibleItemScrollOffset * ARTIST_IMAGE_SCROLL_MODIFIER).toInt())
                            }
                            .padding(bottom = (play_button_size - filter_bar_height) / 2f)
                    )
                }
            }

            // Action / play button bar
            item {
                Box(
                    background_modifier.padding(bottom = 20.dp, end = 10.dp).fillMaxWidth().requiredHeight(filter_bar_height),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    LazyRow(
                        Modifier.fillMaxWidth().padding(end = play_button_size / 2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = content_padding.copy(end = content_padding.calculateEndPadding(LocalLayoutDirection.current) + (play_button_size / 2)),
                    ) {
                        fun chip(text: String, icon: ImageVector, onClick: () -> Unit) {
                            item {
                                ElevatedAssistChip(
                                    onClick,
                                    { Text(text, style = MaterialTheme.typography.labelLarge) },
                                    Modifier.height(filter_bar_height),
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
                            chip(getString("artist_chip_shuffle"), Icons.Outlined.Shuffle) { player.playMediaItem(item, true) }
                        }

                        if (SpMp.context.canShare()) {
                            chip(getString("action_share"), Icons.Outlined.Share) { SpMp.context.shareText(item.url, item.title) }
                        }
                        if (SpMp.context.canOpenUrl()) {
                            chip(getString("artist_chip_open"), Icons.Outlined.OpenInNew) { SpMp.context.openUrl(item.url) }
                        }

                        chip(getString("artist_chip_details"), Icons.Outlined.Info) { show_info = !show_info }
                    }

                    Box(Modifier.requiredHeight(filter_bar_height)) {
                        ShapedIconButton(
                            { player.playMediaItem(item) },
                            Modifier.requiredSize(play_button_size),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = accent_colour ?: LocalContentColor.current,
                                contentColor = (accent_colour ?: LocalContentColor.current).getContrasted()
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(multiselect_context.is_active) {
                    multiselect_context.InfoDisplay(background_modifier)
                }
            }

            if (item.feed_layouts == null) {
                item {
                    Box(background_modifier.fillMaxSize().padding(content_padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent_colour ?: Color.Unspecified)
                    }
                }
            }
            else if (item.feed_layouts!!.size == 1) {
                val layout = item.feed_layouts!!.single()

                item {
                    layout.TitleBar(background_modifier.padding(bottom = 5.dp))
                }

                items(layout.items.size) { i ->
                    Row(background_modifier, verticalAlignment = Alignment.CenterVertically) {
                        Text((i + 1).toString().padStart((layout.items.size + 1).toString().length, '0'), fontWeight = FontWeight.Light)

                        Column {
                            layout.items[i].PreviewLong(MediaItem.PreviewParams(multiselect_context = multiselect_context))
                        }
                    }
                }
            }
            else {
                item {
                    Column(
                        background_modifier
                            .fillMaxSize()
                            .padding(content_padding),
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        for (layout in item.feed_layouts!!) {
                            val type =
                                if (layout.type == null) MediaItemLayout.Type.GRID
                                else if (layout.type == MediaItemLayout.Type.NUMBERED_LIST && item is Artist) MediaItemLayout.Type.LIST
                                else layout.type

                            type.Layout(
                                if (previous_item == null) layout else layout.copy(title = null, subtitle = null),
                                multiselect_context = multiselect_context
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleBar(item: MediaItem, modifier: Modifier = Modifier) {
    val horizontal_padding = 20.dp
    var editing_title by remember { mutableStateOf(false) }
    Crossfade(editing_title) { editing ->
        Column(
            modifier.padding(start = horizontal_padding), 
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
        ) {
            if (editing) {
                var edited_title by remember(item) { mutableStateOf(item.title!!) }

                Column(Modifier.fillMaxWidth().padding(end = horizontal_padding), horizontalAlignment = Alignment.End) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
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
                            editing_title = false
                        }
                    }

                    val field_colour = Theme.current.on_accent
                    OutlinedTextField(
                        edited_title,
                        onValueChange = { text ->
                            edited_title = text
                        },
                        label = { Text(getStringTODO("Edit title")) },
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
                        .fillMaxWidth()
                        .padding(end = horizontal_padding),
                    style = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 35.sp,
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item is Artist && (item.subscriber_count ?: 0) > 0) {
                    Text(item.getReadableSubscriberCount(), style = MaterialTheme.typography.labelLarge )
                }

                Spacer(Modifier.fillMaxWidth().weight(1f))

                Crossfade(item.pinned_to_home) { pinned ->
                    IconButton({ item.setPinnedToHome(!pinned) }) {
                        Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null)
                    }
                }

                if (item is Artist) {
                    ArtistSubscribeButton(item, Modifier.padding(end = horizontal_padding - 10.dp))
                }
            }
        }
    }
}

@Composable
fun ArtistSubscribeButton(
    artist: Artist,
    modifier: Modifier = Modifier,
    accentColourProvider: (() -> Color)? = null,
    icon_modifier: Modifier = Modifier
) {
    if (!DataApi.ytm_authenticated) {
        return
    }

    LaunchedEffect(artist, artist.is_own_channel) {
        if (!artist.is_own_channel) {
            thread {
                artist.updateSubscribed()
            }
        }
    }

    Crossfade(artist.subscribed, modifier) { subscribed ->
        if (subscribed != null) {
            ShapedIconButton(
                {
                    artist.toggleSubscribe(
                        toggle_before_fetch = false,
                    ) { success, subscribing ->
                        if (!success) {
                            SpMp.context.sendToast(getStringTODO(
                                if (subscribing) "Subscribing to ${artist.title} failed"
                                else "Unsubscribing from ${artist.title} failed"
                            ))
                        }
                    }
                },
                icon_modifier,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (subscribed && accentColourProvider != null) accentColourProvider() else Color.Transparent,
                    contentColor = if (subscribed && accentColourProvider != null) accentColourProvider().getContrasted() else LocalContentColor.current
                )
            ) {
                Icon(if (subscribed) Icons.Filled.PersonRemove else Icons.Outlined.PersonAddAlt1, null)
            }
        }
    }
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
        title = { Text(getStringTODO(when (item) {
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
                            Marquee {
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
                InfoValue("Url", item.url.toString())
            }
        }
    )
}
