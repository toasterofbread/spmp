package com.spectre7.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.composable.BackHandler
import com.spectre7.spmp.platform.getPixel
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.spectre7.spmp.ui.layout.nowplaying.overlay.MainOverlayMenu
import com.spectre7.spmp.ui.layout.nowplaying.overlay.OverlayMenu
import com.spectre7.utils.getInnerSquareSizeOfCircle
import com.spectre7.utils.setAlpha
import kotlin.math.min

@Composable
fun ThumbnailRow(
    modifier: Modifier,
    onThumbnailLoaded: (Song?) -> Unit,
    setThemeColour: (Color?) -> Unit,
    getSeekState: () -> Float
) {
    val player = LocalPlayerState.current
    val current_song = player.status.m_song
    val expansion = LocalNowPlayingExpansion.current

    var current_thumb_image: ImageBitmap? by remember { mutableStateOf(null) }
    val thumbnail_rounding: Int? = current_song?.song_reg_entry?.thumbnail_rounding
    val thumbnail_shape = RoundedCornerShape(thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING)
    var image_size by remember { mutableStateOf(IntSize(1, 1)) }
    val disappear_scale = minOf(1f, if (expansion.getAbsolute() < 0.5f) 1f else (1f - ((expansion.getAbsolute() - 0.5f) * 2f)))

    LaunchedEffect(current_song) {
        current_thumb_image = null
    }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        var overlay_menu by remember { mutableStateOf<OverlayMenu?>(null) }
        var colourpick_callback by remember { mutableStateOf<((Color?) -> Unit)?>(null) }

        LaunchedEffect(expansion.getAbsolute() > 0f) {
            overlay_menu = null
        }

        var get_shutter_menu by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        var shutter_menu_open by remember { mutableStateOf(false) }
        LaunchedEffect(expansion.getAbsolute() >= EXPANDED_THRESHOLD) {
            shutter_menu_open = false
            overlay_menu = null
        }

        // Keep thumbnail centered
        Spacer(Modifier)

        Box(Modifier.aspectRatio(1f)) {

            Crossfade(current_song, animationSpec = tween(250)) { song ->
                song?.Thumbnail(
                    MediaItemThumbnailProvider.Quality.HIGH,
                    contentColourProvider = { getNPOnBackground() },
                    onLoaded = {
                        current_thumb_image = it
                        onThumbnailLoaded(song)
                    },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(thumbnail_shape)
                        .onSizeChanged {
                            image_size = it
                        }
                        .run {
                            if (colourpick_callback == null) {
                                if (overlay_menu == null || overlay_menu!!.closeOnTap()) {
                                    this.clickable(
                                        enabled = expansion.getAbsolute() == 1f,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        overlay_menu = if (overlay_menu == null) MainOverlayMenu(
                                            { overlay_menu = it },
                                            { colourpick_callback = it },
                                            {
                                                setThemeColour(it)
                                                overlay_menu = null
                                            },
                                            { SpMp.context.getScreenWidth() }
                                        ) else null
                                    }
                                } else {
                                    this
                                }
                            } else {
                                this.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            if (colourpick_callback != null) {
                                                current_thumb_image?.apply {
                                                    val bitmap_size = min(width, height)
                                                    var x = (offset.x / image_size.width) * bitmap_size
                                                    var y = (offset.y / image_size.height) * bitmap_size

                                                    if (width > height) {
                                                        x += (width - height) / 2
                                                    } else if (height > width) {
                                                        y += (height - width) / 2
                                                    }

                                                    colourpick_callback?.invoke(
                                                        getPixel(x.toInt(), y.toInt())
                                                    )
                                                    colourpick_callback = null
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                )
            }

            // Thumbnail overlay menu
            androidx.compose.animation.AnimatedVisibility(
                overlay_menu != null,
                enter = fadeIn(tween(OVERLAY_MENU_ANIMATION_DURATION)),
                exit = fadeOut(tween(OVERLAY_MENU_ANIMATION_DURATION))
            ) {
                Box(
                    Modifier
                        .alpha(expansion.getAbsolute())
                        .fillMaxSize()
                        .background(
                            Color.DarkGray.setAlpha(0.85f),
                            shape = thumbnail_shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(with(LocalDensity.current) {
                                getInnerSquareSizeOfCircle(
                                    radius = image_size.height.toDp().value,
                                    corner_percent = thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING
                                ).dp
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(overlay_menu) { menu ->
                            if (menu != null) {
                                BackHandler {
                                    overlay_menu = null
                                    colourpick_callback = null
                                }
                            }

                            menu?.Menu(
                                { player.status.m_song!! },
                                expansion.getAbsolute(),
                                {
                                    get_shutter_menu = it
                                    shutter_menu_open = true
                                },
                                {
                                    overlay_menu = null
                                },
                                getSeekState,
                                { current_thumb_image }
                            )
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    shutter_menu_open,
                    enter = expandVertically(tween(200)),
                    exit = shrinkVertically(tween(200))
                ) {
                    val padding = 15.dp
                    CompositionLocalProvider(
                        LocalContentColor provides getNPOnBackground()
                    ) {
                        Column(
                            Modifier
                                .background(
                                    getNPBackground().setAlpha(0.9f),
                                    thumbnail_shape
                                )
                                .padding(start = padding, top = padding, end = padding)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                get_shutter_menu?.invoke()
                            }
                            IconButton(onClick = { shutter_menu_open = false }) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp, null,
                                    tint = getNPOnBackground(),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth(1f - expansion.getAbsolute())
                .scale(disappear_scale, 1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.requiredWidth(10.dp))

            Column(Modifier.fillMaxSize().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                Text(
                    current_song?.title ?: "",
                    maxLines = 1,
                    color = getNPOnBackground(),
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    current_song?.artist?.title ?: "",
                    maxLines = 1,
                    color = getNPOnBackground(),//.copy(alpha = 0.5f),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val player_button_modifier = Modifier.size(40.dp)
            IconButton(player.player::playPause, player_button_modifier) {
                Image(
                    if (player.status.m_playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    getString(if (player.status.m_playing) "media_pause" else "media_play"),
                    colorFilter = ColorFilter.tint(getNPOnBackground())
                )
            }

            IconButton(player.player::seekToNext, player_button_modifier) {
                Image(
                    Icons.Filled.SkipNext,
                    null,
                    colorFilter = ColorFilter.tint(getNPOnBackground())
                )
            }
        }
    }
}
