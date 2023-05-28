package com.spectre7.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.YoutubeMusicAuthInfo
import com.spectre7.spmp.platform.ProjectPreferences
import com.spectre7.spmp.platform.composable.SwipeRefresh
import com.spectre7.spmp.platform.getDefaultHorizontalPadding
import com.spectre7.spmp.ui.component.LazyMediaItemLayoutColumn
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.LibraryPage

@Composable
fun MainPage(
    pinned_items: MutableList<MediaItem>,
    layoutsProvider: () -> List<MediaItemLayout>,
    scroll_state: LazyListState,
    feed_load_state: MutableState<FeedLoadState>,
    can_continue_feed: Boolean,
    getFilterChips: () -> List<Pair<Int, String>>?,
    getSelectedFilterChip: () -> Int?,
    pill_menu: PillMenu,
    loadFeed: (filter_chip: Int?, continuation: Boolean) -> Unit
) {
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
    var auth_info: YoutubeMusicAuthInfo by remember { mutableStateOf(YoutubeMusicAuthInfo(Settings.KEY_YTM_AUTH.get())) }

    LaunchedEffect(layoutsProvider()) {
        populateArtistsLayout(artists_layout, layoutsProvider, auth_info.getOwnChannelOrNull())
    }

    DisposableEffect(Unit) {
        val prefs_listener = Settings.prefs.addListener(object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
                if (key == Settings.KEY_YTM_AUTH.name) {
                    auth_info = YoutubeMusicAuthInfo(Settings.KEY_YTM_AUTH.get(prefs))
                }
            }
        })

        onDispose {
            Settings.prefs.removeListener(prefs_listener)
        }
    }

    Column(Modifier.padding(horizontal = padding)) {

        MainPageTopBar(
            auth_info,
            { if (feed_load_state.value == FeedLoadState.LOADING) null else getFilterChips() },
            getSelectedFilterChip,
            { loadFeed(it, false) },
            Modifier.padding(top = SpMp.context.getStatusBarHeight())
        )

        // Main scrolling view
        SwipeRefresh(
            state = feed_load_state.value == FeedLoadState.LOADING,
            onRefresh = { loadFeed(getSelectedFilterChip(), false) },
            swipe_enabled = feed_load_state.value == FeedLoadState.NONE,
            indicator = false
        ) {
            val layouts_empty by remember { derivedStateOf { layoutsProvider().isEmpty() } }
            val state = if (feed_load_state.value == FeedLoadState.LOADING) null else !layouts_empty
            var current_state by remember { mutableStateOf(state) }
            val state_alpha = remember { Animatable(1f) }

            LaunchedEffect(state) {
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
                    LazyMediaItemLayoutColumn(
                        layoutsProvider,
                        layout_modifier = Modifier.graphicsLayer { alpha = state_alpha.value },
                        padding = PaddingValues(
                            bottom = LocalPlayerState.current.bottom_padding
                        ),
                        onContinuationRequested = if (can_continue_feed) {
                            { loadFeed(getSelectedFilterChip(), true) }
                        } else null,
                        continuation_alignment = Alignment.Start,
                        loading_continuation = feed_load_state.value != FeedLoadState.NONE,
                        scroll_state = scroll_state,
                        scroll_enabled = !state_alpha.isRunning,
                        spacing = 30.dp,
                        topContent = {
                            item {
                                TopContent()
                            }
                        },
                        layoutItem = { layout, i, showLayout ->
                            if (i == 0 && artists_layout.items.isNotEmpty()) {
                                artists_layout.also { showLayout(this, it) }
                            }

                            showLayout(this, layout)
                        },
                        multiselect_context = LocalPlayerState.current.main_multiselect_context
                    ) { it.type ?: MediaItemLayout.Type.GRID }
                }
                // Offline
                false -> {
                    LibraryPage(
                        pill_menu,
                        Modifier.graphicsLayer { alpha = state_alpha.value },
                        close = {},
                        inline = true,
                        outer_multiselect_context = LocalPlayerState.current.main_multiselect_context
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
