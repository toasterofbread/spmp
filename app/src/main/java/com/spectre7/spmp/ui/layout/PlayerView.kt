package com.spectre7.spmp.ui.layout

import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.components.NowPlaying
import kotlinx.coroutines.delay
import kotlin.concurrent.thread

fun convertPixelsToDp(px: Int): Float {
    return px.toFloat() / (MainActivity.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT);
}

fun getStatusBarHeight(): Float {
    var ret = 0;
    val resourceId: Int = MainActivity.resources.getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
        ret = MainActivity.resources.getDimensionPixelSize(resourceId);
    }
    return convertPixelsToDp(ret)
}

const val MINIMISED_NOW_PLAYING_HEIGHT = 64f
enum class OverlayPage {NONE, SEARCH}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerView() {

    var overlay_page by remember { mutableStateOf(OverlayPage.NONE) }

    @Composable
    fun MainPage() {

        @Composable
        fun SongList(label: String, songs: SnapshotStateList<Song>, rows: Int = 2) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(label, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))

                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier.requiredHeight(140.dp * rows)
                    ) {
                        items(songs.size) {
                            Box(modifier = Modifier.requiredWidth(125.dp)) {
                                songs[it].Preview(true)
                            }
                        }
                    }
                }
            }
        }

        val songs = remember { mutableStateListOf<Song>() }

        LaunchedEffect(Unit) {
            thread {
                for (result in DataApi.search("", ResourceType.SONG)) {
                    songs.add(Song.fromId(result.id.videoId))
                    Log.d("", result.id.videoId)
                }
            }
        }

        @Composable
        fun ActionMenu() {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)) {

                var expand by remember { mutableStateOf(false) }

                Column(
                    Modifier
                        .animateContentSize()
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    IconButton(
                        onClick = { expand = !expand },
                    ) {
                        Crossfade(targetState = expand) {
                            Icon(if (it) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    if (expand) {
                        IconButton(onClick = {
                            expand = false
                            overlay_page = OverlayPage.SEARCH
                        }) {
                            Icon(Icons.Filled.Search, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }

                        IconButton(onClick = {
                            expand = false
                        }) {
                            Icon(Icons.Filled.Settings, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                }
            }
        }

        if (overlay_page == OverlayPage.NONE) {
            ActionMenu()
        }

        Column(Modifier.padding(10.dp)) {

            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                item {
                    SongList("もう一度聴く", songs, 2)
                }

                item {
                    SongList("おすすめ", songs, 2)
                }

                item {
                    SongList("お気に入り", songs, 2)
                }

            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(top = getStatusBarHeight().dp)) {

        Box(Modifier.padding(bottom = MINIMISED_NOW_PLAYING_HEIGHT.dp)) {
            MainPage()

            Crossfade(targetState = overlay_page) {
                Column(Modifier.fillMaxSize()) {
                    when (it) {
                        OverlayPage.NONE -> {}
                        OverlayPage.SEARCH -> SearchPage { overlay_page = it }
                    }
                }
            }
        }

        var p_playing by remember { mutableStateOf(false) }
        var p_position by remember { mutableStateOf(0f) }
        var p_song by remember { mutableStateOf<Song?>(null) }

        val col = MaterialTheme.colorScheme.secondaryContainer
        val theme_colour = remember { Animatable(col) }

        MainActivity.player.interact {
            p_playing = it.player.isPlaying
            p_position = it.player.currentPosition.toFloat() / it.player.duration.toFloat()
            p_song = it.player.currentMediaItem?.localConfiguration?.tag as Song?
        }

        val listener = remember {
            object : Player.Listener {
                override fun onMediaItemTransition(
                    media_item: MediaItem?,
                    reason: Int
                ) {
                    p_song = media_item?.localConfiguration?.tag as Song
                }

                override fun onIsPlayingChanged(is_playing: Boolean) {
                    p_playing = is_playing
                }
            }
        }

        DisposableEffect(Unit) {
            MainActivity.player.addListener(listener)
            onDispose {
                MainActivity.player.removeListener(listener)
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                MainActivity.player.interact {
                    p_position = it.player.currentPosition.toFloat() / it.player.duration.toFloat()
                }
                delay(100)
            }
        }

        val screen_height = LocalConfiguration.current.screenHeightDp.toFloat() + getStatusBarHeight()
        val swipe_state = rememberSwipeableState(1)
        val swipe_anchors = mapOf(MINIMISED_NOW_PLAYING_HEIGHT to 0, screen_height to 1)

        var switch by remember { mutableStateOf(false) }
        LaunchedEffect(switch) {
            swipe_state.animateTo(if (swipe_state.currentValue == 0) 1 else 0)
        }

        CompositionLocalProvider(LocalRippleTheme provides object : RippleTheme {
            @Composable
            override fun defaultColor(): Color = Color.Unspecified

            @Composable
            override fun rippleAlpha(): RippleAlpha = RippleAlpha(
                draggedAlpha = 0f,
                focusedAlpha = 0f,
                hoveredAlpha = 0f,
                pressedAlpha = 0f,
            )
        }) {
            Card(colors = CardDefaults.cardColors(
                containerColor = theme_colour.value,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ), modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(swipe_state.offset.value.dp)
                .offset(y = swipe_state.offset.value.dp / -2)
                .swipeable(
                    state = swipe_state,
                    anchors = swipe_anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.2f) },
                    orientation = Orientation.Vertical,
                    reverseDirection = true
                )
                .clickable { switch = !switch }, shape = RectangleShape) {

                CompositionLocalProvider(LocalRippleTheme provides object : RippleTheme {
                    @Composable override fun defaultColor(): Color = MaterialTheme.colorScheme.onSecondaryContainer
                    @Composable
                    override fun rippleAlpha(): RippleAlpha = RippleAlpha(
                        draggedAlpha = 0.25f,
                        focusedAlpha = 0.25f,
                        hoveredAlpha = 0.25f,
                        pressedAlpha = 0.25f,
                    )
                }) {
                    Column(Modifier.fillMaxSize()) {
                        NowPlaying(swipe_state.targetValue == 1, p_song, p_playing, p_position, theme_colour)
                    }
                }
            }
        }
    }
}