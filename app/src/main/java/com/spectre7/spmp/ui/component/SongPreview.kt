package com.spectre7.spmp.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getStatusBarHeight
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongPreview (song: Song, large: Boolean, colour: Color, modifier: Modifier = Modifier, basic: Boolean = false) {

    var show_popup by remember { mutableStateOf(false) }

    var thumb_position: Offset? by remember { mutableStateOf(null) }
    var thumb_size: IntSize? by remember { mutableStateOf(null) }
    var thumb_corner_size: Int by remember { mutableStateOf(0) }

    if (show_popup && thumb_position != null && thumb_size != null) {

        val density = LocalDensity.current
        val status_bar_height = getStatusBarHeight(MainActivity.context)

        val offset = remember { with (density) { DpOffset(thumb_position!!.x.toDp(), thumb_position!!.y.toDp() - status_bar_height) } }
        val size = remember { with (density) { DpSize(thumb_size!!.width.toDp(), thumb_size!!.height.toDp()) } }

        LongPressPopup(song, offset, size, RoundedCornerShape(thumb_corner_size)) {
            show_popup = false
        }
    }

    val onLongPress = {
        show_popup = true
    }

    @Composable
    fun Thumb(size: Dp, corner_size: Int) {
        thumb_corner_size = corner_size
        Image(
            painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner_size))
                .onGloballyPositioned {
                    thumb_position = it.positionInWindow()
                }
                .onSizeChanged {
                    thumb_size = it
                }
        )
    }

    if (large) {
        Column(
            modifier
                .padding(10.dp, 0.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { PlayerServiceHost.service.playSong(song) },
                    onLongClick = onLongPress
                )
                .aspectRatio(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Thumb(100.dp, 10)

            Text(
                song.title,
                fontSize = 12.sp,
                color = colour,
                maxLines = 1,
                lineHeight = 14.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(10.dp, 0.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onLongPress
                )
        ) {

            Thumb(40.dp, 20)

            Column(
                Modifier
                    .padding(10.dp)
                    .fillMaxWidth(0.9f)) {
                Text(
                    song.title,
                    fontSize = 15.sp,
                    color = colour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    song.artist.name,
                    fontSize = 11.sp,
                    color = colour.setAlpha(0.5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!basic) {
                IconButton(onClick = {
                    PlayerServiceHost.service.addToQueue(song) {
                        PlayerServiceHost.service.play()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier, colour)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LongPressPopup(
    song: Song,
    initial_pos: DpOffset,
    initial_size: DpSize,
    clip: Shape,
    close: () -> Unit
) {

    val density = LocalDensity.current
    var fully_open by remember { mutableStateOf(false) }
    val anim_duration = 100

    val pos = remember { Animatable(initial_pos, DpOffset.VectorConverter) }
    val width = remember { Animatable(initial_size.width.value) }
    val height = remember { Animatable(initial_size.height.value) }
    val panel_alpha = remember { Animatable(1f) }

    var target_position: Offset? by remember { mutableStateOf(null) }
    var target_size: IntSize? by remember { mutableStateOf(null) }

    suspend fun animateToTarget() {
        coroutineScope {
            launch {
                panel_alpha.animateTo(1f, tween(anim_duration/2))
            }
            with (density) {
                val pos_job = launch {
                    pos.animateTo(DpOffset(target_position!!.x.toDp(), target_position!!.y.toDp()), tween(anim_duration))
                }
                val width_job = launch {
                    width.animateTo(target_size!!.width.toDp().value, tween(anim_duration))
                }
                val height_job = launch {
                    height.animateTo(target_size!!.height.toDp().value, tween(anim_duration))
                }

                pos_job.join()
                width_job.join()
                height_job.join()

                fully_open = true
            }
        }
    }

    LaunchedEffect(Unit) {
//        delay(100)
        animateToTarget()
    }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {

            Box(Modifier.padding(horizontal = 50.dp).align(Alignment.Center)) {
                Column(
                    Modifier
                        .alpha(panel_alpha.value)
                        .background(MainActivity.theme.getBackground(false), clip)
                        .border(Dp.Hairline, MainActivity.theme.getAccent(), clip)
                        .fillMaxWidth()
                        .padding(15.dp)
                ) {

                    Row(
                        Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .clip(clip)
                                .alpha(if (fully_open) 1f else 0f)
                                .aspectRatio(1f)
                                .onSizeChanged {
                                    target_size = it
                                }
                                .onGloballyPositioned {
                                    target_position = it.localPositionOf(
                                        it.parentCoordinates!!.parentCoordinates!!,
                                        it.positionInRoot()
                                    )
                                }
                        )

                        Column(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(horizontal = 15.dp)
                            , verticalArrangement = Arrangement.Center) {
                            Text(song.title)

                            ArtistPreview(
                                song.artist,
                                false,
                                MainActivity.theme.getOnBackground(false),
                                icon_size = 30.dp, font_size = 12.sp
                            )
                        }
                    }
                }
            }

            if (!fully_open) {
                Box(
                    Modifier
                        .offset(pos.value.x, pos.value.y)
                        .requiredSize(width.value.dp, height.value.dp)
                        .clip(clip)
                        .background(Color.Green.setAlpha(0.3))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}