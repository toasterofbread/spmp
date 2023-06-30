package com.spectre7.spmp.ui.layout.nowplaying

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.spectre7.utils.composable.OnChangedEffect
import com.spectre7.utils.getInnerSquareSizeOfCircle
import com.spectre7.utils.setAlpha
import kotlin.math.absoluteValue
import kotlin.math.min

private fun handleColourPick(image: ImageBitmap, image_size: IntSize, tap_offset: Offset, onPicked: (Color) -> Unit) {
    val bitmap_size = min(image.width, image.height)
    var x = (tap_offset.x / image_size.width) * bitmap_size
    var y = (tap_offset.y / image_size.height) * bitmap_size

    if (image.width > image.height) {
        x += (image.width - image.height) / 2
    } else if (image.height > image.width) {
        y += (image.height - image.width) / 2
    }

    onPicked(image.getPixel(x.toInt(), y.toInt()))
}

@Composable
fun ThumbnailRow(
    modifier: Modifier = Modifier,
    onThumbnailLoaded: (Song?) -> Unit,
    setThemeColour: (Color?) -> Unit,
    getSeekState: () -> Float
) {
    val player = LocalPlayerState.current
    val expansion = LocalNowPlayingExpansion.current
    val current_song = player.status.m_song

    var overlay_menu by player.np_overlay_menu
    var current_thumb_image: ImageBitmap? by remember { mutableStateOf(null) }
    val thumbnail_rounding: Int? = current_song?.song_reg_entry?.thumbnail_rounding
    val thumbnail_shape = RoundedCornerShape(thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING)
    var image_size by remember { mutableStateOf(IntSize(1, 1)) }

    LaunchedEffect(current_song) {
        current_thumb_image = null
        onThumbnailLoaded(null)
    }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        var colourpick_callback by remember { mutableStateOf<((Color?) -> Unit)?>(null) }
        LaunchedEffect(overlay_menu) {
            colourpick_callback = null
        }

        var opened by remember { mutableStateOf(false) }
        val expanded by remember { derivedStateOf {
            (expansion.get() - 1f).absoluteValue <= EXPANDED_THRESHOLD
        } }

        OnChangedEffect(expanded) {
            if (expanded) {
                opened = true
            }
            else if (opened) {
                overlay_menu = null
            }
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
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    colourpick_callback?.also { callback -> 
                                    current_thumb_image?.also { image ->
                                        handleColourPick(image, image_size, offset, callback)
                                        return@detectTapGestures
                                    }}

                                    if (expansion.get() in 0.9f .. 1.1f) {
                                        overlay_menu =
                                            if (overlay_menu == null)
                                                MainOverlayMenu(
                                                    { overlay_menu = it },
                                                    { colourpick_callback = it },
                                                    {
                                                        setThemeColour(it)
                                                        overlay_menu = null
                                                    },
                                                    { SpMp.context.getScreenWidth() }
                                                )
                                            else null
                                    }
                                }
                            )
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
                        .graphicsLayer { alpha = expansion.getAbsolute() }
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

                            CompositionLocalProvider(LocalContentColor provides Color.White) {
                                menu?.Menu(
                                    { player.status.m_song!! },
                                    { expansion.getAbsolute() },
                                    { overlay_menu = it },
                                    getSeekState
                                ) { current_thumb_image }
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth(1f - expansion.getAbsolute())
                .scale(
                    minOf(1f, if (expansion.getAbsolute() < 0.5f) 1f else (1f - ((expansion.getAbsolute() - 0.5f) * 2f))),
                    1f
                ),
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
                    color = getNPOnBackground(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val player_button_modifier = Modifier.size(40.dp)
            IconButton({ player.player?.playPause() }, player_button_modifier) {
                Image(
                    if (player.status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    getString(if (player.status.m_playing) "media_pause" else "media_play"),
                    colorFilter = ColorFilter.tint(getNPOnBackground())
                )
            }

            IconButton({ player.player?.seekToNext() }, player_button_modifier) {
                Image(
                    Icons.Rounded.SkipNext,
                    null,
                    colorFilter = ColorFilter.tint(getNPOnBackground())
                )
            }
        }
    }
}
