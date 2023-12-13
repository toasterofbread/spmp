package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.SwipeRefresh
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.PinnedItemsRow
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.FeedLoadState
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage

@Composable
fun SongFeedAppPage.SFFSongFeedAppPage(
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

    val player = LocalPlayerState.current
    val artists_layout: MediaItemLayout = remember {
        MediaItemLayout(
            mutableStateListOf(),
            null,
            null,
            type = MediaItemLayout.Type.ROW
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
        val items = artists_layout.items as MutableList<MediaItem>
        items.clear()
        items.addAll(
            populateArtistsLayout(
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
        indicator = false,
        modifier = Modifier.fillMaxSize()
    ) {
        val target_state = if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts ?: false
        var current_state by remember { mutableStateOf(target_state) }
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
            PinnedItemsRow(modifier.padding(bottom = 10.dp))
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

        val state = current_state

        when (state) {
            // Loaded
            is List<*> -> {
                val onContinuationRequested = if (continuation != null) {
                    { loadFeed(true) }
                } else null
                val loading_continuation = load_state != FeedLoadState.NONE
                val horizontal_padding: PaddingValues = content_padding.horizontal

                LazyColumn(
                    Modifier.graphicsLayer { alpha = state_alpha.value },
                    state = scroll_state,
                    contentPadding = content_padding.vertical,
                    userScrollEnabled = !state_alpha.isRunning
                ) {
                    item {
                        TopContent(Modifier.padding(horizontal_padding))
                    }

                    item {
                        if (artists_layout.items.isNotEmpty()) {
                            artists_layout.Layout(multiselect_context = player.main_multiselect_context, apply_filter = true, content_padding = horizontal_padding)
                        }
                    }

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

                        val type = layout.type ?: MediaItemLayout.Type.GRID

                        val rows: Int = if (type == MediaItemLayout.Type.GRID_ALT) alt_grid_rows else grid_rows
                        val expanded_rows: Int = if (type == MediaItemLayout.Type.GRID_ALT) alt_grid_rows_expanded else grid_rows_expanded

                        type.Layout(
                            layout,
                            Modifier.padding(top = 20.dp),
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
                            content_padding = horizontal_padding
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
                Column(Modifier.fillMaxSize()) {
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
