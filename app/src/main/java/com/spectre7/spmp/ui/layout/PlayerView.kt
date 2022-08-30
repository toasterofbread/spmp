package com.spectre7.spmp.ui.layout

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.components.NowPlaying
import com.spectre7.spmp.ui.components.NowPlayingMinimised
import kotlinx.coroutines.delay

@Composable
fun HomePage() {

    Column(modifier = Modifier.fillMaxSize()) {

        SearchPage()

        var p_playing by remember { mutableStateOf(false) }
        var p_position by remember { mutableStateOf(0f) }
        var p_song by remember { mutableStateOf<Song?>(null) }

        MainActivity.instance!!.player.interact {
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
            MainActivity.instance!!.player.addListener(listener)
            onDispose {
                MainActivity.instance!!.player.removeListener(listener)
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                MainActivity.instance!!.player.interact {
                    p_position = it.player.currentPosition.toFloat() / it.player.duration.toFloat()
                }
                delay(100)
            }
        }

        var expand_now_playing by remember { mutableStateOf(false) }
        val bar_height by animateDpAsState(
            targetValue = if (expand_now_playing) LocalConfiguration.current.screenHeightDp.dp else LocalConfiguration.current.screenHeightDp.dp * (1.0f - 0.92f),
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearOutSlowInEasing
            )
        )

        Button(onClick = {
            expand_now_playing = !expand_now_playing
        }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ), modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(bar_height).offset(y = (bar_height / -2) + LocalConfiguration.current.screenHeightDp.dp * (1.0f - 0.92f) * 0.5f), contentPadding = PaddingValues(0.dp), shape = RectangleShape) {

            Column(Modifier.fillMaxSize()) {

//                AnimatedVisibility(
//                    visible = expand_now_playing,
//                    enter = fadeIn(),
//                    exit = fadeOut()
//                ) {
                    if (p_song != null && expand_now_playing)
                        NowPlaying(p_song!!, p_playing, p_position)
//                }

                AnimatedVisibility(
                    visible = !expand_now_playing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NowPlayingMinimised(p_song, p_playing, p_position)
                }
            }

        }
    }
}

