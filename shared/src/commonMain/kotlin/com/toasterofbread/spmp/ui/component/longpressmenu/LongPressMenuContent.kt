package com.toasterofbread.spmp.ui.component.longpressmenu

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Divider
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.db.observePinnedToHome
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.MediaItemTitleEditDialog
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.ui.component.mediaitempreview.MediaItemPreviewLong
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.common.copy
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.common.thenIf
import com.toasterofbread.utils.composable.Marquee
import com.toasterofbread.utils.composable.NoRipple

@Composable
internal fun LongPressMenuContent(
    data: LongPressMenuData,
    content_padding: PaddingValues,
    getAccentColour: () -> Color?,
    modifier: Modifier,
    onAction: () -> Unit
) {
    val player = LocalPlayerState.current
    
    @Composable
    fun Thumb(modifier: Modifier) {
        data.item.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, modifier.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING)))
    }

    var show_title_edit_dialog: Boolean by remember { mutableStateOf(false) }
    if (show_title_edit_dialog) {
        MediaItemTitleEditDialog(data.item) {
            show_title_edit_dialog = false
        }
    }

    var item_pinned_to_home: Boolean by data.item.observePinnedToHome()
    val item_title: String? by data.item.observeActiveTitle()

    Column(modifier) {
        val density = LocalDensity.current
        var height by remember { mutableStateOf(0.dp) }

        var show_info by remember { mutableStateOf(false) }
        var main_actions_showing by remember { mutableStateOf(true) }
        var info_showing by remember { mutableStateOf(false) }

        Column(
            Modifier
                .background(Theme.background, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .fillMaxWidth()
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
            Box(Modifier.height(content_padding.calculateTopPadding()).fillMaxWidth()) {
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
                            Modifier.size(pin_button_size)
                        ) {
                            Icon(
                                if (pinned) Icons.Filled.PushPin
                                else Icons.Outlined.PushPin,
                                null
                            )
                        }
                    }
                }
            }

            Column(
                Modifier.padding(content_padding.copy(top = 0.dp)).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)
            ) {
                CompositionLocalProvider(LocalContentColor provides Theme.on_background) {
                    Row(
                        Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                    ) {
                        Thumb(Modifier.aspectRatio(1f))

                        // Item info
                        Column(
                            Modifier
                                .fillMaxSize()
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
                            if (data.item is MediaItem.WithArtist) {
                                val item_artist: Artist? by data.item.Artist.observe(player.database)
                                item_artist?.also { artist ->
                                    Marquee {
                                        val player = LocalPlayerState.current
                                        CompositionLocalProvider(
                                            LocalPlayerState provides remember {
                                                player.copy(
                                                    onClickedOverride = { item, _ ->
                                                        onAction()
                                                        player.onMediaItemClicked(item)
                                                    }
                                                )
                                            }
                                        ) {
                                            MediaItemPreviewLong(artist, Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Info header
                    Row(Modifier.requiredHeight(1.dp)) {
                        var info_title_width: Int by remember { mutableStateOf(0) }
                        var box_width: Int by remember { mutableStateOf(-1) }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onSizeChanged { box_width = it.width }
                        ) {
                            Crossfade(show_info) { info ->
                                val text = if (info) data.info_title else data.getInitialInfoTitle?.invoke()
                                val current = info == show_info
                                if (text != null) {
                                    Text(
                                        text,
                                        Modifier.offset(y = (-10).dp).onSizeChanged { if (current) info_title_width = it.width },
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
                                        if (box_width < 0) fillMaxWidth()
                                        else width(animateDpAsState(
                                            with(LocalDensity.current) {
                                                if (info_title_width == 0) box_width.toDp() else (box_width - info_title_width).toDp() - 15.dp
                                            }
                                        ).value)
                                    }
                                    .requiredHeight(20.dp)
                                    .background(Theme.background)
                                    .align(Alignment.CenterEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Divider(
                                    thickness = Dp.Hairline,
                                    color = Theme.on_background
                                )
                            }
                        }

                        if (data.infoContent != null) {
                            IconButton({ show_info = !show_info }, Modifier.requiredHeight(40.dp)) {
                                Crossfade(show_info) { info ->
                                    Icon(if (info) Icons.Filled.Close else Icons.Filled.Info, null)
                                }
                            }
                        }

                        data.SideButton(Modifier.requiredHeight(40.dp), Theme.background)
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
                                    { getAccentColour() ?: Theme.accent },
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
                                LongPressMenuActions(data, { getAccentColour() ?: Theme.accent }, onAction = onAction)
                            }
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
    close: () -> Unit
) {
    Box(
        modifier
            .background(Color.Black.setAlpha(0.5f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = close)
    )
}
