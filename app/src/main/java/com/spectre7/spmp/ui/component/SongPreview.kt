package com.spectre7.spmp.ui.component

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.rememberAsyncImagePainter
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.utils.Marquee
import com.spectre7.utils.getStatusBarHeight
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongPreview (song: Song, large: Boolean, colour: Color, modifier: Modifier = Modifier, basic: Boolean = false) {

    var show_popup by remember { mutableStateOf(false) }
    var hide_thumb by remember { mutableStateOf(false) }

    var thumb_position: Offset? by remember { mutableStateOf(null) }
    var thumb_size: IntSize? by remember { mutableStateOf(null) }
    var thumb_corner_size: Int by remember { mutableStateOf(0) }

    if (show_popup && thumb_position != null && thumb_size != null) {
        val density = LocalDensity.current
        val status_bar_height = getStatusBarHeight(MainActivity.context)

        val offset = remember { with (density) { DpOffset(thumb_position!!.x.toDp(), thumb_position!!.y.toDp() - status_bar_height) } }
        val size = remember { with (density) { DpSize(thumb_size!!.width.toDp(), thumb_size!!.height.toDp()) } }

        LongPressPopup(song, offset, size, RoundedCornerShape(thumb_corner_size), { hide_thumb = true }) {
            show_popup = false
            hide_thumb = false
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
                .alpha(if (hide_thumb) 0f else 1f)
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
    onShown: () -> Unit,
    close: () -> Unit
) {

    val density = LocalDensity.current
    val anim_duration = 200
    var fully_open by remember { mutableStateOf(false) }

    val pos = remember { Animatable(initial_pos, DpOffset.VectorConverter) }
    val width = remember { Animatable(initial_size.width.value) }
    val height = remember { Animatable(initial_size.height.value) }
    val panel_alpha = remember { Animatable(1f) }

    var target_position: Offset? by remember { mutableStateOf(null) }
    var target_size: IntSize? by remember { mutableStateOf(null) }

    suspend fun animateValues(to_target: Boolean) {

        val pos_target: DpOffset
        val width_target: Float
        val height_target: Float

        if (to_target) {
            with (density) {
                pos_target = DpOffset(target_position!!.x.toDp(), target_position!!.y.toDp())
                width_target = target_size!!.width.toDp().value
                height_target = target_size!!.height.toDp().value
            }
        }
        else {
            pos_target = initial_pos
            width_target = initial_size.width.value
            height_target = initial_size.height.value
        }

        if (!to_target) {
            fully_open = false
        }

        coroutineScope {
            launch {
                panel_alpha.animateTo(if (to_target) 1f else 0f, tween(anim_duration))
            }

            val pos_job = launch {
                pos.animateTo(pos_target, tween(anim_duration))
            }
            val width_job = launch {
                width.animateTo(width_target, tween(anim_duration))
            }
            val height_job = launch {
                height.animateTo(height_target, tween(anim_duration))
            }

            pos_job.join()
            width_job.join()
            height_job.join()

            fully_open = to_target
        }
    }

    LaunchedEffect(Unit) {
        animateValues(true)
    }

    suspend fun closePopup() {
        animateValues(false)
        close()
    }

    var close_requested by remember { mutableStateOf(false) }
    LaunchedEffect(close_requested) {
        if (close_requested) {
            closePopup()
        }
    }

    Dialog(
        onDismissRequest = { close_requested = true },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        val dialog = LocalView.current.parent as DialogWindowProvider
        dialog.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        Box(Modifier.fillMaxSize().background(Color.Black.setAlpha(panel_alpha.value / 2.0))) {

            val shape = RoundedCornerShape(topStartPercent = 12, topEndPercent = 12)
            Column(
                Modifier
                    .alpha(panel_alpha.value)
                    .background(MainActivity.theme.getBackground(false), shape)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(25.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    Modifier
                        .height(80.dp)
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

                        Marquee(false) {
                            Text(song.title, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                        }

                        Marquee(false) {
                            ArtistPreview(
                                song.artist,
                                false,
                                MainActivity.theme.getOnBackground(false),
                                Modifier.fillMaxWidth(),
                                icon_size = 30.dp, font_size = 15.sp
                            )
                        }
                    }
                }

                Divider(thickness = Dp.Hairline)

                @Composable
                fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
                    Row(Modifier.clickable(onClick = onClick), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Icon(icon, null)
                        Text(label, fontSize = 15.sp)
                    }
                }

                ActionButton(Icons.Filled.PlayArrow, "Play") {
                    PlayerServiceHost.service.playSong(song)
                }

                ActionButton(Icons.Filled.ArrowRightAlt, "Play next") { }

                val queue_song = PlayerServiceHost.service.getSong(PlayerServiceHost.service.active_queue_index)
                if (queue_song != null) {
                    Row() {
                        ActionButton(Icons.Filled.SubdirectoryArrowRight, "Play after ${queue_song.title}") {
                            PlayerServiceHost.service.addToQueue(song, PlayerServiceHost.service.active_queue_index + 1, true)
                        }
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                            val button_padding = PaddingValues(0.dp)
                            val button_modifier = Modifier.size(30.dp)

                            Button(
                                {
                                    PlayerServiceHost.service.updateActiveQueueIndex(-1)
                                },
                                button_modifier,
                                contentPadding = button_padding
                            ) {
                                Text("-")
                            }
                            Button(
                                {
                                    PlayerServiceHost.service.updateActiveQueueIndex(1)
                                },
                                button_modifier,
                                contentPadding = button_padding
                            ) {
                                Text("+")
                            }
                        }
                    }
                }

                val share_intent = remember(song.url) {
                    Intent.createChooser(Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TITLE, song.title)
                        putExtra(Intent.EXTRA_TEXT, song.url)
                        type = "text/plain"
                    }, null)
                }

                val open_intent: Intent? = remember(song.url) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(song.url))
                    if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
                        null
                    }
                    else {
                        intent
                    }
                }

                ActionButton(Icons.Filled.Download, "Download") { } // TODO
                ActionButton(Icons.Filled.Share, "Share") {
                    MainActivity.context.startActivity(share_intent)
                }

                if (open_intent != null) {
                    ActionButton(Icons.Filled.OpenWith, "Open") {
                        MainActivity.context.startActivity(open_intent)
                    }
                }
            }

            if (!fully_open) {
                Box(
                    Modifier
                        .offset(pos.value.x, pos.value.y)
                        .requiredSize(width.value.dp, height.value.dp)
                        .clip(clip)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(song.getThumbUrl(false)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                    onShown()
                }
            }
        }
    }
}