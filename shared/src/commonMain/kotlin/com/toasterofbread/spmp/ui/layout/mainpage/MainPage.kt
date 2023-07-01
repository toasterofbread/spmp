package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
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
import com.toasterofbread.spmp.ui.component.LazyMediaItemLayoutColumn
import com.toasterofbread.spmp.ui.component.MediaItemLayout
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.library.LibraryPage
import com.toasterofbread.spmp.ui.theme.Theme

@Composable
fun MainPage(
    pinned_items: List<MediaItemHolder>,
    getLayouts: () -> List<MediaItemLayout>,
    scroll_state: LazyListState,
    feed_load_state: MutableState<FeedLoadState>,
    can_continue_feed: Boolean,
    getFilterChips: () -> List<Pair<Int, String>>?,
    getSelectedFilterChip: () -> Int?,
    pill_menu: PillMenu,
    loadFeed: (filter_chip: Int?, continuation: Boolean) -> Unit
) {
    val player = LocalPlayerState.current
    val padding by animateDpAsState(SpMp.context.getDefaultHorizontalPadding())

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

    LaunchedEffect(getLayouts()) {
        populateArtistsLayout(artists_layout, getLayouts, Api.ytm_auth.getOwnChannelOrNull())
    }

    Column(Modifier.padding(horizontal = padding)) {

        MainPageTopBar(
            Api.ytm_auth,
            getFilterChips,
            getSelectedFilterChip,
            { loadFeed(it, false) },
            Modifier.padding(top = SpMp.context.getStatusBarHeight()).zIndex(1f)
        )

        // Main scrolling view
        SwipeRefresh(
            state = feed_load_state.value == FeedLoadState.LOADING,
            onRefresh = { loadFeed(getSelectedFilterChip(), false) },
            swipe_enabled = feed_load_state.value == FeedLoadState.NONE,
            indicator = false
        ) {
            val layouts_empty by remember { derivedStateOf { getLayouts().isEmpty() } }
            val state = if (feed_load_state.value == FeedLoadState.LOADING || feed_load_state.value == FeedLoadState.PREINIT) null else !layouts_empty
            var current_state by remember { mutableStateOf(state) }
            val state_alpha = remember { Animatable(1f) }

            LaunchedEffect(state) {
                if (current_state == state) {
                    state_alpha.animateTo(1f, tween(300))
                    return@LaunchedEffect
                }

                state_alpha.animateTo(0f, tween(300))
                current_state = state
                state_alpha.animateTo(1f, tween(300))
            }

            @Composable
            fun TopContent() {
                MainPageScrollableTopContent(pinned_items, Modifier.padding(bottom = 15.dp))
            }

            when (current_state) {
                // Loaded
                true -> {
                    val onContinuationRequested = if (can_continue_feed) {
                        { loadFeed(getSelectedFilterChip(), true) }
                    } else null
                    val loading_continuation = feed_load_state.value != FeedLoadState.NONE

                    LazyColumn(
                        Modifier.graphicsLayer { alpha = state_alpha.value },
                        state = scroll_state,
                        contentPadding = PaddingValues(
                            bottom = LocalPlayerState.current.bottom_padding
                        ),
                        userScrollEnabled = !state_alpha.isRunning,
                        verticalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        item {
                            Column {
                                TopContent()
                                artists_layout.Layout(multiselect_context = player.main_multiselect_context)
                            }
                        }

                        itemsIndexed(getLayouts()) { index, layout ->
                            if (layout.items.isEmpty()) {
                                return@itemsIndexed
                            }

                            val type = layout.type ?: MediaItemLayout.Type.GRID
                            type.Layout(layout, multiselect_context = player.main_multiselect_context)
                        }

                        item {
                            Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                    if (data.second) {
                                        CircularProgressIndicator(color = Theme.current.on_background)
                                    }
                                    else if (data.first != null) {
                                        IconButton({ data.first!!.invoke() }) {
                                            Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.current.on_background)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Offline
                false -> {
                    LibraryPage(
                        pill_menu,
                        LocalPlayerState.current.bottom_padding,
                        Modifier.graphicsLayer { alpha = state_alpha.value },
                        close = {},
                        inline = true,
                        outer_multiselect_context = LocalPlayerState.current.main_multiselect_context,
                        mainTopContent = { TopContent() }
                    )
                }
                // Loading
                null -> {
                    Column(Modifier.fillMaxSize()) {
                        TopContent()
                        MainPageLoadingView(Modifier.graphicsLayer { alpha = state_alpha.value }.fillMaxSize())
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
