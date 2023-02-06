@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.getHomeFeed
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
fun getScreenHeight(): Float {
    return LocalConfiguration.current.screenHeightDp.toFloat() + getStatusBarHeight(MainActivity.context).value
}

const val MINIMISED_NOW_PLAYING_HEIGHT = 64f
enum class OverlayPage { NONE, SEARCH, SETTINGS, MEDIAITEM }

data class PlayerViewContext(
    private val onClickedOverride: ((item: MediaItem) -> Unit)? = null,
    private val onLongClickedOverride: ((item: MediaItem) -> Unit)? = null
) {
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
    }
    fun openMediaItem(item: MediaItem) {
        overlay_page = OverlayPage.MEDIAITEM
        overlay_media_item = item
    }

    private var long_press_menu_data: LongPressMenuData? by mutableStateOf(null)
    private var long_press_menu_showing: Boolean by mutableStateOf(false)
    private var long_press_menu_direct: Boolean by mutableStateOf(false)

    fun showLongPressMenu(data: LongPressMenuData) {
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
                    long_press_menu_direct && current,
                    {
                        if (current) {
                            hideLongPressMenu()
                        }
                    },
                    this,
                    data,
                    Modifier
                        .onSizeChanged {
                            height = maxOf(height, it.height)
                            height_found = true
                        }
                        .height(if (height_found && height > 0) with (LocalDensity.current) { height.toDp() } else Dp.Unspecified)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerView() {
    val player = remember { PlayerViewContext() }
    player.LongPressMenu()

    LaunchedEffect(player.overlay_page) {
        if (player.overlay_page == OverlayPage.NONE) {
            player.pill_menu.clearExtraActions()
            player.pill_menu.clearActionOverriders()
            player.pill_menu.setBackgroundColourOverride(null)
        }
    }

    Column(Modifier
        .fillMaxSize()
        .background(MainActivity.theme.getBackground(false))) {

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
                MainActivity.theme.getAccentProvider()
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
                        OverlayPage.NONE -> MainPage(main_page_layouts, player, main_page_scroll_state)
                        OverlayPage.SEARCH -> SearchPage(player.pill_menu, player) { player.overlay_page = OverlayPage.NONE }
                        OverlayPage.SETTINGS -> PrefsPage(player.pill_menu) { player.overlay_page = OverlayPage.NONE }
                        OverlayPage.MEDIAITEM -> Crossfade(player.overlay_media_item) { item ->
                            when (item) {
                                null -> {}
                                is Artist -> ArtistPage(player.pill_menu, item, player) { player.overlay_page = OverlayPage.NONE }
                                is Playlist -> PlaylistPage(player.pill_menu, item, player) { player.overlay_page = OverlayPage.NONE }
                                else -> throw NotImplementedError()
                            }
                        }
                    }
                }
            }
        }

        NowPlaying(player)
    }
}

@Composable
private fun NowPlaying(player: PlayerViewContext) {
    AnimatedVisibility(PlayerServiceHost.session_started, enter = slideInVertically(), exit = slideOutVertically()) {
        val screen_height = getScreenHeight()
        val swipe_state = rememberSwipeableState(0)

        var switch: Boolean by remember { mutableStateOf(false) }

        OnChangedEffect(switch) {
            if (swipe_state.targetValue == switch.toInt()) {
                swipe_state.animateTo(if (swipe_state.targetValue == 1) 0 else 1)
            }
            else {
                swipe_state.animateTo(switch.toInt())
            }
        }

        Card(colors = CardDefaults.cardColors(
            containerColor = MainActivity.theme.getBackground(true)
        ), modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(screen_height.dp)
            .offset(y = (screen_height.dp / 2) - swipe_state.offset.value.dp)
            .swipeable(
                state = swipe_state,
                anchors = mapOf(MINIMISED_NOW_PLAYING_HEIGHT to 0, screen_height to 1),
                thresholds = { _, _ -> FractionalThreshold(0.2f) },
                orientation = Orientation.Vertical,
                reverseDirection = true,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                enabled = swipe_state.targetValue == 0,
                indication = null
            ) { switch = !switch }, shape = RectangleShape) {

            Column(Modifier.fillMaxSize()) {
                NowPlaying(swipe_state.offset.value / screen_height, screen_height, { switch = !switch }, player)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainPage(
    layouts: MutableList<MediaItemLayout>,
    player: PlayerViewContext,
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
                CompositionLocalProvider(
                    LocalOverScrollConfiguration provides null
                ) {
                    MediaItemLayoutColumn(
                        layouts,
                        player,
                        top_padding = getStatusBarHeight(MainActivity.context),
                        bottom_padding = MINIMISED_NOW_PLAYING_HEIGHT.dp,
                        scroll_state = scroll_state
                    )
                }
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

            if (!feed_result.success) {
                // MainActivity.error_manager.onError(feed_result.exception) { resolve ->
                //     refreshFeed(false, feed_layouts) { success -> if (it) resolve() }
                // }
                feed_refresh_lock.unlock()
                onFinished(false)
                return@thread
            }

            val artists = MediaItemLayout(getString(R.string.feed_row_artists), null, MediaItemLayout.Type.GRID)
            val playlists = MediaItemLayout(getString(R.string.feed_row_playlists), null, MediaItemLayout.Type.GRID)

            val rows = mutableListOf<MediaItemLayout>()
            val request_limit = Semaphore(10)

            runBlocking { withContext(Dispatchers.IO) { coroutineScope {
                for (row in feed_result.data) {
                    val entry = MediaItemLayout(row.title, row.subtitle, MediaItemLayout.Type.GRID)
                    rows.add(entry)

                    for (item in row.items) {
                        val media_item = item.toMediaItem()
                        launch {
                            request_limit.withPermit {
                                val loaded = media_item.getOrReplacedWith().loadData()
                                synchronized(request_limit) {
                                    when (loaded) {
                                        is Song -> {
                                            entry.addItem(loaded)
                                            artists.addItem(loaded.artist)
                                        }
                                        is Artist -> artists.addItem(loaded)
                                        is Playlist -> playlists.addItem(media_item)
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
            if (artists.items.isNotEmpty()) {
                feed_layouts.add(artists)
            }
            if (playlists.items.isNotEmpty()) {
                feed_layouts.add(playlists)
            }

            feed_refresh_lock.unlock()
            onFinished(true)
        }
    }
}