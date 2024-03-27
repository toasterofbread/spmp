package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import com.toasterofbread.composekit.platform.composable.SwipeRefresh
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.composable.WidthShrinkText
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.getIcon
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.getPinnedItems
import com.toasterofbread.spmp.model.mediaitem.layout.Layout
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredYtmItems
import com.toasterofbread.spmp.model.serialise
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.model.MediaItemGridParams
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.service.playercontroller.FeedLoadState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.uistrings.UiString

@Composable
fun SongFeedAppPage.LFFSongFeedAppPage(
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier,
    content_padding: PaddingValues,
    close: () -> Unit
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current

    if (!feed_endpoint.isImplemented()) {
        feed_endpoint.NotImplementedMessage(modifier.fillMaxSize())
        return
    }

    BackHandler({ selected_filter_chip != null }) {
        selected_filter_chip = null
        loadFeed(false)
    }

    val player: PlayerState = LocalPlayerState.current
    var artists_layout: MediaItemLayout by remember {
        mutableStateOf(
            MediaItemLayout(
                emptyList(),
                null,
                null,
                type = ItemLayoutType.GRID
            )
        )
    }

    val hidden_rows: Set<String> by FeedSettings.Key.HIDDEN_ROWS.rememberMutableState()
    val hidden_row_titles: List<String> = remember(hidden_rows) {
        hidden_rows.map { row_title ->
            UiString.deserialise(row_title).getString(player.context)
        }
    }

    val square_item_max_text_rows: Int by FeedSettings.Key.SQUARE_PREVIEW_TEXT_LINES.rememberMutableState()
    val show_download_indicators: Boolean by FeedSettings.Key.SHOW_SONG_DOWNLOAD_INDICATORS.rememberMutableState()

    val grid_rows: Int by
        when (player.form_factor) {
            FormFactor.PORTRAIT -> FeedSettings.Key.GRID_ROW_COUNT
            FormFactor.LANDSCAPE -> FeedSettings.Key.LANDSCAPE_GRID_ROW_COUNT
        }.rememberMutableState()
    val grid_rows_expanded: Int by
        when (player.form_factor) {
            FormFactor.PORTRAIT -> FeedSettings.Key.GRID_ROW_COUNT_EXPANDED
            FormFactor.LANDSCAPE -> FeedSettings.Key.LANDSCAPE_GRID_ROW_COUNT_EXPANDED
        }.rememberMutableState()

    LaunchedEffect(Unit) {
        if (layouts.isNullOrEmpty()) {
            coroutine_scope.launchSingle {
                loadFeed(allow_cached = !retrying, continue_feed = false)
                retrying = false
            }
        }
    }

    LaunchedEffect(layouts) {
        artists_layout = artists_layout.copy(
            items = populateArtistsLayout(
                layouts,
                player.context.ytapi.user_auth_state?.own_channel_id,
                player.context
            )
        )
    }

    Column(modifier) {
        multiselect_context.InfoDisplay(
            getAllItems = {
                (listOf(artists_layout) + layouts.orEmpty()).map {
                    it.items.map { Pair(it, null) }
                } + listOf(player.database.getPinnedItems().map { Pair(it, null) })
            }
        )

        // Main scrolling view
        SwipeRefresh(
            state = load_state == FeedLoadState.LOADING,
            onRefresh = { loadFeed(false) },
            swipe_enabled = load_state == FeedLoadState.NONE,
            indicator = false
        ) {
            val target_state: Any? = if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts ?: false
            var current_state: Any? by remember { mutableStateOf(target_state) }
            val state_alpha: Animatable<Float, AnimationVector1D> = remember { Animatable(1f) }

            Row(modifier, horizontalArrangement = Arrangement.spacedBy(content_padding.calculateStartPadding(LocalLayoutDirection.current))) {
                LFFArtistsLayout(
                    if (isReloading()) null else artists_layout,
                    Modifier.width(
                        when (Platform.current) {
                            Platform.ANDROID -> 100.dp
                            Platform.DESKTOP -> 125.dp
                        }
                    ),
                    content_padding = content_padding.copy(end = 0.dp),
                    scroll_enabled = !state_alpha.isRunning
                )

                LaunchedEffect(target_state) {
                    if (current_state == target_state) {
                        state_alpha.animateTo(1f, tween(300))
                        return@LaunchedEffect
                    }

                    if (current_state is List<*> && target_state is List<*>) {
                        state_alpha.snapTo(1f)
                        current_state = target_state
                    }
                    else {
                        state_alpha.animateTo(0f, tween(300))
                        current_state = target_state
                        state_alpha.animateTo(1f, tween(300))
                    }
                }

                var hiding_layout: MediaItemLayout? by remember { mutableStateOf(null) }

                hiding_layout?.also { layout ->
                    val title: UiString = layout.title ?: return@also

                    AlertDialog(
                        onDismissRequest = { hiding_layout = null },
                        confirmButton = {
                            Button({
                                FeedSettings.Key.HIDDEN_ROWS.set(
                                    hidden_rows.plus(title.serialise())
                                )

                                hiding_layout = null
                            }) {
                                Text(getString("action_confirm_action"))
                            }
                        },
                        dismissButton = {
                            Button({
                                hiding_layout = null
                            }) {
                                Text(getString("action_deny_action"))
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(getString("prompt_confirm_action"))

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                IconButton({
                                    clipboard.setText(AnnotatedString(title.getString(player.context)))
                                }) {
                                    Icon(Icons.Default.ContentCopy, null)
                                }
                            }
                        },
                        text = {
                            Text(
                                getString("prompt_hide_feed_rows_with_\$title")
                                    .replace("\$title", title.getString(player.context))
                            )
                        }
                    )
                }

                val state: Any? = current_state

                when (state) {
                    // Loaded
                    is List<*> -> {
                        val onContinuationRequested: (() -> Unit)? =
                            if (continuation != null) {{ loadFeed(true) }}
                            else null
                        val loading_continuation: Boolean = load_state != FeedLoadState.NONE

                        ScrollBarLazyColumn(
                            Modifier.graphicsLayer { alpha = state_alpha.value },
                            state = scroll_state,
                            contentPadding = content_padding.vertical,
                            userScrollEnabled = !state_alpha.isRunning
                        ) {
                            items((state as List<MediaItemLayout>)) { layout ->
                                if (layout.items.isEmpty()) {
                                    return@items
                                }

                                val is_hidden: Boolean = remember(layout.title, hidden_row_titles) {
                                    layout.title?.let { layout_title ->
                                        val title: String = layout_title.getString(player.context)
                                        hidden_row_titles.any { it == title }
                                    } ?: false
                                }

                                if (is_hidden) {
                                    return@items
                                }

                                val type: ItemLayoutType = layout.type ?: ItemLayoutType.GRID

                                val rows: Int = if (type == ItemLayoutType.GRID_ALT) grid_rows * 2 else grid_rows
                                val expanded_rows: Int = if (type == ItemLayoutType.GRID_ALT) grid_rows_expanded * 2 else grid_rows_expanded

                                type.Layout(
                                    layout,
                                    MediaItemLayoutParams(
                                        is_song_feed = true,
                                        modifier = Modifier.padding(bottom = 20.dp),
                                        title_modifier = Modifier.platformClickable(
                                            onAltClick = {
                                                if (layout.title != null) {
                                                    hiding_layout = layout
                                                }
                                            }
                                        ),
                                        multiselect_context = player.main_multiselect_context,
                                        apply_filter = true,
                                        show_download_indicators = show_download_indicators,
                                        content_padding = PaddingValues(end = content_padding.calculateEndPadding(LocalLayoutDirection.current))
                                    ),
                                    MediaItemGridParams(
                                        square_item_max_text_rows = square_item_max_text_rows,
                                        rows = Pair(rows, expanded_rows)
                                    )
                                )
                            }

                            item {
                                Crossfade(Pair(onContinuationRequested, loading_continuation), Modifier.padding(end = content_padding.calculateEndPadding(LocalLayoutDirection.current))) { data ->
                                    val (requestContinuation, loading) = data

                                    if (loading || requestContinuation != null) {
                                        Box(Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.Center) {
                                            if (loading) {
                                                SubtleLoadingIndicator()
                                            }
                                            else if (requestContinuation != null) {
                                                IconButton({ requestContinuation() }) {
                                                    Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = player.theme.on_background)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading
                    null -> {
                        Column(Modifier.fillMaxSize()) {
                            SongFeedPageLoadingView(Modifier.graphicsLayer { alpha = state_alpha.value }.fillMaxSize())
                        }
                    }

                    // Load failed
                    else -> {
                        LaunchedEffect(load_error) {
                            if (load_error == null) {
                                return@LaunchedEffect
                            }

                            val library = player.app_page_state.Library
                            library.external_load_error = load_error
                            load_error = null

                            player.openAppPage(library)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongFeedAppPage.LFFArtistsLayout(layout: MediaItemLayout?, modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), scroll_enabled: Boolean = true) {
    val player: PlayerState = LocalPlayerState.current

    val artists: List<MediaItem>? by layout?.items?.rememberFilteredYtmItems()
    var show_filters: Boolean by remember { mutableStateOf(false) }

    val can_show_artists: Boolean = !artists.isNullOrEmpty()
    val can_show_filters: Boolean = !filter_chips.isNullOrEmpty()

    Crossfade(show_filters) { filters ->
        Column(
            modifier.padding(top = content_padding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.padding(content_padding.horizontal), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val selected_colours: IconButtonColors = IconButtonDefaults.iconButtonColors(
                    containerColor = player.theme.vibrant_accent.copy(alpha = 0.85f),
                    contentColor = player.theme.vibrant_accent.getContrasted()
                )

                ShapedIconButton(
                    { show_filters = false },
                    if (!filters) selected_colours
                    else IconButtonDefaults.iconButtonColors(),
                    enabled = can_show_artists || !filters
                ) {
                    Icon(Icons.Default.Person, null)
                }

                ShapedIconButton(
                    { show_filters = true },
                    if (filters) selected_colours
                    else IconButtonDefaults.iconButtonColors(),
                    enabled = can_show_filters || filters
                ) {
                    Icon(Icons.Default.FilterAlt, null)
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = content_padding.copy(top = 0.dp),
                userScrollEnabled = scroll_enabled,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                if (filters) {
                    itemsIndexed(filter_chips ?: emptyList()) { index, filter ->
                        val selected: Boolean = index == selected_filter_chip

                        Card(
                            { selectFilterChip(index) },
                            Modifier.aspectRatio(1f),
                            colors =
                                if (selected) CardDefaults.cardColors(
                                    containerColor = player.theme.vibrant_accent,
                                    contentColor = player.theme.vibrant_accent.getContrasted()
                                )
                                else CardDefaults.cardColors(
                                    containerColor = player.theme.accent.blendWith(player.theme.background, 0.05f),
                                    contentColor = player.theme.on_background
                                ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val icon: ImageVector? = filter.getIcon()
                                if (icon != null) {
                                    Icon(
                                        icon,
                                        null,
                                        Modifier.aspectRatio(1f).fillMaxHeight().weight(1f).padding(10.dp),
                                        tint =
                                            if (selected) LocalContentColor.current
                                            else player.theme.vibrant_accent
                                    )
                                }

                                WidthShrinkText(
                                    filter.text.getString(player.context),
                                    style = MaterialTheme.typography.labelLarge,
                                    alignment = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else {
                    items(artists ?: emptyList()) { item ->
                        MediaItemPreviewSquare(
                            item,
                            multiselect_context = player.main_multiselect_context,
                            apply_size = false
                        )
                    }
                }
            }
        }
    }
}
