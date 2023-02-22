@file:OptIn(ExperimentalMaterialApi::class)

package com.spectre7.spmp.ui.layout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.getHomeFeed
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.component.*
import com.spectre7.spmp.ui.layout.nowplaying.NowPlaying
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

@Composable
fun getScreenHeight(): Dp {
    return LocalConfiguration.current.screenHeightDp.dp + getStatusBarHeight(MainActivity.context)
}

const val MINIMISED_NOW_PLAYING_HEIGHT: Int = 64
enum class OverlayPage { NONE, SEARCH, SETTINGS, MEDIAITEM }

data class PlayerViewContext(
    private val onClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val base: PlayerViewContext? = null
) {

    fun copy(onClickedOverride: ((item: MediaItem) -> Unit)? = null, onLongClickedOverride: ((item: MediaItem) -> Unit)? = null): PlayerViewContext {
        return PlayerViewContext(onClickedOverride, onLongClickedOverride, this)
    }

    val pill_menu = PillMenu(
        top = false
    )

    var overlay_page by mutableStateOf(OverlayPage.NONE)
    var overlay_media_item: MediaItem? by mutableStateOf(null)
    
    fun onMediaItemClicked(item: MediaItem) {
        if (onClickedOverride != null) {
            onClickedOverride.invoke(item)
            return
        }

        if (base != null) {
            base.onMediaItemClicked(item)
            return
        }

        when (item) {
            is Song -> PlayerServiceHost.service.playSong(item)
            else -> openMediaItem(item)
        }
    }
    fun onMediaItemLongClicked(item: MediaItem) {
        if (onLongClickedOverride != null) {
            onLongClickedOverride.invoke(item)
            return
        }

        if (base != null) {
            base.onMediaItemLongClicked(item)
            return
        }

        showLongPressMenu(LongPressMenuData(item, actions = when (item) {
            is Song -> songLongPressPopupActions
            is Artist -> artistLongPressPopupActions
            else -> null
        }))
    }

    fun openMediaItem(item: MediaItem) {
        if (base != null) {
            base.openMediaItem(item)
            return
        }

        overlay_page = OverlayPage.MEDIAITEM
        overlay_media_item = item
    }

    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)

    fun showLongPressMenu(data: LongPressMenuData) {
        if (base != null) {
            base.showLongPressMenu(data)
            return
        }

        long_press_menu_data = data

        if (long_press_menu_showing) {
            long_press_menu_direct = true
        }
        else {
            long_press_menu_showing = true
            long_press_menu_direct = false
        }
    }

    fun hideLongPressMenu() {
        if (base != null) {
            base.hideLongPressMenu()
            return
        }
        long_press_menu_showing = false
        long_press_menu_direct = false
    }

    @Composable
    internal fun LongPressMenu() {
        var height by remember { mutableStateOf(0) }

        Crossfade(long_press_menu_data) { data ->
            if (data != null) {
                val current = data == long_press_menu_data
                var height_found by remember { mutableStateOf(false) }

                LongPressIconMenu(
                    long_press_menu_showing && current,
                    (long_press_menu_direct && current) || data.thumb_shape == null,
                    {
                        if (current) {
                            hideLongPressMenu()
                        }
                    },
                    { this },
                    data,
                    Modifier
                        .onSizeChanged {
                            height = maxOf(height, it.height)
                            height_found = true
                        }
                        .height(if (height_found && height > 0) with(LocalDensity.current) { height.toDp() } else Dp.Unspecified)
                )
            }
        }
    }
}

@Composable
fun PlayerView() {
    val player = remember { PlayerViewContext() }
    val playerProvider = remember { { player } }
    player.LongPressMenu()

    LaunchedEffect(player.overlay_page) {
        if (player.overlay_page == OverlayPage.NONE) {
            player.pill_menu.clearExtraActions()
            player.pill_menu.clearActionOverriders()
            player.pill_menu.setBackgroundColourOverride(null)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MainActivity.theme.getBackground(false))
    ) {
        val now_playing_swipe_state = rememberSwipeableState(0)

        Box {
            val expand_state = remember { mutableStateOf(false) }
            val overlay_open by remember { derivedStateOf { player.overlay_page != OverlayPage.NONE } }

            player.pill_menu.PillMenu(
                if (overlay_open) 1 else 2,
                { index, action_count ->
                    ActionButton(
                        if (action_count == 1) Icons.Filled.Close else
                            when (index) {
                                0 -> Icons.Filled.Settings
                                else -> Icons.Filled.Search
                            }
                    ) {
                        player.overlay_page = if (action_count == 1) OverlayPage.NONE else
                            when (index) {
                                0 -> OverlayPage.SETTINGS
                                else -> OverlayPage.SEARCH
                            }
                    }
                },
                if (!overlay_open) expand_state else null,
                MainActivity.theme.getAccentProvider(),
                container_modifier = Modifier.offset { IntOffset(x = 0, y = -now_playing_swipe_state.offset.value.dp.toPx().toInt()) }
            )

            val main_page_layouts = remember { mutableStateListOf<MediaItemLayout>() }

            LaunchedEffect(Unit) {
                refreshFeed(true, main_page_layouts) {}
            }

            val main_page_scroll_state = rememberLazyListState()

            Crossfade(targetState = player.overlay_page) {
                Column(Modifier.fillMaxSize()) {
                    if (it != OverlayPage.NONE && it != OverlayPage.MEDIAITEM) {
                        Spacer(Modifier.requiredHeight(getStatusBarHeight(MainActivity.context)))
                    }
                    when (it) {
                        OverlayPage.NONE -> MainPage(main_page_layouts, playerProvider, main_page_scroll_state)
                        OverlayPage.SEARCH -> SearchPage(player.pill_menu, playerProvider) { player.overlay_page = OverlayPage.NONE }
                        OverlayPage.SETTINGS -> PrefsPage(player.pill_menu) { player.overlay_page = OverlayPage.NONE }
                        OverlayPage.MEDIAITEM -> Crossfade(player.overlay_media_item) { item ->
                            when (item) {
                                null -> {}
                                is Artist -> ArtistPage(player.pill_menu, item, playerProvider) { player.overlay_page = OverlayPage.NONE }
                                is Playlist -> PlaylistPage(player.pill_menu, item, playerProvider) { player.overlay_page = OverlayPage.NONE }
                                else -> throw NotImplementedError()
                            }
                        }
                    }
                }
            }
        }

        NowPlaying(playerProvider, now_playing_swipe_state)
    }
}

@Composable
private fun MainPage(
    layouts: MutableList<MediaItemLayout>,
    playerProvider: () -> PlayerViewContext,
    scroll_state: LazyListState
) {
    var refreshing by remember { mutableStateOf(false) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing),
        onRefresh = {
            refreshing = true
            refreshFeed(false, layouts) {
                refreshing = false
            }
        },
        Modifier.padding(horizontal = 10.dp)
    ) {
        Crossfade(remember { derivedStateOf { layouts.isNotEmpty() } }.value) { loaded ->
            if (loaded) {
//                CompositionLocalProvider(
//                    LocalOverscrollConfiguration provides null
//                ) {
                    LazyMediaItemLayoutColumn(
                        layouts,
                        playerProvider,
                        top_padding = getStatusBarHeight(MainActivity.context),
                        bottom_padding = MINIMISED_NOW_PLAYING_HEIGHT.dp,
                        scroll_state = scroll_state
                    )
//                }
            }
            else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(getString(R.string.loading_feed), Modifier.alpha(0.4f), fontSize = 12.sp, color = MainActivity.theme.getOnBackground(false))
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        Modifier
                            .alpha(0.4f)
                            .fillMaxWidth(0.35f), color = MainActivity.theme.getOnBackground(false))
                }
            }
        }
    }
}

private val feed_refresh_lock = ReentrantLock()

private fun refreshFeed(allow_cached: Boolean, feed_layouts: MutableList<MediaItemLayout>, onFinished: (success: Boolean) -> Unit) {
    thread {
        if (!feed_refresh_lock.tryLock()) {
            return@thread
        }
        else {
            feed_layouts.clear()

            val feed_result = getHomeFeed(allow_cached = allow_cached)

            if (!feed_result.isSuccess) {
                MainActivity.error_manager.onError("refreshFeed", feed_result.exceptionOrNull()!!) { result ->
                    refreshFeed(false, feed_layouts) { success -> result(success, null) }
                }
                feed_refresh_lock.unlock()
                onFinished(false)
                return@thread
            }

            val rows = mutableListOf<MediaItemLayout>()
            val request_limit = Semaphore(10) // TODO?

            runBlocking { withContext(Dispatchers.IO) { coroutineScope {
                for (row in feed_result.getOrThrowHere()) {
                    val entry = MediaItemLayout(row.title, row.subtitle, MediaItemLayout.Type.GRID)
                    rows.add(entry)

                    for (item in row.items) {
                        if (item.title != null) {
                            entry.addItem(item)
                            continue
                        }

                        launch {
                            request_limit.withPermit {
                                item.loadData().onSuccess { loaded ->
                                    synchronized(request_limit) {
                                        entry.addItem(loaded)
                                    }
                                }
                            }
                        }
                    }
                }
            }}}

            for (row in rows) {
                if (row.items.isNotEmpty()) {
                    feed_layouts.add(row)
                }
            }

            feed_refresh_lock.unlock()
            onFinished(true)
        }
    }
}
