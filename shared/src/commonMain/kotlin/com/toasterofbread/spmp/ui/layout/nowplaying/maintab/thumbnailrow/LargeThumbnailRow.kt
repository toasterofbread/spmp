package com.toasterofbread.spmp.ui.layout.nowplaying.maintab.thumbnailrow

import LocalNowPlayingExpansion
import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.getInnerSquareSizeOfCircle
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.composable.MeasureUnconstrainedView
import com.toasterofbread.composekit.utils.composable.OnChangedEffect
import com.toasterofbread.composekit.utils.modifier.background
import com.toasterofbread.composekit.utils.modifier.disableParentScroll
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.db.observePropertyActiveTitle
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.observeThumbnailRounding
import com.toasterofbread.spmp.model.settings.category.PlayerSettings
import com.toasterofbread.spmp.model.settings.getEnum
import com.toasterofbread.spmp.ui.component.HorizontalLyricsLineDisplay
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.EXPANDED_THRESHOLD
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.OVERLAY_MENU_ANIMATION_DURATION
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.*
import kotlin.math.absoluteValue

internal typealias ColourpickCallback = (Color?) -> Unit

@Composable
fun LargeThumbnailRow(
    modifier: Modifier = Modifier,
    onThumbnailLoaded: (Song?, ImageBitmap?) -> Unit,
    setThemeColour: (Color?) -> Unit,
    getSeekState: () -> Float,
    disable_parent_scroll_while_menu_open: Boolean = true,
    center_thumbnail: Boolean = false,
    thumbnail_modifier: Modifier = Modifier,
    overlayContent: (@Composable () -> Unit)? = null,
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current
    val current_song: Song? = player.status.m_song

    val song_title: String? by current_song?.observeActiveTitle()
    val song_artist_title: String? by current_song?.Artist?.observePropertyActiveTitle()

    val thumbnail_rounding: Int = current_song.observeThumbnailRounding()
    val thumbnail_shape: RoundedCornerShape = RoundedCornerShape(thumbnail_rounding)

    val overlay_menu: PlayerOverlayMenu? by player.np_overlay_menu
    var current_thumb_image: ImageBitmap? by remember { mutableStateOf(null) }
    var image_size: IntSize by remember { mutableStateOf(IntSize(1, 1)) }

    var colourpick_callback: ColourpickCallback? by remember { mutableStateOf(null) }
    LaunchedEffect(overlay_menu) {
        colourpick_callback = null
    }

    val main_overlay_menu: MainPlayerOverlayMenu = remember {
        MainPlayerOverlayMenu(
            player::openNpOverlayMenu,
            { colourpick_callback = it },
            setThemeColour,
            { player.screen_size.width }
        )
    }

    BoxWithConstraints(modifier) {
        Row(
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
                    player.openNpOverlayMenu(null)
                }
            }

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

            MeasureUnconstrainedView({ ButtonRow() }) { size ->
                button_row_width = with (density) { size.width.toDp() }
                ButtonRow(Modifier.width(button_row_width * (1f - expansion.getBounded())))
            }

            Spacer(Modifier.fillMaxWidth().weight(1f))

            val getTextScale: () -> Float = { minOf(1f, 1f - expansion.getAbsolute()) }
            var thumb_width: Dp by remember { mutableStateOf(0.dp) }

            Box(
                thumbnail_modifier
                    .aspectRatio(1f)
                    .onSizeChanged {
                        thumb_width = with (density) {
                            it.width.toDp()
                        }
                    }
                    .offset {
                        if (!center_thumbnail) {
                            return@offset IntOffset(0, 0)
                        }

                        return@offset IntOffset(
                            ((this@BoxWithConstraints.maxWidth - thumb_width) * (1f - getTextScale()) / 2).roundToPx(),
                            0
                        )
                    }
            ) {
                Crossfade(current_song, animationSpec = tween(250)) { song ->
                    if (song == null) {
                        return@Crossfade
                    }

                    song.Thumbnail(
                        ThumbnailProvider.Quality.HIGH,
                        getContentColour = { player.getNPOnBackground() },
                        onLoaded = {
                            current_thumb_image = it
                            onThumbnailLoaded(song, it)
                        },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .onSizeChanged {
                                image_size = it
                            }
                            .thenIf(current_thumb_image != null) {
                                songThumbnailShadow(song, thumbnail_shape)
                            }
                            .thenIf(expanded) {
                                platformClickable(
                                    onClick = {
                                        if (overlay_menu != null || expansion.get() !in 0.9f .. 1.1f) {
                                            return@platformClickable
                                        }

                                        player.performPressAction(
                                            false,
                                            main_overlay_menu,
                                            setThemeColour,
                                            { colourpick_callback = it },
                                            { player.openNpOverlayMenu(it) }
                                        )
                                    },
                                    onAltClick = {
                                        if (overlay_menu != null || expansion.get() !in 0.9f .. 1.1f) {
                                            return@platformClickable
                                        }
                                        player.performPressAction(
                                            true,
                                            main_overlay_menu,
                                            setThemeColour,
                                            { colourpick_callback = it },
                                            { player.openNpOverlayMenu(it) }
                                        )
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
                    val overlay_background_alpha: Float by animateFloatAsState(if (colourpick_callback != null) 0.4f else 0.8f)

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
                                                colourpick_callback = null
                                                return@detectTapGestures
                                            }
                                        }

                                        if (expansion.get() in 0.9f .. 1.1f && overlay_menu?.closeOnTap() == true) {
                                            player.openNpOverlayMenu(null)
                                        }
                                    }
                                )
                            }
                            .thenIf(colourpick_callback != null) {
                                pointerHoverIcon(PointerIcon.Crosshair)
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
                                        corner_percent = thumbnail_rounding
                                    ).dp
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            BackHandler(overlay_menu != null, priority = 2) {
                                player.navigateNpOverlayMenuBack()
                                colourpick_callback = null
                            }

                            Crossfade(overlay_menu) { menu ->
                                CompositionLocalProvider(LocalContentColor provides Color.White) {
                                    menu?.Menu(
                                        { player.status.m_song!! },
                                        { expansion.getAbsolute() },
                                        { player.openNpOverlayMenu(it ?: main_overlay_menu) },
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
                Modifier
                    .graphicsLayer {
                        scaleX = getTextScale()
                    }
                    .padding(start = 10.dp),
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

            val lyrics_state: SongLyricsLoader.ItemState? = current_song?.let { SongLyricsLoader.rememberItemState(it, player.context) }
            val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, true)

            AnimatedVisibility(
                lyrics_state?.lyrics?.synced == true,
                Modifier.fillMaxWidth(),
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                if (!expanded) {
                    lyrics_state?.lyrics?.also { lyrics ->
                        HorizontalLyricsLineDisplay(
                            lyrics = lyrics,
                            getTime = {
                                (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
                            },
                            text_colour = player.getNPOnBackground().copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }

            if (lyrics_state?.lyrics?.synced != true) {
                Spacer(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

private fun PlayerState.performPressAction(
    long_press: Boolean,
    main_overlay_menu: PlayerOverlayMenu,
    setThemeColour: (Color) -> Unit,
    setColourpickCallback: (ColourpickCallback?) -> Unit,
    setOverlayMenu: (PlayerOverlayMenu?) -> Unit
) {
    val custom_action: Boolean =
        if (PlayerSettings.Key.OVERLAY_SWAP_LONG_SHORT_PRESS_ACTIONS.get()) !long_press
        else long_press

    val action: PlayerOverlayMenuAction =
        if (custom_action) PlayerSettings.Key.OVERLAY_CUSTOM_ACTION.getEnum()
        else PlayerOverlayMenuAction.DEFAULT

    when (action) {
        PlayerOverlayMenuAction.OPEN_MAIN_MENU -> setOverlayMenu(main_overlay_menu)
        PlayerOverlayMenuAction.OPEN_THEMING -> {
            setOverlayMenu(
                SongThemePlayerOverlayMenu(
                    setColourpickCallback,
                    setThemeColour
                )
            )
        }
        PlayerOverlayMenuAction.PICK_THEME_COLOUR -> {
            setColourpickCallback { colour ->
                if (colour != null) {
                    setThemeColour(colour)
                    setColourpickCallback(null)
                }
            }
        }
        PlayerOverlayMenuAction.ADJUST_NOTIFICATION_IMAGE_OFFSET -> {
            setOverlayMenu(NotifImagePlayerOverlayMenu())
        }
        PlayerOverlayMenuAction.OPEN_LYRICS -> {
            setOverlayMenu(PlayerOverlayMenu.getLyricsMenu())
        }
        PlayerOverlayMenuAction.OPEN_RELATED -> {
            setOverlayMenu(RelatedContentPlayerOverlayMenu(context.ytapi.SongRelatedContent))
        }
        PlayerOverlayMenuAction.DOWNLOAD -> {
            status.m_song?.also { song ->
                onSongDownloadRequested(song)
            }
        }
    }
}
