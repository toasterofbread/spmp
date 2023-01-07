package com.spectre7.spmp.ui.layout

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.exoplayer2.ExoPlayer
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.model.YtItem
import com.spectre7.spmp.ui.component.AutoResizeText
import com.spectre7.spmp.ui.component.FontSizeRange
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.nowplaying.NowPlaying
import com.spectre7.utils.*
import kotlinx.coroutines.runBlocking
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

data class YtItemRow(val title: String, val subtitle: String?, val type: TYPE, val items: MutableList<Pair<YtItem, Long>> = mutableStateListOf()) {

    enum class TYPE { SQUARE, LONG }

    @Composable
    private fun ItemPreview(item: YtItem, height: Dp, animate_visibility: Boolean, modifier: Modifier = Modifier) {
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
                    item.Preview(true)
                }
            }
            else {
                item.Preview(true)
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

    fun add(item: YtItem?) {
        if (item != null && items.firstOrNull { it.first.id == item.id } == null) {
            items.add(Pair(item, System.currentTimeMillis()))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerView() {
    var overlay_page by remember { mutableStateOf(OverlayPage.NONE) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = getStatusBarHeight(MainActivity.context))
    ) {

        Box(Modifier.padding(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp)) {

            PillMenu(
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
                top = overlay_page == OverlayPage.SETTINGS
            )

            val main_page_rows = remember { mutableStateListOf<YtItemRow>() }
            val feed_refresh_mutex = remember { ReentrantLock() }
            val refreshFeed: (Boolean, (success: Boolean) -> Unit) -> Unit = { allow_cached, onFinished ->
                if (!feed_refresh_mutex.isLocked) {
                    MainActivity.network.onRetry()
                    thread {
                        feed_refresh_mutex.lock()
                        try {
                            main_page_rows.clear()
                            val feed = DataApi.getRecommendedFeed(allow_cached)

                            val artist_row =
                                YtItemRow("Recommended Artists", null, YtItemRow.TYPE.LONG)
                            val playlist_row =
                                YtItemRow("Recommended playlists", null, YtItemRow.TYPE.SQUARE)

                            for (row in feed) {
                                val entry =
                                    YtItemRow(row.title, row.subtitle, YtItemRow.TYPE.SQUARE)
                                var entry_added = false

                                for (item in row.items) {
                                    item.getPreviewable().loadData(false) { loaded ->
                                        when (loaded) {
                                            is Song -> {
                                                if (!entry_added) {
                                                    entry_added = true
                                                }
                                                entry.add(loaded)
                                                artist_row.add(loaded.artist)
                                            }
                                            is Artist -> artist_row.add(loaded)
                                            is Playlist -> playlist_row.add(loaded)
                                        }
                                    }
                                }
                                main_page_rows.add(entry)
                            }

                            main_page_rows.add(artist_row)
                            main_page_rows.add(playlist_row)
                            DataApi.processYtItemLoadQueue()
                            onFinished(true)
                        } catch (e: Exception) {
                            MainActivity.network.onError(e)
                            onFinished(false)
                        }
                        feed_refresh_mutex.unlock()
                    }
                }
            }

            LaunchedEffect(Unit) {
                refreshFeed(true) {}
            }

            Crossfade(targetState = overlay_page) {
                Column(Modifier.fillMaxSize()) {
                    when (it) {
                        OverlayPage.NONE -> MainPage(main_page_rows, refreshFeed)
                        OverlayPage.SEARCH -> SearchPage { overlay_page = it }
                        OverlayPage.SETTINGS -> PrefsPage { overlay_page = it }
                    }
                }
            }
        }

        var player by remember { mutableStateOf<ExoPlayer?>(null) }
        LaunchedEffect(Unit) {
            player = PlayerServiceHost.service.player
        }

        val screen_height = getScreenHeight()
        val swipe_state = rememberSwipeableState(0)
        val swipe_anchors = mapOf(MINIMISED_NOW_PLAYING_HEIGHT to 0, screen_height to 1)

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
                anchors = swipe_anchors,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPage(_rows: List<YtItemRow>, refreshFeed: (allow_cache: Boolean, onFinished: (success: Boolean) -> Unit) -> Unit) {

    var rows: List<YtItemRow> by remember { mutableStateOf(_rows) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(_rows.isEmpty() && MainActivity.network.error == null),
        onRefresh = {
            rows = _rows.toList()
            refreshFeed(false) { success ->
                if (success) {
                    rows = _rows
                }
            }
        },
        Modifier.padding(10.dp)
    ) {
        Crossfade(targetState = MainActivity.network.error) { error ->
            if (error != null) {
                var expand by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.CloudOff, "", Modifier.size(50.dp))

                    fun refresh() {
                        if (rows.isEmpty()) {
                            refreshFeed(true) {}
                        }
                        else {
                            MainActivity.network.onRetry()
                        }
                    }

                    PillMenu(
                        if (PlayerServiceHost.service.getIntegratedServer() == null) 3 else 2,
                        { index, _ ->
                            when (index) {
                                0 -> ActionButton(Icons.Filled.Refresh) {
                                    refresh()
                                }
                                1 -> {
                                    Text(
                                        getString(R.string.generic_network_error),
                                        textAlign = TextAlign.Center,
                                        fontSize = 15.sp,
                                        color = MainActivity.theme.getOnAccent(),
                                        modifier = Modifier.clickable {
                                            expand = !expand
                                        }
                                    )
                                }
                                2 -> ActionButton(Icons.Filled.DownloadForOffline) {
                                    sendToast("Starting integrated server...")
                                    thread {
                                        runBlocking {
                                            PlayerServiceHost.service.startIntegratedServer()
                                        }
                                        refresh()
                                    }
                                }
                            }
                        },
                        null,
                        MainActivity.theme.getAccent(),
                        MainActivity.theme.getOnAccent(),
                        container_modifier = Modifier,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(expand) {
                        val msg = "Error: ${error.javaClass.simpleName}" +
                                "\n\nMessage: ${error.message}" +
                                "\n\nCause: ${error.cause}" +
                                "\n\nStack trace: ${error.stackTrace.asList()}"
                        LazyColumn(Modifier.fillMaxHeight(0.4f)) {
                            item {
                                Button({
                                    throw error
                                }) {
                                    Text("Throw error")
                                }
                            }
                            item {
                                Text(
                                    msg,
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            else {
                Crossfade(rows.isNotEmpty()) { loaded ->
                    if (loaded) {
                        CompositionLocalProvider(
                            LocalOverScrollConfiguration provides null
                        ) {
                            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
    }
}
