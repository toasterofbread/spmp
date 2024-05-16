package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.platform.composable.SwipeRefresh
import dev.toastbits.composekit.platform.composable.platformClickable
import dev.toastbits.composekit.utils.common.launchSingle
import dev.toastbits.composekit.utils.composable.SubtleLoadingIndicator
import dev.toastbits.composekit.utils.modifier.horizontal
import dev.toastbits.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.deserialise
import com.toasterofbread.spmp.model.getString
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.layout.Layout
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.serialise
import dev.toastbits.ytmkt.model.external.mediaitem.MediaItemLayout
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.model.MediaItemGridParams
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.PinnedItemsRow
import com.toasterofbread.spmp.service.playercontroller.FeedLoadState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.model.external.mediaitem.YtmMediaItem
import dev.toastbits.ytmkt.uistrings.UiString

@Composable
internal fun SongFeedAppPage.SFFSongFeedAppPage(
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
    val form_factor: FormFactor by FormFactor.observe()

    val artists_layout: AppMediaItemLayout = remember {
        AppMediaItemLayout(
            mutableStateListOf(),
            null,
            null,
            type = ItemLayoutType.ROW
        )
    }

    val hidden_rows: Set<String> by player.settings.feed.HIDDEN_ROWS.observe()
    val hidden_row_titles: List<String> = remember(hidden_rows) {
        hidden_rows.map { row_title ->
            UiString.deserialise(row_title).getString(player.context)
        }
    }

    val square_item_max_text_rows: Int by player.settings.feed.SQUARE_PREVIEW_TEXT_LINES.observe()
    val show_download_indicators: Boolean by player.settings.feed.SHOW_SONG_DOWNLOAD_INDICATORS.observe()

    val grid_rows: Int by
        when (form_factor) {
            FormFactor.PORTRAIT -> player.settings.feed.GRID_ROW_COUNT
            FormFactor.LANDSCAPE -> player.settings.feed.LANDSCAPE_GRID_ROW_COUNT
        }.observe()
    val grid_rows_expanded: Int by
        when (form_factor) {
            FormFactor.PORTRAIT -> player.settings.feed.GRID_ROW_COUNT_EXPANDED
            FormFactor.LANDSCAPE -> player.settings.feed.LANDSCAPE_GRID_ROW_COUNT_EXPANDED
        }.observe()

    LaunchedEffect(Unit) {
        if (layouts.isNullOrEmpty()) {
            coroutine_scope.launchSingle {
                loadFeed(allow_cached = !retrying, continue_feed = false)
                retrying = false
            }
        }
    }

    LaunchedEffect(layouts) {
        val items = artists_layout.items as MutableList<YtmMediaItem>
        items.clear()
        items.addAll(
            populateArtistsLayout(
                layouts,
                player.context.ytapi.user_auth_state?.own_channel_id,
                player.context
            )
        )
    }

    // Main scrolling view
    SwipeRefresh(
        state = load_state == FeedLoadState.LOADING,
        onRefresh = { loadFeed(false) },
        swipe_enabled = load_state == FeedLoadState.NONE,
        indicator = false,
        indicator_padding = PaddingValues(top = content_padding.calculateTopPadding()),
        modifier = Modifier.fillMaxSize()
    ) {
        val target_state: Any? = if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts ?: false
        var current_state: Any? by remember { mutableStateOf(target_state) }
        val state_alpha = remember { Animatable(1f) }

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

        @Composable
        fun TopContent(modifier: Modifier = Modifier) {
            PinnedItemsRow(modifier, multiselect_context = multiselect_context)
        }

        var hiding_layout: AppMediaItemLayout? by remember { mutableStateOf(null) }

        hiding_layout?.also { layout ->
            val title: UiString = layout.title ?: return@also

            AlertDialog(
                onDismissRequest = { hiding_layout = null },
                confirmButton = {
                    Button({
                        player.settings.feed.HIDDEN_ROWS.set(
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
                    Text(getString("prompt_confirm_action"))
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
                val horizontal_padding: PaddingValues = content_padding.horizontal
                val show_artists_row: Boolean by player.settings.feed.SHOW_ARTISTS_ROW.observe()

                LazyColumn(
                    Modifier.graphicsLayer { alpha = state_alpha.value },
                    state = scroll_state,
                    contentPadding = content_padding.vertical,
                    userScrollEnabled = !state_alpha.isRunning
                ) {
                    item {
                        TopContent(Modifier.padding(horizontal_padding))
                    }

                    if (show_artists_row) {
                        item {
                            if (artists_layout.items.isNotEmpty()) {
                                artists_layout.Layout(
                                    MediaItemLayoutParams(
                                        multiselect_context = multiselect_context,
                                        apply_filter = true,
                                        content_padding = horizontal_padding
                                    )
                                )
                            }
                        }
                    }

                    items((state as List<AppMediaItemLayout>)) { layout ->
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
                                modifier = Modifier.padding(top = 20.dp),
                                title_modifier = Modifier.platformClickable(
                                    onAltClick = {
                                        if (layout.title != null) {
                                            hiding_layout = layout
                                        }
                                    }
                                ),
                                multiselect_context = multiselect_context,
                                apply_filter = true,
                                show_download_indicators = show_download_indicators,
                                content_padding = horizontal_padding
                            ),
                            grid_params = MediaItemGridParams(
                                square_item_max_text_rows = square_item_max_text_rows,
                                rows = Pair(rows, expanded_rows)
                            )
                        )
                    }

                    item {
                        Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                            val (requestContinuation, loading) = data

                            if (loading || requestContinuation != null) {
                                Box(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal_padding), contentAlignment = Alignment.Center) {
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
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(content_padding.vertical)
                ) {
                    TopContent()
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
