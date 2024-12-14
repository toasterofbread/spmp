package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.toastbits.composekit.components.platform.composable.platformClickable
import dev.toastbits.composekit.context.vibrateShort
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.util.composable.AlignableCrossfade
import dev.toastbits.composekit.components.utils.composable.Marquee
import dev.toastbits.composekit.components.utils.composable.NoRipple
import dev.toastbits.composekit.components.utils.composable.PlatformClickableIconButton
import dev.toastbits.composekit.components.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.service.playercontroller.LocalPlayerClickOverrides
import com.toasterofbread.spmp.service.playercontroller.PlayerClickOverrides
import com.toasterofbread.spmp.ui.component.MediaItemTitleEditDialog
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.songtheme.DEFAULT_THUMBNAIL_ROUNDING
import dev.toastbits.composekit.util.composable.copy
import dev.toastbits.composekit.util.composable.thenIf
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_long_press_actions

@Composable
internal fun LongPressMenuContent(
    data: LongPressMenuData,
    shape: Shape,
    background_colour: Color,
    content_padding: PaddingValues,
    getAccentColour: () -> Color?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    val player: PlayerState = LocalPlayerState.current
    val click_overrides: PlayerClickOverrides = LocalPlayerClickOverrides.current

    @Composable
    fun Thumb(modifier: Modifier) {
        data.item.Thumbnail(ThumbnailProvider.Quality.LOW, modifier.clip(data.thumb_shape ?: RoundedCornerShape(
            DEFAULT_THUMBNAIL_ROUNDING
        )))
    }

    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }
    if (show_title_edit_dialog) {
        MediaItemTitleEditDialog(data.item) {
            show_title_edit_dialog = false
        }
    }

    var item_pinned_to_home: Boolean by data.item.observePinnedToHome()
    val item_title: String? by data.item.observeActiveTitle()

    val density: Density = LocalDensity.current
    var height: Dp by remember { mutableStateOf(0.dp) }

    var show_info: Boolean by remember { mutableStateOf(false) }
    var main_actions_showing: Boolean by remember { mutableStateOf(true) }
    var info_showing: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier
            .background(background_colour, shape)
            .onSizeChanged {
                if (show_info || !main_actions_showing) {
                    return@onSizeChanged
                }

                val h = with(density) { it.height.toDp() }
                if (h > height) {
                    height = h
                }
            }
            .thenIf(show_info || info_showing, Modifier.height(height)),
    ) {
        Box(Modifier.height(content_padding.calculateTopPadding()).align(Alignment.End)) {
            NoRipple {
                val pin_button_size = 24.dp
                val pin_button_padding = 15.dp
                Crossfade(
                    item_pinned_to_home,
                    Modifier.align(Alignment.CenterEnd).offset(x = -pin_button_padding, y = pin_button_padding)
                ) { pinned ->
                    IconButton(
                        {
                            item_pinned_to_home = !pinned
                        },
                        Modifier.size(pin_button_size).bounceOnClick()
                    ) {
                        Icon(
                            if (pinned) Icons.Filled.PushPin
                            else Icons.Outlined.PushPin,
                            null,
                            tint = background_colour.getContrasted()
                        )
                    }
                }
            }
        }

        Column(
            Modifier.padding(content_padding.copy(top = 0.dp)),
            verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides background_colour.getContrasted()) {
                Row(Modifier.height(80.dp)) {
                    Thumb(Modifier.aspectRatio(1f))

                    // Item info
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 15.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Title
                        Marquee(
                            Modifier.platformClickable(
                                onAltClick = {
                                    show_title_edit_dialog = !show_title_edit_dialog
                                    player.context.vibrateShort()
                                }
                            )
                        ) {
                            Text(
                                item_title ?: "",
                                Modifier.fillMaxWidth(),
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Artist
                        if (data.item is MediaItem.WithArtists) {
                            val item_artists: List<Artist>? by data.item.Artists.observe(player.database)
                            item_artists?.firstOrNull()?.also { artist ->
                                Marquee {
                                    CompositionLocalProvider(LocalPlayerClickOverrides provides click_overrides.copy(
                                        onClickOverride = { item, _ ->
                                            onAction()
                                            click_overrides.onMediaItemClicked(item, player)
                                        }
                                    )) {
                                        MediaItemPreviewLong(artist)
                                    }
                                }
                            }
                        }
                    }
                }

                // Info header
                Row(Modifier.requiredHeight(1.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    var info_title_width: Int by remember { mutableStateOf(0) }

                    Box(
                        Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AlignableCrossfade(show_info, Modifier.requiredHeight(40.dp), contentAlignment = Alignment.CenterStart) { info ->
                            val text = if (info) stringResource(Res.string.lpm_long_press_actions) else data.getTitle?.invoke()
                            val current = info == show_info
                            if (text != null) {
                                Text(
                                    text,
                                    Modifier.onSizeChanged { if (current) info_title_width = it.width },
                                    overflow = TextOverflow.Visible
                                )
                            }
                            else if (current) {
                                info_title_width = 0
                            }
                        }

                        Box(
                            Modifier
                                .run {
                                    padding(
                                        start = animateDpAsState(
                                            with (density) {
                                                if (info_title_width == 0) 0.dp else (info_title_width.toDp() + 15.dp)
                                            }
                                        ).value
                                    )
                                }
                                .requiredHeight(20.dp)
                                .background(background_colour)
                                .align(Alignment.CenterEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            WaveBorder(
                                Modifier.fillMaxWidth(),
                                border_thickness = 2.dp,
                                waves = 6,
                                border_colour = background_colour.getContrasted().copy(alpha = 0.1f),
                                getOffset = { 0f },
                                getColour = { background_colour }
                            )
                        }
                    }

                    data.SideButton(
                        Modifier.requiredHeight(40.dp),
                        background_colour
                    )

                    PlatformClickableIconButton(
                        onClick = {
                            show_info = !show_info
                        },
                        modifier = Modifier.requiredHeight(40.dp),
                        apply_minimum_size = false
                    ) {
                        Crossfade(show_info) { info ->
                            Icon(if (info) Icons.Filled.Close else Icons.Filled.Info, null)
                        }
                    }
                }

                // Info/action list
                    Crossfade(show_info) { info ->
                        Column(verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)) {
                            if (info) {
                                DisposableEffect(Unit) {
                                    info_showing = true
                                    onDispose {
                                        info_showing = false
                                    }
                                }
                                LongPressMenuInfoActions(
                                    data,
                                    MENU_ITEM_SPACING.dp,
                                    { getAccentColour() ?: player.theme.accent },
                                    onAction = onAction
                                )
                            }
                            else {
                                DisposableEffect(Unit) {
                                    main_actions_showing = true
                                    onDispose {
                                        main_actions_showing = false
                                    }
                                }
                                LongPressMenuActions(data, background_colour, { getAccentColour() ?: player.theme.accent }, onAction = onAction)
                            }
                        }
                    }
            }
        }
    }
}

@Composable
internal fun LongPressMenuBackground(
    modifier: Modifier = Modifier,
    onScroll: () -> Unit = {},
    enable_input: Boolean = true,
    getAlpha: () -> Float = { 1f },
    close: () -> Unit
) {
    Box(
        modifier
            .graphicsLayer {
                alpha = getAlpha() * 0.5f
            }
            .background(Color.Black)
            .thenIf(enable_input) {
                pointerInput(Unit) {
                    while (currentCoroutineContext().isActive) {
                        awaitPointerEventScope {
                            val event: PointerEvent = awaitPointerEvent()
                            if (event.type == PointerEventType.Release) {
                                close()
                            }
                            else if (event.type == PointerEventType.Scroll) {
                                onScroll()
                            }
                        }
                    }
                }
            }
    )
}
