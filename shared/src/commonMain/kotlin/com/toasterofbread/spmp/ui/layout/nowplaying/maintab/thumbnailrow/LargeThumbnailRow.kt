package com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.getInnerSquareSizeOfCircle
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.MeasureUnconstrainedView
import com.toasterofbread.composekit.utils.composable.OnChangedEffect
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.composekit.utils.modifier.disableParentScroll
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.PlayerSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.EXPANDED_THRESHOLD
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.OVERLAY_MENU_ANIMATION_DURATION
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.MainPlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.NotifImagePlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PaletteSelectorPlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenuAction
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.RelatedContentPlayerOverlayMenu
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import kotlin.math.absoluteValue

@Composable
fun LargeThumbnailRow(
    modifier: Modifier = Modifier,
    onThumbnailLoaded: (Song?, ImageBitmap?) -> Unit,
    setThemeColour: (Color?) -> Unit,
    getSeekState: () -> Float,
    disable_parent_scroll_while_menu_open: Boolean = true,
    overlayContent: (@Composable () -> Unit)? = null
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current
    val current_song: Song? = player.status.m_song

    val song_title: String? by current_song?.observeActiveTitle()
    val song_artist_title: String? by current_song?.Artist?.observePropertyActiveTitle()

    val thumbnail_rounding: Int? = current_song?.ThumbnailRounding?.observe(player.context.database)?.value

    var overlay_menu: PlayerOverlayMenu? by player.np_overlay_menu
    var current_thumb_image: ImageBitmap? by remember { mutableStateOf(null) }
    val thumbnail_shape: RoundedCornerShape = RoundedCornerShape(thumbnail_rounding ?: DEFAULT_THUMBNAIL_ROUNDING)
    var image_size: IntSize by remember { mutableStateOf(IntSize(1, 1)) }

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
        modifier.clip(thumbnail_shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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

        val scale_modifier: Modifier =
            Modifier.scale(
//                minOf(1f, if (expansion.getAbsolute() < 0.5f) 1f else (1f - ((expansion.getAbsolute() - 0.5f) * 2f))),
                minOf(1f, 1f - expansion.getAbsolute()),
                1f
            )

        @Composable
        fun ButtonRow(modifier: Modifier = Modifier) {
            val button_modifier: Modifier = Modifier.size(40.dp)
            val button_image_modifier: Modifier = button_modifier
            Row(
                modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ThumbnailRowControlButtons(button_modifier, button_image_modifier, rounded_icons = false)
            }
        }

        var button_row_width: Dp by remember { mutableStateOf(0.dp) }

        MeasureUnconstrainedView({ ButtonRow() }) { width, _ ->
            button_row_width = with (density) { width.toDp() }
            ButtonRow(Modifier.width(button_row_width * (1f - expansion.getBounded())))
        }

        Spacer(Modifier.fillMaxWidth().weight(1f))

        Box(Modifier.aspectRatio(1f)) {
            fun performPressAction(long_press: Boolean) {
                if (overlay_menu != null || expansion.get() !in 0.9f .. 1.1f) {
                    return
                }

                val custom_action: Boolean =
                    if (PlayerSettings.Key.OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS.get()) !long_press
                    else long_press

                val action: PlayerOverlayMenuAction =
                    if (custom_action) PlayerSettings.Key.OVERLAY_CUSTOM_ACTION.getEnum()
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
                            player.context.download_manager.startDownload(song)
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
                        .thenIf(expanded) {
                            platformClickable(
                                onClick = {
                                    performPressAction(false)
                                },
                                onAltClick = {
                                    performPressAction(true)
                                }
                            )
                        }
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
                        .thenIf(disable_parent_scroll_while_menu_open) {
                            disableParentScroll(child_does_not_scroll = true)
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    colourpick_callback?.also { callback ->
                                        current_thumb_image?.also { image ->
                                            handleThumbnailColourPick(image, image_size, offset, callback)
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
                            { Color.DarkGray.copy(alpha = overlay_background_alpha) }
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

            overlayContent?.invoke()
        }

        Row(
            scale_modifier.padding(start = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier, verticalArrangement = Arrangement.SpaceEvenly) {
                Text(
                    song_title ?: "",
                    maxLines = 1,
                    color = player.getNPOnBackground(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    song_artist_title ?: "",
                    maxLines = 1,
                    color = player.getNPOnBackground().copy(alpha = 0.5f),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.fillMaxWidth().weight(1f))

        Spacer(Modifier.width(button_row_width))
    }
}
