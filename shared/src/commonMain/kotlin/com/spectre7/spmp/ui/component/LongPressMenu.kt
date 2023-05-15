package com.spectre7.spmp.ui.component

import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.composable.PlatformDialog
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.resources.getStringTODO
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.mainpage.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.Marquee
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS = 150

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
                                SpMp.context.vibrateShort()
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
    val multiselect_context: MediaItemMultiSelectContext? = null,
    val multiselect_key: Int? = null,
    val sideButton: (@Composable (Modifier, background: Color, accent: Color) -> Unit)? = null,
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

@Composable
fun LongPressMenu(
    showing: Boolean,
    no_transition: Boolean,
    onDismissRequest: () -> Unit,
    playerProvider: () -> PlayerViewContext,
    data: LongPressMenuData,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(showing) }
    LaunchedEffect(showing) {
        if (showing) {
            show = true
        }
    }

    if (show && (data.thumb_shape == null || data.thumb_position != null)) {

        // Animation logic

        val density = LocalDensity.current
        val status_bar_height = SpMp.context.getStatusBarHeight()

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

        val target_position: MutableState<Offset?> = remember { mutableStateOf(null) }
        val target_size: MutableState<IntSize?> = remember { mutableStateOf(null) }

        suspend fun animateValues(to_target: Boolean) {

            var pos_target: DpOffset = DpOffset.Unspecified
            var width_target: Float = Float.NaN
            var height_target: Float = Float.NaN

            if (to_target) {
                with (density) {
                    pos_target = DpOffset(target_position.value!!.x.toDp(), target_position.value!!.y.toDp())
                    width_target = target_size.value!!.width.toDp().value
                    height_target = target_size.value!!.height.toDp().value
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

        // UI

        val accent_colour: MutableState<Color?> = remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            if (data.item is Song && data.item.theme_colour != null) {
                accent_colour.value = data.item.theme_colour!!
            }
        }

        val thumb_quality = MediaItem.ThumbnailQuality.LOW
        LaunchedEffect(data.item.isThumbnailLoaded(thumb_quality)) {
            if (!data.item.isThumbnailLoaded(thumb_quality)) {
                data.item.loadAndGetThumbnail(MediaItem.ThumbnailQuality.LOW)
            }
            else {
                accent_colour.value = (data.item.getDefaultThemeColour() ?: Theme.current.background)
                    .contrastAgainst(Theme.current.background, 0.2f)
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

        PlatformDialog(
            onDismissRequest = { close_requested = true },
            use_platform_default_width = false,
            dim_behind = false
        ) {
            MenuContent(
                data,
                accent_colour,
                panel_alpha,
                fully_open,
                no_transition,
                pos,
                width,
                height,
                target_position,
                target_size,
                modifier,
                playerProvider,
                { close_requested = true }
            )
        }
    }
}

@Composable
private fun MenuContent(
    data: LongPressMenuData,
    accent_colour: MutableState<Color?>,
    panel_alpha: Animatable<Float, AnimationVector1D>,
    fully_open: Boolean,
    no_transition: Boolean,
    pos: Animatable<DpOffset, AnimationVector2D>?,
    width: Animatable<Float, AnimationVector1D>?,
    height: Animatable<Float, AnimationVector1D>?,
    target_position: MutableState<Offset?>,
    target_size: MutableState<IntSize?>,
    modifier: Modifier,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    val y_offset = 10

    @Composable
    fun Thumb(modifier: Modifier) {
        Crossfade(data.item.loadAndGetThumbnail(MediaItem.ThumbnailQuality.LOW)) { thumbnail ->
            if (thumbnail != null) {
                Image(
                    thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
                )
            }
        }
    }

    Box(
        Modifier
            .requiredHeight(SpMp.context.getScreenHeight())
            .offset { IntOffset(0, y_offset) }
            .background(Color.Black.setAlpha(0.5f * panel_alpha.value))
    ) {
        val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier
                .fillMaxSize()
                .weight(1f)
                .clickable(remember { MutableInteractionSource() }, null, onClick = close)
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

                    // Main item thumbnail
                    Thumb(Modifier
                        .drawWithContent {
                            if (fully_open) {
                                drawContent()
                            }
                        }
                        .aspectRatio(1f)
                        .onSizeChanged {
                            target_size.value = it
                        }
                        .onGloballyPositioned {
                            target_position.value = it.localPositionOf(
                                it.parentCoordinates!!.parentCoordinates!!,
                                it.positionInRoot()
                            )
                        }
                    )

                    // Item info
                    Column(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 15.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Title
                        Marquee(autoscroll = false) {
                            Text(
                                data.item.title ?: "",
                                Modifier.fillMaxWidth(),
                                color = Theme.current.on_background,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Artist
                        if (data.item !is Artist) {
                            val artist = data.item.artist
                            if (artist != null) {
                                Marquee(autoscroll = false) {
                                    artist.PreviewLong(MediaItem.PreviewParams(
                                        remember { { playerProvider().let { player ->
                                            player.copy(onClickedOverride = {
                                                close()
                                                player.onMediaItemClicked(it)
                                            })
                                        }}},
                                        Modifier.fillMaxWidth()
                                    ))
                                }
                            }
                        }
                    }
                }

                var show_info by remember { mutableStateOf(false) }

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
                                .run {
                                    if (box_width < 0) fillMaxWidth()
                                    else width(animateDpAsState(
                                        with(LocalDensity.current) {
                                            if (show_info) (box_width - info_title_width).toDp() - 15.dp else box_width.toDp()
                                        }
                                    ).value)
                                }
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

                    data.sideButton?.invoke(Modifier.requiredHeight(40.dp), Theme.current.background, accent_colour.value ?: Theme.current.accent)
                }

                // Info/action list
                Crossfade(show_info) { info ->
                    Column(verticalArrangement = Arrangement.spacedBy(item_spacing)) {
                        if (info) {
                            data.infoContent?.invoke(this, accent_colour.value ?: Theme.current.accent)
                        }
                        else {
                            MenuActions(data, accent_colour.value ?: Theme.current.accent, playerProvider, close = close)
                        }
                    }
                }
            }
        }

        if (!fully_open && pos != null && width != null && height != null) {
            Box(
                Modifier
                    .offset { IntOffset(pos.value.x.toPx().roundToInt(), pos.value.y.toPx().roundToInt() - y_offset) }
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

@Composable
private fun MenuActions(data: LongPressMenuData, accent_colour: Color, playerProvider: () -> PlayerViewContext, close: () -> Unit) {
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

    // Begin multiple selection
    data.multiselect_context?.also { multiselect ->
        Crossfade(multiselect.is_active) { active ->
            LongPressMenuActionProvider.ActionButton(
                Icons.Default.Checklist,
                getString(if (active) "multiselect_end" else "multiselect_begin"),
                accent_colour_provider,
                onClick = {
                    multiselect.setActive(!active)
                    multiselect.toggleItem(data.item, data.multiselect_key)
                },
                closeMenu = close
            )
        }
    }

    // Pin / unpin
    Crossfade(data.item.pinned_to_home) { pinned ->
        LongPressMenuActionProvider.ActionButton(
            if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            getString(
                if (pinned) "lpm_action_home_unpin"
                else "lpm_action_home_pin"
            ),
            accent_colour_provider,
            onClick = {
                data.item.setPinnedToHome(!pinned, playerProvider)
            },
            closeMenu = close
        )
    }

    if (SpMp.context.canShare()) {
        LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, getString("lpm_action_share"), accent_colour_provider, onClick = {
            SpMp.context.shareText(data.item.url, if (data.item is Song) data.item.title else null)
        }, closeMenu = close)
    }

    if (SpMp.context.canOpenUrl()) {
        LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, getString("lpm_action_open_external"), accent_colour_provider, onClick = {
            SpMp.context.openUrl(data.item.url)
        }, closeMenu = close)
    }
}
