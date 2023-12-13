package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.ScrollBarLazyColumn
import com.toasterofbread.composekit.platform.composable.SwipeRefresh
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.rememberFilteredItems
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewSquare
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.FeedLoadState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage

@Composable
fun SongFeedAppPage.LFFSongFeedAppPage(
    multiselect_context: MediaItemMultiSelectContext,
    modifier: Modifier,
    content_padding: PaddingValues,
    close: () -> Unit
) {
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
                type = MediaItemLayout.Type.GRID
            )
        )
    }
    val hidden_rows: Set<String> by FeedSettings.Key.HIDDEN_ROWS.rememberMutableState()

    val square_item_max_text_rows: Int by FeedSettings.Key.SQUARE_PREVIEW_TEXT_LINES.rememberMutableState()
    val show_download_indicators: Boolean by FeedSettings.Key.SHOW_SONG_DOWNLOAD_INDICATORS.rememberMutableState()

    val grid_rows: Int by FeedSettings.Key.GRID_ROW_COUNT.rememberMutableState()
    val grid_rows_expanded: Int by FeedSettings.Key.GRID_ROW_COUNT_EXPANDED.rememberMutableState()
    val alt_grid_rows: Int by FeedSettings.Key.ALT_GRID_ROW_COUNT.rememberMutableState()
    val alt_grid_rows_expanded: Int by FeedSettings.Key.ALT_GRID_ROW_COUNT_EXPANDED.rememberMutableState()

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
                player.context.ytapi.user_auth_state?.own_channel,
                player.context
            )
        )
    }

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
                Modifier.width(125.dp),
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
                check(layout.title != null)

                AlertDialog(
                    onDismissRequest = { hiding_layout = null },
                    confirmButton = {
                        Button({
                            val hidden_rows: Set<String> = FeedSettings.Key.HIDDEN_ROWS.get()
                            FeedSettings.Key.HIDDEN_ROWS.set(
                                hidden_rows.plus(layout.title.serialise())
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
                        Text(getString("prompt_confirm_action"))
                    },
                    text = {
                        Text(getString("prompt_hide_feed_rows_with_\$title").replace("\$title", layout.title.getString(player.context)))
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

                            if (layout.title != null) {
                                val title: String = layout.title.getString(player.context)
                                if (
                                    hidden_rows.any { row_title ->
                                        LocalisedString.deserialise(row_title).getString(player.context) == title
                                    }
                                    ) {
                                    return@items
                                }
                            }

                            val type: MediaItemLayout.Type = layout.type ?: MediaItemLayout.Type.GRID

                            val rows: Int = if (type == MediaItemLayout.Type.GRID_ALT) alt_grid_rows else grid_rows
                            val expanded_rows: Int = if (type == MediaItemLayout.Type.GRID_ALT) alt_grid_rows_expanded else grid_rows_expanded

                            type.Layout(
                                layout,
                                Modifier.padding(bottom = 20.dp),
                                title_modifier = Modifier.platformClickable(
                                    onAltClick = {
                                        if (layout.title != null) {
                                            hiding_layout = layout
                                        }
                                    }
                                ),
                                multiselect_context = player.main_multiselect_context,
                                apply_filter = true,
                                square_item_max_text_rows = square_item_max_text_rows,
                                show_download_indicators = show_download_indicators,
                                grid_rows = Pair(rows, expanded_rows),
                                content_padding = PaddingValues(end = content_padding.calculateEndPadding(LocalLayoutDirection.current)),
                                itemSizeProvider = {
                                    if (type == MediaItemLayout.Type.GRID_ALT) DpSize(450.dp, 75.dp)
                                    else DpSize(150.dp, 180.dp)
                                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongFeedAppPage.LFFArtistsLayout(layout: MediaItemLayout?, modifier: Modifier = Modifier, content_padding: PaddingValues = PaddingValues(), scroll_enabled: Boolean = true) {
    val player: PlayerState = LocalPlayerState.current

    val artists: List<MediaItem>? by layout?.items?.rememberFilteredItems()
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

                                Text(
                                    filter.text.getString(player.context),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                else {
                    items(artists ?: emptyList()) { item ->
                        MediaItemPreviewSquare(item, multiselect_context = player.main_multiselect_context)
                    }
                }
            }
        }
    }
}
