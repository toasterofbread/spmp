package com.spectre7.spmp.ui.component

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.platform.composable.PlatformDialog
import com.spectre7.spmp.resources.getString
import com.spectre7.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.spectre7.spmp.ui.layout.nowplaying.overlay.DEFAULT_THUMBNAIL_ROUNDING
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.composable.Marquee
import com.spectre7.utils.contrastAgainst
import com.spectre7.utils.setAlpha
import com.spectre7.utils.thenIf
import kotlinx.coroutines.delay

private const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS = 150
private const val MENU_ITEM_SPACING = 20

data class LongPressMenuData(
    val item: MediaItem,
    val thumb_shape: Shape? = null,
    val infoContent: (@Composable ColumnScope.(accent: Color) -> Unit)? = null,
    val info_title: String? = null,
    val multiselect_context: MediaItemMultiSelectContext? = null,
    val multiselect_key: Int? = null,
    val sideButton: (@Composable (Modifier, background: Color, accent: Color) -> Unit)? = null,
    val actions: (@Composable LongPressMenuActionProvider.(item: MediaItem, spacing: Dp) -> Unit)? = null
) {
    var current_interaction_stage: MediaItemPreviewInteractionPressStage? by mutableStateOf(null)
    private val HINT_MIN_STAGE = MediaItemPreviewInteractionPressStage.LONG_1

    fun getInteractionHintScale(): Int {
        return current_interaction_stage?.let {
            if (it < HINT_MIN_STAGE) 0
            else it.ordinal - HINT_MIN_STAGE.ordinal + 1
        } ?: 0
    }
}

@Composable
fun Modifier.longPressMenuIcon(data: LongPressMenuData, enabled: Boolean = true): Modifier {
    val scale by animateFloatAsState(1f + (if (!enabled) 0f else data.getInteractionHintScale() * 0.2f))
    return this
        .clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING))
        .scale(scale)
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
                        data.item.loadThumbnail(MediaItemThumbnailProvider.Quality.LOW)
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
                    { if (Settings.KEY_LPM_CLOSE_ON_ACTION.get()) close_requested = true },
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
    onAction: () -> Unit,
    close: () -> Unit
) {
    @Composable
    fun Thumb(modifier: Modifier) {
        data.item.Thumbnail(MediaItemThumbnailProvider.Quality.LOW, modifier.clip(data.thumb_shape ?: RoundedCornerShape(DEFAULT_THUMBNAIL_ROUNDING)))
    }

    Box(
        Modifier
            .requiredHeight(SpMp.context.getScreenHeight() + 1.dp)
            .offset(y = -SpMp.context.getStatusBarHeight())
            .background(Color.Black.setAlpha(0.5f))
    ) {
        val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

        Column(Modifier.fillMaxSize().animateContentSize()) {
            Spacer(Modifier
                .fillMaxSize()
                .weight(1f)
                .clickable(remember { MutableInteractionSource() }, null, onClick = close)
            )

            var height by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            val padding = 25.dp

            var show_info by remember { mutableStateOf(false) }
            var main_actions_showing by remember { mutableStateOf(false) }
            var info_showing by remember { mutableStateOf(false) }

            Column(
                modifier
                    .background(Theme.current.background, shape)
                    .fillMaxWidth()
                    .padding(padding)
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
                verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)
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
                                                    onAction()
                                                    player.onMediaItemClicked(item)
                                                }
                                            )
                                        }
                                    ) {
                                        artist.PreviewLong(MediaItemPreviewParams(Modifier.fillMaxWidth()))
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
                    Column(verticalArrangement = Arrangement.spacedBy(MENU_ITEM_SPACING.dp)) {
                        if (info) {
                            DisposableEffect(Unit) {
                                info_showing = true
                                onDispose {
                                    info_showing = false
                                }
                            }
                            data.infoContent?.invoke(this, accent_colour.value ?: Theme.current.accent)
                        }
                        else {
                            DisposableEffect(Unit) {
                                main_actions_showing = true
                                onDispose {
                                    main_actions_showing = false
                                }
                            }
                            MenuActions(data, accent_colour.value ?: Theme.current.accent, onAction = onAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActions(data: LongPressMenuData, accent_colour: Color, onAction: () -> Unit) {
    val accent_colour_provider = remember (accent_colour) { { accent_colour } }

    // Data-provided actions
    data.actions?.invoke(
        LongPressMenuActionProvider(
            Theme.current.on_background_provider,
            accent_colour_provider,
            Theme.current.background_provider,
            onAction
        ),
        data.item,
        MENU_ITEM_SPACING.dp
    )

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
            onAction = onAction
        )
    }

    data.item.url?.also { url ->
        // Share
        if (SpMp.context.canShare()) {
            LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, getString("lpm_action_share"), accent_colour_provider, onClick = {
                SpMp.context.shareText(url, if (data.item is Song) data.item.title else null)
            }, onAction = onAction)
        }

        // Open
        if (SpMp.context.canOpenUrl()) {
            LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, getString("lpm_action_open_external"), accent_colour_provider, onClick = {
                SpMp.context.openUrl(url)
            }, onAction = onAction)
        }
    }
}
