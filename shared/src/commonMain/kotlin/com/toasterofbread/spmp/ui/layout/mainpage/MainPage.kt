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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
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

@Composable
fun MainPage(
    pinned_items: List<MediaItemHolder>,
    getLayouts: () -> List<MediaItemLayout>,
    scroll_state: LazyListState,
    feed_load_state: MutableState<FeedLoadState>,
    feed_load_error: Throwable?,
    can_continue_feed: Boolean,
    getFilterChips: () -> List<FilterChip>?,
    getSelectedFilterChip: () -> Int?,
    pill_menu: PillMenu,
    loadFeed: (filter_chip: Int?, continuation: Boolean) -> Unit
) {
    val player = LocalPlayerState.current
    val padding by animateDpAsState(SpMp.context.getDefaultHorizontalPadding())

    val artists_layout: MediaItemLayout = remember {
        MediaItemLayout(
            mutableStateListOf(),
            null,
            null,
            type = MediaItemLayout.Type.ROW
        )
    }

    LaunchedEffect(getLayouts()) {
        populateArtistsLayout(
            artists_layout.items as MutableList<MediaItem>,
            getLayouts,
            Api.ytm_auth.getOwnChannelOrNull(),
            SpMp.context.database
        )
    }

    Column(Modifier.padding(horizontal = padding)) {

        val top_padding = WAVE_BORDER_DEFAULT_HEIGHT.dp
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
            val target_state = if (feed_load_state.value == FeedLoadState.LOADING || feed_load_state.value == FeedLoadState.PREINIT) null else getLayouts().ifEmpty { feed_load_error ?: false }
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

            val top_content_visible = pinned_items.isNotEmpty()
            @Composable
            fun TopContent() {
                MainPageScrollableTopContent(pinned_items, Modifier.padding(bottom = 10.dp), top_content_visible)
            }

            current_state.also { state ->
                when (state) {
                    // Loaded
                    is List<*> -> {
                        val onContinuationRequested = if (can_continue_feed) {
                            { loadFeed(getSelectedFilterChip(), true) }
                        } else null
                        val loading_continuation = feed_load_state.value != FeedLoadState.NONE

                        LazyColumn(
                            Modifier.graphicsLayer { alpha = state_alpha.value },
                            state = scroll_state,
                            contentPadding = PaddingValues(
                                bottom = LocalPlayerState.current.bottom_padding_dp,
                                top = top_padding
                            ),
                            userScrollEnabled = !state_alpha.isRunning,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            if (top_content_visible || artists_layout.items.isNotEmpty()) {
                                item {
                                    Column {
                                        TopContent()
                                        if (artists_layout.items.isNotEmpty()) {
                                            artists_layout.Layout(multiselect_context = player.main_multiselect_context, apply_filter = true)
                                        }
                                    }
                                }
                            }

                            itemsIndexed(state as List<MediaItemLayout>) { index, layout ->
                                if (layout.items.isEmpty()) {
                                    return@itemsIndexed
                                }

                                val type = layout.type ?: MediaItemLayout.Type.GRID
                                type.Layout(layout, multiselect_context = player.main_multiselect_context, apply_filter = true)
                            }

                            item {
                                Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                        if (data.second) {
                                            CircularProgressIndicator(color = Theme.on_background)
                                        }
                                        else if (data.first != null) {
                                            IconButton({ data.first!!.invoke() }) {
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
                                .padding(top = top_padding)
                                .fillMaxHeight()
                        ) {
                            if (state is Throwable) {
                                AnimatedVisibility(!error_dismissed) {
                                    ErrorInfoDisplay(
                                        state,
                                        expanded_modifier = Modifier.padding(bottom = SpMp.context.getDefaultVerticalPadding()),
                                        message = getString("error_yt_feed_parse_failed"),
                                        onDismiss = {
                                            error_dismissed = true
                                        }
                                    )
                                }
                            }

                            LibraryPage(
                                pill_menu,
                                LocalPlayerState.current.bottom_padding_dp,
                                close = {},
                                inline = true,
                                outer_multiselect_context = LocalPlayerState.current.main_multiselect_context,
                                mainTopContent = { TopContent() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun populateArtistsLayout(
    artists_layout_items: MutableList<MediaItem>,
    layoutsProvider: () -> List<MediaItemLayout>,
    own_channel: Artist?,
    db: Database
) {
    val artists_map: MutableMap<Artist, Int?> = mutableMapOf()
    for (layout in layoutsProvider()) {
        for (item in layout.items) {
            if (item is Artist) {
                artists_map[item] = null
                continue
            }

            if (item is MediaItem.WithArtist) {
                item.Artist.get(db)?.also { artist ->
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
    }

    val artists = artists_map.mapNotNull { artist ->
        if (artist.value == null || artist.value!! < 2) null
        else Pair(artist.key, artist.value)
    }.sortedByDescending { it.second }

    artists_layout_items.clear()
    for (artist in artists) {
        artists_layout_items.add(artist.first)
    }
}
