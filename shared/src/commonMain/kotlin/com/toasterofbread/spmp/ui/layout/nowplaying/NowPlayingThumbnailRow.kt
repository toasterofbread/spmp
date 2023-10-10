package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.getPixel
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.OVERLAY_MENU_ANIMATION_DURATION
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.MainPlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.RelatedContentPlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.NotifImagePlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PaletteSelectorPlayerOverlayMenu
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import com.toasterofbread.utils.common.getInnerSquareSizeOfCircle
import com.toasterofbread.utils.common.getValue
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.OnChangedEffect
import com.toasterofbread.utils.modifier.background
import com.toasterofbread.utils.modifier.disableParentScroll
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
    onThumbnailLoaded: (Song?, ImageBitmap?) -> Unit,
    setThemeColour: (Color?) -> Unit,
    getSeekState: () -> Float
) {
    val player = LocalPlayerState.current
    val db = player.context.database
    val expansion = LocalNowPlayingExpansion.current
    val current_song = player.status.m_song

    val song_title: String? by current_song?.observeActiveTitle()
    val song_artist_title: String? by current_song?.Artist?.observePropertyActiveTitle()

    val thumbnail_rounding: Int? = current_song?.ThumbnailRounding?.observe(db)?.value

    var overlay_menu by player.np_overlay_menu
    var current_thumb_image: ImageBitmap? by remember { mutableStateOf(null) }
    val thumbnail_shape = RoundedCornerShape(thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING)
    var image_size by remember { mutableStateOf(IntSize(1, 1)) }

    var colourpick_callback by remember { mutableStateOf<((Color?) -> Unit)?>(null) }
    LaunchedEffect(overlay_menu) {
        colourpick_callback = null
    }

    val main_overlay_menu = remember {
        MainPlayerOverlayMenu(
            { overlay_menu = it },
            { colourpick_callback = it },
            {
                setThemeColour(it)
                overlay_menu = null
            },
            { player.screen_size.width }
        )
    }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
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
            fun performPressAction(long_press: Boolean) {
                if (overlay_menu != null || expansion.get() !in 0.9f .. 1.1f) {
                    return
                }

                val custom_action: Boolean =
                    if (Settings.KEY_PLAYER_OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS.get()) !long_press
                    else long_press

                val action: PlayerOverlayMenuAction =
                    if (custom_action) Settings.KEY_PLAYER_OVERLAY_CUSTOM_ACTION.getEnum()
                    else PlayerOverlayMenuAction.DEFAULT

                when (action) {
                    PlayerOverlayMenuAction.OPEN_MAIN_MENU -> overlay_menu = main_overlay_menu
                    PlayerOverlayMenuAction.OPEN_THEMING -> {
                        overlay_menu = PaletteSelectorPlayerOverlayMenu(
                            { colourpick_callback = it },
                            {
                                setThemeColour(it)
                                overlay_menu = null
                            }
                        )
                    }
                    PlayerOverlayMenuAction.PICK_THEME_COLOUR -> {
                        colourpick_callback = { colour ->
                            if (colour != null) {
                                setThemeColour(colour)
                                overlay_menu = null
                                colourpick_callback = null
                            }
                        }
                    }
                    PlayerOverlayMenuAction.ADJUST_NOTIFICATION_IMAGE_OFFSET -> {
                        overlay_menu = NotifImagePlayerOverlayMenu()
                    }
                    PlayerOverlayMenuAction.OPEN_LYRICS -> {
                        overlay_menu = PlayerOverlayMenu.getLyricsMenu()
                    }
                    PlayerOverlayMenuAction.OPEN_RELATED -> {
                        val related_endpoint = player.context.ytapi.SongRelatedContent
                        if (related_endpoint.isImplemented()) {
                            overlay_menu = RelatedContentPlayerOverlayMenu(related_endpoint)
                        }
                        else {
                            throw EndpointNotImplementedException(related_endpoint)
                        }
                    }
                    PlayerOverlayMenuAction.DOWNLOAD -> {
                        current_song?.also { song ->
                            player.context.download_manager.startDownload(song.id)
                        }
                    }
                }
            }

            Crossfade(current_song, animationSpec = tween(250)) { song ->
                song?.Thumbnail(
                    MediaItemThumbnailProvider.Quality.HIGH,
                    getContentColour = { player.getNPOnBackground() },
                    onLoaded = {
                        current_thumb_image = it
                        onThumbnailLoaded(song, it)
                    },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(thumbnail_shape)
                        .onSizeChanged {
                            image_size = it
                        }
                        .platformClickable(
                            onClick = {
                                performPressAction(false)
                            },
                            onAltClick = {
                                performPressAction(true)
                            }
                        )
                )
            }

            // Thumbnail overlay menu
            androidx.compose.animation.AnimatedVisibility(
                overlay_menu != null || colourpick_callback != null,
                Modifier.fillMaxSize(),
                enter = fadeIn(tween(OVERLAY_MENU_ANIMATION_DURATION)),
                exit = fadeOut(tween(OVERLAY_MENU_ANIMATION_DURATION))
            ) {
                val overlay_background_alpha by animateFloatAsState(if (colourpick_callback != null) 0.4f else 0.8f)

                Box(
                    Modifier
                        .disableParentScroll(child_does_not_scroll = true)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    colourpick_callback?.also { callback ->
                                        current_thumb_image?.also { image ->
                                            handleColourPick(image, image_size, offset, callback)
                                            return@detectTapGestures
                                        }
                                    }

                                    if (expansion.get() in 0.9f .. 1.1f && overlay_menu?.closeOnTap() == true) {
                                        overlay_menu = null
                                    }
                                }
                            )
                        }
                        .graphicsLayer { alpha = expansion.getAbsolute() }
                        .fillMaxSize()
                        .background(
                            thumbnail_shape,
                            { Color.DarkGray.setAlpha(overlay_background_alpha) }
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
                        BackHandler(overlay_menu != null) {
                            if (overlay_menu == main_overlay_menu) {
                                overlay_menu = null
                            }
                            else {
                                overlay_menu = main_overlay_menu
                            }
                            colourpick_callback = null
                        }

                        Crossfade(overlay_menu) { menu ->
                            CompositionLocalProvider(LocalContentColor provides Color.White) {
                                menu?.Menu(
                                    { player.status.m_song!! },
                                    { expansion.getAbsolute() },
                                    { overlay_menu = it ?: main_overlay_menu },
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
                    song_title ?: "",
                    maxLines = 1,
                    color = player.getNPOnBackground(),
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song_artist_title ?: "",
                    maxLines = 1,
                    color = player.getNPOnBackground(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val player_button_modifier = Modifier.size(40.dp)

            val show_prev_button: Boolean by Settings.KEY_MINI_PLAYER_SHOW_PREV_BUTTON.rememberMutableState()

            if (show_prev_button) {
                IconButton({ player.controller?.seekToPrevious() }, player_button_modifier) {
                    Image(
                        Icons.Rounded.SkipPrevious,
                        null,
                        colorFilter = ColorFilter.tint(player.getNPOnBackground())
                    )
                }
            }

            IconButton({ player.controller?.playPause() }, player_button_modifier) {
                Image(
                    if (player.status.m_playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    getString(if (player.status.m_playing) "media_pause" else "media_play"),
                    colorFilter = ColorFilter.tint(player.getNPOnBackground())
                )
            }

            IconButton({ player.controller?.seekToNext() }, player_button_modifier) {
                Image(
                    Icons.Rounded.SkipNext,
                    null,
                    colorFilter = ColorFilter.tint(player.getNPOnBackground())
                )
            }
        }
    }
}
