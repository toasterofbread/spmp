package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.platform.composable.SwipeRefresh
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.WAVE_BORDER_DEFAULT_HEIGHT
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.ui.layout.library.LibraryPage
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

class HomeFeedPage(
    val getLoadState: () -> FeedLoadState,
    val getLoadError: () -> Throwable?,
    val loadFeed: (continuation: Boolean) -> Unit,
    val getCanContinue: () -> Boolean,
    val getLayouts: () -> List<MediaItemLayout>,
    val scroll_state: LazyListState
): MainPage() {
    @Composable
    override fun Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val load_state = getLoadState()
        val layouts = getLayouts()
        val player = LocalPlayerState.current

        val artists_layout: MediaItemLayout = remember {
            MediaItemLayout(
                null,
                null,
                items = mutableStateListOf(),
                type = MediaItemLayout.Type.ROW,
                itemSizeProvider = {
                    val size = 80.dp
                    DpSize(size, size + 30.dp)
                }
            )
        }
    
        LaunchedEffect(layouts) {
            populateArtistsLayout(artists_layout, getLayouts, Api.ytm_auth.getOwnChannelOrNull())
        }

        // Main scrolling view
        SwipeRefresh(
            state = load_state == FeedLoadState.LOADING,
            onRefresh = { loadFeed(false) },
            swipe_enabled = load_state == FeedLoadState.NONE,
            indicator = false
        ) {
            val target_state = if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts.ifEmpty { getLoadError() ?: false }
            var current_state by remember { mutableStateOf(target_state) }
            val state_alpha = remember { Animatable(1f) }

            LaunchedEffect(target_state) {
                if (current_state == target_state) {
                    state_alpha.animateTo(1f, tween(300))
                    return@LaunchedEffect
                }

                state_alpha.animateTo(0f, tween(300))
                current_state = target_state
                state_alpha.animateTo(1f, tween(300))
            }

            @Composable
            fun TopContent() {
                val pinned_items = player.pinned_items
                AnimatedVisibility(pinned_items.isNotEmpty()) {
                    PinnedItemsRow(Modifier.padding(bottom = 10.dp), pinned_items)
                }
            }

            current_state.also { state ->
                when (state) {
                    // Loaded
                    is List<*> -> {
                        val onContinuationRequested = if (getCanContinue()) {
                            { loadFeed(true) }
                        } else null
                        val loading_continuation = load_state != FeedLoadState.NONE

                        LazyColumn(
                            Modifier.graphicsLayer { alpha = state_alpha.value },
                            state = scroll_state,
                            contentPadding = content_padding,
                            userScrollEnabled = !state_alpha.isRunning
                        ) {
                            item {
                                TopContent()
                            }

                            item {
                                if (artists_layout.items.isNotEmpty()) {
                                    artists_layout.Layout(multiselect_context = player.main_multiselect_context, apply_filter = true)
                                }
                            }

                            itemsIndexed(state as List<MediaItemLayout>) { index, layout ->
                                if (layout.items.isEmpty()) {
                                    return@itemsIndexed
                                }

                                val type = layout.type ?: MediaItemLayout.Type.GRID
                                type.Layout(
                                    layout, 
                                    Modifier.padding(bottom = 20.dp), 
                                    multiselect_context = player.main_multiselect_context, 
                                    apply_filter = true
                                )
                            }

                            item {
                                Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                                    val (requestContinuation, loading) = data

                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                        if (loading) {
                                            CircularProgressIndicator(color = Theme.on_background)
                                        }
                                        else if (requestContinuation != null) {
                                            IconButton({ requestContinuation() }) {
                                                Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.on_background)
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
                            MainPageLoadingView(Modifier.graphicsLayer { alpha = state_alpha.value }.fillMaxSize())
                        }
                    }

                    // Offline
                    else -> {
                        var error_dismissed by remember { mutableStateOf(false) }

                        Column(
                            Modifier
                                .graphicsLayer { alpha = state_alpha.value }
                                .padding(content_padding)
                                .fillMaxHeight()
                        ) {
                            if (state is Throwable) {
                                AnimatedVisibility(!error_dismissed) {
                                    ErrorInfoDisplay(
                                        state,
                                        modifier = Modifier.padding(bottom = 20.dp),
                                        expanded_modifier = Modifier.padding(bottom = SpMp.context.getDefaultVerticalPadding()),
                                        message = getString("error_yt_feed_parse_failed"),
                                        onDismiss = {
                                            error_dismissed = true
                                        }
                                    )
                                }
                            }

                            LibraryPage(
                                PaddingValues(bottom = LocalPlayerState.current.bottom_padding_dp),
                                close = {},
                                inline = true,
                                outer_multiselect_context = LocalPlayerState.current.main_multiselect_context
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun populateArtistsLayout(artists_layout: MediaItemLayout, layoutsProvider: () -> List<MediaItemLayout>, own_channel: Artist?) {
    val artists_map: MutableMap<Artist, Int?> = mutableMapOf()
    for (layout in layoutsProvider()) {
        for (item in layout.items) {
            if (item is Artist) {
                artists_map[item] = null
                continue
            }

            item.artist?.also { artist ->
                if (artist == own_channel) {
                    return@also
                }

                if (artists_map.containsKey(artist)) {
                    val current = artists_map[artist]
                    if (current != null) {
                        artists_map[artist] = current + 1
                    }
                }
                else {
                    artists_map[artist] = 1
                }
            }
        }
    }

    val artists = artists_map.mapNotNull { artist ->
        if (artist.value == null || artist.value!! < 2) null
        else Pair(artist.key, artist.value)
    }.sortedByDescending { it.second }

    artists_layout.items.clear()
    for (artist in artists) {
        artists_layout.items.add(artist.first)
    }
}
