@file:OptIn(ExperimentalComposeUiApi::class)

package com.spectre7.spmp.ui.component

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.DEFAULT_THUMBNAIL_ROUNDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS = 200

class LongPressMenuActionProvider(
    val content_colour: () -> Color,
    val accent_colour: () -> Color,
    val background_colour: () -> Color,
    val playerProvider: () -> PlayerViewContext,
    val closeMenu: () -> Unit
) {
    @Composable
    fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, fill_width: Boolean = true) =
        ActionButton(icon, label, accent_colour, modifier = modifier, onClick = onClick, onLongClick = onLongClick, closeMenu = closeMenu, fill_width = fill_width)

    companion object {
        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ActionButton(
            icon: ImageVector,
            label: String,
            icon_colour: () -> Color = { Color.Unspecified },
            text_colour: () -> Color = { Color.Unspecified },
            modifier: Modifier = Modifier,
            onClick: () -> Unit,
            onLongClick: (() -> Unit)? = null,
            closeMenu: () -> Unit,
            fill_width: Boolean = true
        ) {
            Row(
                modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onClick()
                            closeMenu()
                        },
                        onLongClick = if (onLongClick == null) null else {
                            {
                                vibrateShort()
                                onLongClick()
                                closeMenu()
                            }
                        }
                    )
                    .let { if (fill_width) it.fillMaxWidth() else it },
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon_col = icon_colour()
                Icon(icon, null, tint = if (icon_col.isUnspecified) LocalContentColor.current else icon_col)
                Text(label, fontSize = 15.sp, color = text_colour())
            }
        }
    }
}

data class LongPressMenuData(
    val item: MediaItem,
    val thumb_shape: Shape? = null,
    val infoContent: (@Composable ColumnScope.(accent: Color) -> Unit)? = null,
    val info_title: String? = null,
    val actions: (@Composable LongPressMenuActionProvider.(MediaItem) -> Unit)? = null
) {
    internal var thumb_size: IntSize? = null
    internal var thumb_position: Offset? = null
    internal var hide_thumb: Boolean by mutableStateOf(false)
}

fun Modifier.longPressMenuIcon(data: LongPressMenuData, enabled: Boolean = true): Modifier {
    if (!enabled) {
        return this.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
    }
    return this
        .onGloballyPositioned {
            data.thumb_position = it.positionInWindow()
        }
        .onSizeChanged {
            data.thumb_size = it
        }
        .drawWithContent {
            if (!data.hide_thumb) {
                drawContent()
            }
        }
        .clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LongPressIconMenu(
    showing: Boolean,
    no_transition: Boolean,
    onDismissRequest: () -> Unit,
    playerProvider: () -> PlayerViewContext,
    data: LongPressMenuData,
    modifier: Modifier = Modifier
) {
    @Composable
    fun Thumb(modifier: Modifier) {
        Crossfade(data.item.getThumbnail(MediaItem.ThumbnailQuality.LOW)) { thumbnail ->
            if (thumbnail != null) {
                Image(
                    thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
                )
            }
        }
    }

    var show by remember { mutableStateOf(showing) }
    LaunchedEffect(showing) {
        if (showing) {
            show = true
        }
    }

    if (show && (data.thumb_shape == null || data.thumb_position != null)) {
        val density = LocalDensity.current
        val status_bar_height = getStatusBarHeight(MainActivity.context)

        val initial_pos = remember {
            if (data.thumb_position == null) null else
            with (density) { DpOffset(data.thumb_position!!.x.toDp(), data.thumb_position!!.y.toDp() - status_bar_height) }
        }
        val initial_size = remember {
            if (data.thumb_size == null) null else
            with (density) { DpSize(data.thumb_size!!.width.toDp(), data.thumb_size!!.height.toDp()) }
        }

        var fully_open by remember { mutableStateOf(false) }

        val pos = remember {
            if (initial_pos == null) null else
            Animatable(initial_pos, DpOffset.VectorConverter)
        }
        val width = remember {
            if (initial_size == null) null else
            Animatable(initial_size.width.value)
        }
        val height = remember {
            if (initial_size == null) null else
            Animatable(initial_size.height.value)
        }
        val panel_alpha = remember { Animatable(1f) }

        var target_position: Offset? by remember { mutableStateOf(null) }
        var target_size: IntSize? by remember { mutableStateOf(null) }

        var accent_colour by remember { mutableStateOf(Color.Unspecified) }

        fun applyPalette(item: MediaItem) {
            accent_colour = (item.getDefaultThemeColour() ?: Theme.current.background)
                .contrastAgainst(Theme.current.background, 0.2f)
        }

        LaunchedEffect(Unit) {
            if (data.item is Song && data.item.theme_colour != null) {
                accent_colour = data.item.theme_colour!!
            }
        }

        LaunchedEffect(data.item.thumbnail_palette) {
            if (data.item.thumbnail_palette == null) {
                data.item.getThumbnail(MediaItem.ThumbnailQuality.LOW)
            }
            else {
                applyPalette(data.item)
            }
        }

        suspend fun animateValues(to_target: Boolean) {

            var pos_target: DpOffset = DpOffset.Unspecified
            var width_target: Float = Float.NaN
            var height_target: Float = Float.NaN

            if (to_target) {
                with (density) {
                    pos_target = DpOffset(target_position!!.x.toDp(), target_position!!.y.toDp())
                    width_target = target_size!!.width.toDp().value
                    height_target = target_size!!.height.toDp().value
                }
            }
            else if (initial_pos != null) {
                pos_target = initial_pos
                width_target = initial_size!!.width.value
                height_target = initial_size.height.value
            }

            if (!to_target) {
                fully_open = false
            }

            coroutineScope {
                val animation_duration = if (no_transition && to_target) 0 else LONG_PRESS_ICON_MENU_OPEN_ANIM_MS

                launch {
                    panel_alpha.animateTo(if (to_target) 1f else 0f, tween(animation_duration))
                }

                if ((!no_transition || to_target) && initial_pos != null) {
                    listOf(
                        launch {
                            pos?.animateTo(pos_target, tween(animation_duration))
                        },
                        launch {
                            width?.animateTo(width_target, tween(animation_duration))
                        },
                        launch {
                            height?.animateTo(height_target, tween(animation_duration))
                        }
                    ).joinAll()
                }

                fully_open = to_target
            }
        }

        LaunchedEffect(target_position) {
            if (target_position != null) {
                animateValues(true)
            }
        }

        suspend fun closePopup() {
            animateValues(false)
            data.hide_thumb = false
            show = false
            onDismissRequest()
        }

        var close_requested by remember { mutableStateOf(false) }
        LaunchedEffect(showing, close_requested) {
            if (!showing || close_requested) {
                closePopup()
            }
        }

        Dialog(
            onDismissRequest = { close_requested = true },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {

            val dialog = LocalView.current.parent as DialogWindowProvider
            dialog.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            Box(
                Modifier
                    .requiredHeight(getScreenHeight())
                    .offset(y = status_bar_height * -0.5f)
                    .background(Color.Black.setAlpha(0.5f * panel_alpha.value))
            ) {
                val shape = RoundedCornerShape(topStartPercent = 12, topEndPercent = 12)

                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .clickable(remember { MutableInteractionSource() }, null) {
                            close_requested = true
                        }
                    )

                    val item_spacing = 20.dp
                    val padding = 25.dp

                    Column(
                        modifier
                            .alpha(panel_alpha.value)
                            .background(Theme.current.background, shape)
                            .fillMaxWidth()
                            .padding(padding),
                        verticalArrangement = Arrangement.spacedBy(item_spacing)
                    ) {
                        Row(
                            Modifier
                                .height(80.dp)
                                .fillMaxWidth()
                        ) {

                            Thumb(Modifier
                                .drawWithContent {
                                    if (fully_open) {
                                        drawContent()
                                    }
                                }
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
                                    .padding(horizontal = 15.dp), 
                                verticalArrangement = Arrangement.Center
                            ) {
                                Marquee(false) {
                                    Text(
                                        data.item.title ?: "",
                                        Modifier.fillMaxWidth(),
                                        color = Theme.current.on_background,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (data.item !is Artist) {
                                    val artist = data.item.artist
                                    if (artist != null) {
                                        Marquee(false) {
                                            artist.PreviewLong(
                                                content_colour = Theme.current.on_background_provider,
                                                remember { { playerProvider().let { player ->
                                                    player.copy(onClickedOverride = {
                                                        close_requested = true
                                                        player.onMediaItemClicked(it)
                                                    })
                                                }}},
                                                true,
                                                Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        var show_info by remember { mutableStateOf(false) }

                        Row(Modifier.requiredHeight(1.dp)) {
                            var info_title_width: Int by remember { mutableStateOf(0) }
                            var box_width: Int by remember { mutableStateOf(0) }

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .onSizeChanged { box_width = it.width }
                            ) {
                                if (data.info_title != null) {
                                    Text(
                                        data.info_title,
                                        Modifier.offset(y = (-10).dp).onSizeChanged { info_title_width = it.width },
                                        color = Theme.current.on_background,
                                        overflow = TextOverflow.Visible
                                    )
                                }

                                Box(
                                    Modifier
                                        .width(animateDpAsState(
                                            with(LocalDensity.current) {
                                                if (show_info) (box_width - info_title_width).toDp() - 15.dp else box_width.toDp()
                                            }
                                        ).value)
                                        .requiredHeight(20.dp)
                                        .background(Theme.current.background)
                                        .align(Alignment.CenterEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Divider(
                                        thickness = Dp.Hairline,
                                        color = Theme.current.on_background
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
                        }

                        Crossfade(show_info) { info ->
                            Column(verticalArrangement = Arrangement.spacedBy(item_spacing)) {
                                if (info) {
                                    data.infoContent?.invoke(this, accent_colour)
                                }
                                else {
                                    Actions(data, accent_colour, playerProvider) { close_requested = true }
                                }
                            }
                        }
                    }
                }

                if (!fully_open && pos != null && width != null && height != null) {
                    Box(
                        Modifier
                            .offset(pos.value.x, pos.value.y + status_bar_height)
                            .requiredSize(width.value.dp, height.value.dp)
                            .clip(
                                data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING)
                            )
                            .alpha(if (no_transition) panel_alpha.value else 1f)
                    ) {
                        Thumb(Modifier.fillMaxSize())
                        data.hide_thumb = true
                    }
                }
            }
        }
    }
}

@Composable
private fun Actions(data: LongPressMenuData, accent_colour: Color, playerProvider: () -> PlayerViewContext, close: () -> Unit) {
    val accent_colour_provider = remember (accent_colour) { { accent_colour } }

    data.actions?.invoke(
        LongPressMenuActionProvider(
            Theme.current.on_background_provider,
            accent_colour_provider,
            Theme.current.background_provider,
            playerProvider,
            close
        ),
        data.item
    )

    val share_intent = remember(data.item.url) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND

            if (data.item is Song) {
                putExtra(Intent.EXTRA_TITLE, data.item.title)
            }

            putExtra(Intent.EXTRA_TEXT, data.item.url)
            type = "text/plain"
        }, null)
    }

    LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, "Share", accent_colour_provider, onClick = {
        MainActivity.context.startActivity(share_intent)
    }, closeMenu = close)

    val open_intent: Intent? = remember(data.item.url) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.item.url))
        if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
            null
        }
        else {
            intent
        }
    }

    if (open_intent != null) {
        LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, "Open externally", accent_colour_provider, onClick = {
            MainActivity.context.startActivity(open_intent)
        }, closeMenu = close)
    }
}
