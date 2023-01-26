package com.spectre7.spmp.ui.layout

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.getHomeFeed
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.AutoResizeText
import com.spectre7.spmp.ui.component.FontSizeRange
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.nowplaying.NowPlaying
import com.spectre7.utils.*
import java.lang.Integer.min
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.math.ceil

@Composable
fun getScreenHeight(): Float {
    return LocalConfiguration.current.screenHeightDp.toFloat() + getStatusBarHeight(MainActivity.context).value
}

const val MINIMISED_NOW_PLAYING_HEIGHT = 64f
enum class OverlayPage { NONE, SEARCH, SETTINGS }

data class YtItemRow(val title: String, val subtitle: String?, val type: TYPE, val items: MutableList<Pair<MediaItem, Long>> = mutableStateListOf()) {

    enum class TYPE { SQUARE, LONG }

    @Composable
    private fun ItemPreview(item: MediaItem, height: Dp, animate_visibility: Boolean, modifier: Modifier = Modifier) {
        Box(modifier.requiredHeight(height), contentAlignment = Alignment.Center) {
            if(animate_visibility) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(visible) {
                    visible = true
                }
                AnimatedVisibility(
                    visible,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
                ) {
                    item.PreviewSquare(MainActivity.theme.getOnBackground(false), null, null, Modifier)
                }
            }
            else {
                item.PreviewSquare(MainActivity.theme.getOnBackground(false), null, null, Modifier)
            }
        }
    }

    @Composable
    private fun SquareList() {
        val row_count = 2
        LazyHorizontalGrid(
            rows = GridCells.Fixed(row_count),
            modifier = Modifier.requiredHeight(140.dp * row_count)
        ) {
            items(items.size, { items[it].first.id }) {
                val item = items[it]
                ItemPreview(item.first, 130.dp, remember { System.currentTimeMillis() - item.second < 250})
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    private fun LongList(rows: Int = 5, columns: Int = 1) {
        HorizontalPager(ceil((items.size / rows.toFloat()) / columns.toFloat()).toInt()) { page ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val start = page * rows * columns
                for (column in 0 until columns) {
                    Column(Modifier.weight(1f)) {
                        for (i in start + rows * column until min(start + (rows * columns), start + (rows * (column + 1)))) {
                            if (i < items.size) {
                                val item = items[i]
                                ItemPreview(item.first, 50.dp, remember { System.currentTimeMillis() - item.second < 250})
                            }
                            else {
                                Spacer(Modifier.requiredHeight(50.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ItemRow() {
        if (items.isEmpty()) {
            return
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Column {
                    AutoResizeText(
                        text = title,
                        maxLines = 1,
                        fontSizeRange = FontSizeRange(
                            20.sp, 30.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )

                    if (subtitle != null) {
                        Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onBackground.setAlpha(0.5))
                    }
                }

                when (type) {
                    TYPE.SQUARE -> SquareList()
                    TYPE.LONG -> LongList(3, 2)
                }
            }
        }
    }

    fun add(item: MediaItem?) {
        if (item != null && items.firstOrNull { it.first.id == item.id } == null) {
            items.add(Pair(item, System.currentTimeMillis()))
        }
    }
}

val feed_refresh_mutex = ReentrantLock()

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerView() {
    var overlay_page by remember { mutableStateOf(OverlayPage.NONE) }
    val pill_menu = remember { PillMenu(
        top = false
    ) }
    val minimised_now_playing_height = remember { Animatable(0f) }

    LaunchedEffect(overlay_page) {
        if (overlay_page == OverlayPage.NONE) {
            pill_menu.clearExtraActions()
            pill_menu.clearActionOverriders()
        }
    }

    LaunchedEffect(PlayerServiceHost.session_started) {
        if (PlayerServiceHost.session_started) {
            minimised_now_playing_height.animateTo(MINIMISED_NOW_PLAYING_HEIGHT)
        }
    }

    Column(Modifier.fillMaxSize()) {

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
                MainActivity.theme.getAccent(),
                MainActivity.theme.getAccent().getContrasted(),
            )

            val main_page_rows = remember { mutableStateListOf<YtItemRow>() }

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

                        val artist_row =
                            YtItemRow(getString(R.string.feed_row_artists), null, YtItemRow.TYPE.LONG)
                        val playlist_row =
                            YtItemRow(getString(R.string.feed_row_playlists), null, YtItemRow.TYPE.SQUARE)

                        for (row in feed_result.data) {
                            val entry =
                                YtItemRow(row.title, row.subtitle, YtItemRow.TYPE.SQUARE)
                            var entry_added = false

                            for (item in row.items) {
                                thread {
                                    val previewable = item.getPreviewable().loadData()
                                    synchronized(main_page_rows) {
                                        when (previewable) {
                                            is Song -> {
                                                if (!entry_added) {
                                                    entry_added = true
                                                    main_page_rows.add(entry)
                                                }
                                                entry.add(previewable)
                                                artist_row.add(previewable.artist)
                                            }
                                            is Artist -> artist_row.add(previewable)
                                            is Playlist -> playlist_row.add(previewable)
                                        }
                                    }
                                }
                            }
                        }

                        main_page_rows.add(artist_row)
                        main_page_rows.add(playlist_row)

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
                    if (it != OverlayPage.NONE) {
                        Spacer(Modifier.requiredHeight(getStatusBarHeight(MainActivity.context)))
                    }
                    when (it) {
                        OverlayPage.NONE -> MainPage(main_page_rows, refreshFeed)
                        OverlayPage.SEARCH -> SearchPage(pill_menu) { overlay_page = it }
                        OverlayPage.SETTINGS -> PrefsPage(pill_menu) { overlay_page = it }
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
                    NowPlaying(swipe_state.offset.value / screen_height, screen_height) { switch = !switch }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPage(_rows: List<YtItemRow>, refreshFeed: (allow_cache: Boolean, onFinished: (success: Boolean) -> Unit) -> Unit) {

    var rows: List<YtItemRow> by remember { mutableStateOf(_rows) }

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
                        items(rows.size) { index ->
                            rows[index].ItemRow()
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
