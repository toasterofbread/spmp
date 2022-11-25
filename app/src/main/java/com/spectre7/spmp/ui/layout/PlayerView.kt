package com.spectre7.spmp.ui.layout

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.exoplayer2.ExoPlayer
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.YtItem
import com.spectre7.spmp.ui.component.AutoResizeText
import com.spectre7.spmp.ui.component.FontSizeRange
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.nowplaying.NowPlaying
import com.spectre7.utils.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

@SuppressLint("InternalInsetResource")
@Composable
fun getStatusBarHeight(): Dp {
    val resource_id: Int = MainActivity.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resource_id > 0) {
        with(LocalDensity.current) {
            return MainActivity.resources.getDimensionPixelSize(resource_id).toDp()
        }
    }
    throw RuntimeException()
}

@Composable
fun getScreenHeight(): Float {
    return LocalConfiguration.current.screenHeightDp.toFloat() + getStatusBarHeight().value
}

const val MINIMISED_NOW_PLAYING_HEIGHT = 64f
enum class OverlayPage {NONE, SEARCH, SETTINGS}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerView() {
    var overlay_page by remember { mutableStateOf(OverlayPage.NONE) }

    @Composable
    fun MainPage() {

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

        data class Row(val title: String, val subtitle: String?, val items: MutableList<Pair<YtItem, Long>> = mutableStateListOf())
        val rows = remember { mutableStateListOf<Row>() }

        @Composable
        fun itemPreview(item: YtItem, animate_visibility: Boolean) {
            Box(Modifier.requiredSize(125.dp), contentAlignment = Alignment.Center) {
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
        fun SongList(row: Row) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {

                    Column {
                        AutoResizeText(
                            text = row.title,
                            maxLines = 1,
                            fontSizeRange = FontSizeRange(
                                20.sp, 30.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )

                        if (row.subtitle != null) {
                            Text(row.subtitle, fontSize = 15.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onBackground.setAlpha(0.5))
                        }
                    }

                    val row_count = 2
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(row_count),
                        modifier = Modifier.requiredHeight(140.dp * row_count)
                    ) {
                        items(row.items.size, { row.items[it].first.getId() }) {
                            val item = row.items[it]
                            itemPreview(item.first, remember { System.currentTimeMillis() - item.second < 250})
                        }
                    }
                }
            }
        }

        val refresh_mutex = remember { ReentrantLock() }
        fun refreshFeed() {
            if (refresh_mutex.isLocked) {
                return
            }

            MainActivity.network.onRetry()

            thread {
                refresh_mutex.lock()
                try {
                    rows.clear()
                    val feed = DataApi.getRecommendedFeed()
                    for (row in feed) {
                        val entry = Row(row.title, row.subtitle)
                        for (item in row.items) {
                            item.getPreviewable().loadData(false) { loaded ->
                                if (loaded != null) {
                                    entry.items.add(Pair(loaded, System.currentTimeMillis()))
                                }
                            }
                        }
                        rows.add(entry)
                    }
                    DataApi.processYtItemLoadQueue()
                }
                catch (e: Exception) {
                    MainActivity.network.onError(e)
                }
                refresh_mutex.unlock()
            }
        }

        LaunchedEffect(Unit) {
            refreshFeed()
        }

        Column(Modifier.padding(10.dp)) {
            Crossfade(targetState = MainActivity.network.error) { error ->
                if (error != null) {
                    var expand by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.CloudOff, "", Modifier.size(50.dp))

                        fun refresh() {
                            if (rows.isEmpty()) {
                                refreshFeed()
                            }
                            else {
                                MainActivity.network.onRetry()
                            }
                        }

                        PillMenu(
                            if (PlayerHost.service.getIntegratedServerAddress() == null) 3 else 2,
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
                                                PlayerHost.service.startIntegratedServer()
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
                            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                items(rows.size) { index ->
                                    SongList(rows[index])
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

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = getStatusBarHeight())
    ) {

        Box(Modifier.padding(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp)) {
            MainPage()

            Crossfade(targetState = overlay_page) {
                Column(Modifier.fillMaxSize()) {
                    when (it) {
                        OverlayPage.NONE -> {}
                        OverlayPage.SEARCH -> SearchPage { overlay_page = it }
                        OverlayPage.SETTINGS -> PrefsPage { overlay_page = it }
                    }
                }
            }
        }

        var player by remember { mutableStateOf<ExoPlayer?>(null) }
        LaunchedEffect(Unit) {
            player = PlayerHost.service.player
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