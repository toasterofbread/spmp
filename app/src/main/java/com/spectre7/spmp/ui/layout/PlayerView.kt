package com.spectre7.spmp.ui.layout

import android.util.Log
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.components.NowPlaying
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomePage() {

    Column(modifier = Modifier.fillMaxSize()) {

        SearchPage()

        var p_playing by remember { mutableStateOf(false) }
        var p_position by remember { mutableStateOf(0f) }
        var p_song by remember { mutableStateOf<Song?>(null) }


        val col = MaterialTheme.colorScheme.secondaryContainer
        val theme_colour = remember { Animatable(col) }

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

        val swipe_state = rememberSwipeableState(1)
        val swipe_anchors = mapOf(LocalConfiguration.current.screenHeightDp.toFloat() * (1.0f - 0.92f) to 0, LocalConfiguration.current.screenHeightDp.toFloat() to 1)

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
                .offset(y = (swipe_state.offset.value.dp / -2) + LocalConfiguration.current.screenHeightDp.dp * (1.0f - 0.92f) * 0.5f)
                .swipeable(
                    state = swipe_state,
                    anchors = swipe_anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.2f) },
                    orientation = Orientation.Vertical,
                    reverseDirection = true
                )
                .clickable{ switch = !switch }, shape = RectangleShape) {

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
