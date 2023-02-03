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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalConfiguration
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

val feed_refresh_mutex = ReentrantLock()

abstract class PlayerViewContext {
    abstract fun onMediaItemCLicked(item: MediaItem)
    abstract fun onMediaItemLongClicked(item: MediaItem)
    abstract fun openMediaItem(item: MediaItem)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerView() {
    var overlay_page by remember { mutableStateOf(OverlayPage.NONE) }
    var overlay_media_item: MediaItem? by remember { mutableStateOf(null) }

    val onMediaItemClicked: (MediaItem) -> Unit = { item ->
        when (item) {
            is Song -> PlayerServiceHost.service.playSong(item)
            else -> {
                overlay_page = OverlayPage.MEDIAITEM
                overlay_media_item = item
            }
        }
    }

    val pill_menu = remember { PillMenu(
        top = false
    ) }
    val minimised_now_playing_height = remember { Animatable(0f) }

    LaunchedEffect(overlay_page) {
        if (overlay_page == OverlayPage.NONE) {
            pill_menu.clearExtraActions()
            pill_menu.clearActionOverriders()
            pill_menu.setBackgroundColourOverride(null)
        }
    }

    LaunchedEffect(PlayerServiceHost.session_started) {
        if (PlayerServiceHost.session_started) {
            minimised_now_playing_height.animateTo(MINIMISED_NOW_PLAYING_HEIGHT)
        }
    }

    Column(Modifier.fillMaxSize().background(MainActivity.theme.getBackground(false))) {

        Box(Modifier.padding(bottom = minimised_now_playing_height.value.dp)) {

            pill_menu.PillMenu(
                if (overlay_page != OverlayPage.NONE) 1 else 2,
                { index, action_count ->
                    ActionButton(
                        if (action_count == 1) Icons.Filled.Close else
                            when (index) {
                                0 -> Icons.Filled.Settings
                                else -> Icons.Filled.Search
                            }
                    ) {
                        overlay_page = if (action_count == 1) OverlayPage.NONE else
                            when (index) {
                                0 -> OverlayPage.SETTINGS
                                else -> OverlayPage.SEARCH
                            }
                    }
                },
                if (overlay_page == OverlayPage.NONE) remember { mutableStateOf(false) } else null,
                MainActivity.theme.getAccent()
            )

            val main_page_rows = remember { mutableStateListOf<MediaItemRow>() }

            lateinit var refreshFeed: (allow_cached: Boolean, onFinished: (success: Boolean) -> Unit) -> Unit
            refreshFeed = { allow_cached: Boolean, onFinished: (success: Boolean) -> Unit ->
                if (!feed_refresh_mutex.isLocked) {
                    thread {
                        feed_refresh_mutex.lock()
                        main_page_rows.clear()

                        val feed_result = getHomeFeed()

                        if (!feed_result.success) {
                            MainActivity.error_manager.onError(feed_result.exception) { resolve ->
                                refreshFeed(true) { if (it) resolve() }
                            }
                            feed_refresh_mutex.unlock()
                            onFinished(false)
                            return@thread
                        }

                        val artists = MediaItemRow(getString(R.string.feed_row_artists), null)
                        val playlists = MediaItemRow(getString(R.string.feed_row_playlists), null)

                        val rows = mutableListOf<MediaItemRow>()
                        val request_limit = Semaphore(10)

                        runBlocking { withContext(Dispatchers.IO) { coroutineScope {
                            for (row in feed_result.data) {
                                val entry = MediaItemRow(row.title, row.subtitle)
                                rows.add(entry)

                                for (item in row.items) {
                                    val media_item = item.toMediaItem()
                                    launch {
                                        request_limit.withPermit {
                                            val loaded = media_item.getOrReplacedWith().loadData()
                                            synchronized(request_limit) {
                                                when (loaded) {
                                                    is Song -> {
                                                        entry.add(loaded)
                                                        artists.add(loaded.artist)
                                                    }
                                                    is Artist -> artists.add(loaded)
                                                    is Playlist -> playlists.add(media_item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }}}

                        for (row in rows) {
                            if (row.items.isNotEmpty()) {
                                main_page_rows.add(row)
                            }
                        }
                        if (artists.items.isNotEmpty()) {
                            main_page_rows.add(artists)
                        }
                        if (playlists.items.isNotEmpty()) {
                            main_page_rows.add(playlists)
                        }

                        feed_refresh_mutex.unlock()
                        onFinished(true)
                    }
                }
            }

            LaunchedEffect(Unit) {
                refreshFeed(true) {}
            }

            Crossfade(targetState = overlay_page) {
                Column(Modifier.fillMaxSize()) {
                    if (it != OverlayPage.NONE && it != OverlayPage.MEDIAITEM) {
                        Spacer(Modifier.requiredHeight(getStatusBarHeight(MainActivity.context)))
                    }
                    when (it) {
                        OverlayPage.NONE -> MainPage(main_page_rows, refreshFeed, onMediaItemClicked)
                        OverlayPage.SEARCH -> SearchPage(pill_menu) { overlay_page = OverlayPage.NONE }
                        OverlayPage.SETTINGS -> PrefsPage(pill_menu) { overlay_page = OverlayPage.NONE }
                        OverlayPage.MEDIAITEM -> Crossfade(overlay_media_item) { item ->
                            when (item) {
                                null -> {}
                                is Artist -> ArtistPage(pill_menu, item, { overlay_page = OverlayPage.NONE }, onMediaItemClicked)
                                else -> throw NotImplementedError()
                            }
                        }
                    }
                }
            }
        }

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
                    NowPlaying(swipe_state.offset.value / screen_height, screen_height, { switch = !switch }, onMediaItemClicked)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPage(
    _rows: List<MediaItemRow>,
    refreshFeed: (allow_cache: Boolean, onFinished: (success: Boolean) -> Unit) -> Unit,
    onMediaItemClicked: (MediaItem) -> Unit
) {
    var rows: List<MediaItemRow> by remember { mutableStateOf(_rows) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(_rows.isEmpty()), // TODO
        onRefresh = {
            rows = _rows.toList()
            refreshFeed(false) { success ->
                if (success) {
                    rows = _rows
                }
            }
        },
        Modifier.padding(horizontal = 10.dp)
    ) {
        Crossfade(rows.isNotEmpty()) { loaded ->
            if (loaded) {
                CompositionLocalProvider(
                    LocalOverScrollConfiguration provides null
                ) {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        item {
                            Spacer(Modifier.requiredHeight(getStatusBarHeight(MainActivity.context)))
                        }
                        items(rows.size) { i ->
                            val row = rows[i]
                            MediaItemGrid(row.title, row.subtitle, row.items, onClick = onMediaItemClicked)
                        }
                    }
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
