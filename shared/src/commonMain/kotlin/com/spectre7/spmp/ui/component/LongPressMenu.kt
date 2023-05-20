package com.spectre7.spmp.ui.component

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.*
import com.spectre7.spmp.platform.composable.PlatformDialog
import com.spectre7.spmp.platform.vibrateShort
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import com.spectre7.utils.composable.Marquee
import kotlinx.coroutines.delay

const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS = 150

class LongPressMenuActionProvider(
    val content_colour: () -> Color,
    val accent_colour: () -> Color,
    val background_colour: () -> Color,
    val closeMenu: () -> Unit
) {
    @Composable
    fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, fill_width: Boolean = true) =
        ActionButton(icon, label, accent_colour, modifier = modifier, onClick = onClick, onLongClick = onLongClick, closeMenu = closeMenu, fill_width = fill_width)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ActiveQueueIndexAction(
        getText: (distance: Int) -> String,
        onClick: (active_queue_index: Int) -> Unit,
        onLongClick: ((active_queue_index: Int) -> Unit)? = null
    ) {
        var active_queue_item: Song? by remember { mutableStateOf(null) }
        AnimatedVisibility(PlayerServiceHost.player.active_queue_index < PlayerServiceHost.status.m_queue_size) {
            if (PlayerServiceHost.player.active_queue_index < PlayerServiceHost.status.m_queue_size) {
                active_queue_item = PlayerServiceHost.player.getSong(PlayerServiceHost.player.active_queue_index)
            }

            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distance = PlayerServiceHost.player.active_queue_index - PlayerServiceHost.status.index + 1
                    ActionButton(
                        Icons.Filled.SubdirectoryArrowRight,
                        getText(distance),
                        fill_width = false,
                        onClick = { onClick(PlayerServiceHost.player.active_queue_index) },
                        onLongClick = onLongClick?.let { { it.invoke(PlayerServiceHost.player.active_queue_index) } }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val button_modifier = Modifier
                            .size(30.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .align(Alignment.CenterVertically)

                        Surface(
                            button_modifier.combinedClickable(
                                remember { MutableInteractionSource() },
                                rememberRipple(),
                                onClick = {
                                    PlayerServiceHost.player.updateActiveQueueIndex(-1)
                                },
                                onLongClick = {
                                    SpMp.context.vibrateShort()
                                    PlayerServiceHost.player.updateActiveQueueIndex(Int.MIN_VALUE)
                                }
                            ),
                            color = accent_colour(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Remove, null, tint = background_colour())
                        }

                        Surface(
                            button_modifier.combinedClickable(
                                remember { MutableInteractionSource() },
                                rememberRipple(),
                                onClick = {
                                    PlayerServiceHost.player.updateActiveQueueIndex(1)
                                },
                                onLongClick = {
                                    SpMp.context.vibrateShort()
                                    PlayerServiceHost.player.updateActiveQueueIndex(Int.MAX_VALUE)
                                }
                            ),
                            color = accent_colour(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, null, tint = background_colour())
                        }
                    }
                }

                Crossfade(active_queue_item, animationSpec = tween(100)) {

                    val player = LocalPlayerState.current
                    CompositionLocalProvider(
                        LocalPlayerState provides remember { player.copy(onClickedOverride = { item, _ -> player.openMediaItem(item) }) }
                    ) {
                        it?.PreviewLong(MediaItem.PreviewParams(contentColour = content_colour))
                    }
                }
            }
        }
    }

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
)

fun Modifier.longPressMenuIcon(data: LongPressMenuData, enabled: Boolean = true): Modifier {
    if (!enabled) {
        return this.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
    }
    return this.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
}

@Composable
fun LongPressMenu(
    showing: Boolean,
    onDismissRequest: () -> Unit,
    data: LongPressMenuData,
    modifier: Modifier = Modifier
) {
    var close_requested by remember { mutableStateOf(false) }
    var show_dialog by remember { mutableStateOf(showing) }
    var show_content by remember { mutableStateOf(true) }

    suspend fun closePopup() {
        show_content = false
        delay(LONG_PRESS_ICON_MENU_OPEN_ANIM_MS.toLong())
        show_dialog = false
        onDismissRequest()
    }

    LaunchedEffect(showing, close_requested) {
        if (!showing || close_requested) {
            closePopup()
            close_requested = false
        }
        else {
            show_dialog = true
            show_content = true
        }
    }

    if (show_dialog) {
        PlatformDialog(
            onDismissRequest = { close_requested = true },
            use_platform_default_width = false,
            dim_behind = false
        ) {
            AnimatedVisibility(
                show_content,
                // Can't find a way to disable Android Dialog's animations, or an alternative
                enter = EnterTransition.None,
                exit = slideOutVertically(tween(LONG_PRESS_ICON_MENU_OPEN_ANIM_MS)) { it }
            ) {
                val accent_colour: MutableState<Color?> = remember { mutableStateOf(null) }

                LaunchedEffect(Unit) {
                    if (data.item is Song && data.item.theme_colour != null) {
                        accent_colour.value = data.item.theme_colour!!
                    }
                }

                val thumb_quality = MediaItemThumbnailProvider.Quality.LOW
                LaunchedEffect(data.item.isThumbnailLoaded(thumb_quality)) {
                    if (!data.item.isThumbnailLoaded(thumb_quality)) {
                        data.item.loadAndGetThumbnail(MediaItemThumbnailProvider.Quality.LOW)
                    }
                    else {
                        accent_colour.value = (data.item.getDefaultThemeColour() ?: Theme.current.background)
                            .contrastAgainst(Theme.current.background, 0.2f)
                    }
                }

                MenuContent(
                    data,
                    accent_colour,
                    modifier,
                    { close_requested = true }
                )
            }
        }
    }
}

@Composable
private fun MenuContent(
    data: LongPressMenuData,
    accent_colour: MutableState<Color?>,
    modifier: Modifier,
    close: () -> Unit
) {
    @Composable
    fun Thumb(modifier: Modifier) {
        data.item.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, modifier.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING)))
    }

    val y_offset = SpMp.context.getStatusBarHeight()

    Box(
        Modifier
            .requiredHeight(SpMp.context.getScreenHeight())
            .offset(y = y_offset)
//            .background(Color.Black.setAlpha(0.5f))
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
                        Marquee {
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
                                Marquee {
                                    val player = LocalPlayerState.current
                                    CompositionLocalProvider(
                                        LocalPlayerState provides remember {
                                            player.copy(
                                                onClickedOverride = { item, _ ->
                                                    close()
                                                    player.onMediaItemClicked(item)
                                                }
                                            )
                                        }
                                    ) {
                                        artist.PreviewLong(MediaItem.PreviewParams(Modifier.fillMaxWidth()))
                                    }
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
                            MenuActions(data, accent_colour.value ?: Theme.current.accent, close = close)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActions(data: LongPressMenuData, accent_colour: Color, close: () -> Unit) {
    val accent_colour_provider = remember (accent_colour) { { accent_colour } }

    data.actions?.invoke(
        LongPressMenuActionProvider(
            Theme.current.on_background_provider,
            accent_colour_provider,
            Theme.current.background_provider,
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
                data.item.setPinnedToHome(!pinned)
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
