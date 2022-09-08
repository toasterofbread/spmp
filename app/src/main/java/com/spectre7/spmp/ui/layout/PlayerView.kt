package com.spectre7.spmp.ui.layout

import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
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
import com.google.android.exoplayer2.Tracks
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

class PlayerStatus {
    var song: Song? by mutableStateOf(null)
    var queue: List<Song> by mutableStateListOf<Song>() // TODO Update queue
    var playing: Boolean by mutableStateOf(false)
    var position: Float by mutableStateOf(0.0f)
    var shuffle: Boolean by mutableStateOf(false)
    var repeat_mode: Int by mutableStateOf(Player.REPEAT_MODE_OFF)
    var has_next: Boolean by mutableStateOf(false)
    var has_previous: Boolean by mutableStateOf(false)
}

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

            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .padding(15.dp)
            ) {
                var expand by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { expand = !expand },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, "", tint = MaterialTheme.colorScheme.onPrimary)
                }

                AnimatedVisibility(visible = expand, enter = expandHorizontally(tween(250)), exit = shrinkHorizontally(tween(250))) {
                    Row(
                        Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                        horizontalArrangement = Arrangement.End
                    ) {

                        IconButton(onClick = {
                            expand = false
                        }) {
                            Icon(Icons.Filled.Settings, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }

                        IconButton(onClick = {
                            expand = false
                            overlay_page = OverlayPage.SEARCH
                        }) {
                            Icon(Icons.Filled.Search, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }

                        IconButton(onClick = {
                            expand = false }
                        ) {
                            Icon(Icons.Filled.KeyboardArrowRight, "", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

//        if (overlay_page == OverlayPage.NONE) {
            ActionMenu()
//        }

        Column(Modifier.padding(10.dp)) {

            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                item {
//                    SongList("もう一度聴く", songs, 2)
                    SongList("Listen again", songs, 2)
                }

                item {
//                    SongList("おすすめ", songs, 2)
                    SongList("Quick picks", songs, 2)
                }

                item {
//                    SongList("お気に入り", songs, 2)
                    SongList("Recommended MVs", songs, 2)
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


        val p_status by remember { mutableStateOf(PlayerStatus()) }

        LaunchedEffect(Unit) {
            MainActivity.player.interact {
                p_status.playing = it.player.isPlaying
                p_status.position = it.player.currentPosition.toFloat() / it.player.duration.toFloat()
                p_status.song = it.player.currentMediaItem?.localConfiguration?.tag as Song?
                p_status.shuffle = it.player.shuffleModeEnabled
                p_status.repeat_mode = it.player.repeatMode
                p_status.has_next = it.player.hasNextMediaItem()
                p_status.has_previous = it.player.hasPreviousMediaItem()
                p_status.queue = it.p_queue
            }
        }

        val listener = remember {
            object : Player.Listener {

                override fun onMediaItemTransition(
                    media_item: MediaItem?,
                    reason: Int
                ) {
                    p_status.song = media_item?.localConfiguration?.tag as Song
                }

                override fun onIsPlayingChanged(is_playing: Boolean) {
                    p_status.playing = is_playing
                }

                override fun onShuffleModeEnabledChanged(shuffle_enabled: Boolean) {
                    p_status.shuffle = shuffle_enabled
                }

                override fun onRepeatModeChanged(repeat_mode: Int) {
                    p_status.repeat_mode = repeat_mode
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    p_status.has_previous = player.hasPreviousMediaItem()
                    p_status.has_next = player.hasNextMediaItem()
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
                    p_status.position = it.player.currentPosition.toFloat() / it.player.duration.toFloat()
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

        val default_background_colour = MaterialTheme.colorScheme.background
        val background_colour = remember { Animatable(default_background_colour) }

        Card(colors = CardDefaults.cardColors(
            containerColor = background_colour.value
        ), modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(screen_height.dp)
            .offset(y = (screen_height.dp / 2) - swipe_state.offset.value.dp)
            .swipeable(
                state = swipe_state,
                anchors = swipe_anchors,
                thresholds = { _, _ -> FractionalThreshold(0.2f) },
                orientation = Orientation.Vertical,
                reverseDirection = true
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, enabled = swipe_state.targetValue == 0, indication = null) { switch = !switch }, shape = RectangleShape) {

            Column(Modifier.fillMaxSize()) {
                NowPlaying(swipe_state.offset.value / screen_height, screen_height, p_status, background_colour)
            }
        }
    }
}