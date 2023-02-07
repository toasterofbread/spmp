@file:OptIn(ExperimentalComposeUiApi::class)

package com.spectre7.spmp.ui.component

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.spectre7.spmp.ui.layout.getScreenHeight
import com.spectre7.utils.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

const val LONG_PRESS_ICON_MENU_OPEN_ANIM_MS = 200

class LongPressMenuActionProvider(
    val content_colour: () -> Color,
    val accent_colour: () -> Color,
    val background_colour: () -> Color,
    val playerProvider: () -> PlayerViewContext
) {
    @Composable
    fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) =
        ActionButton(icon, label, accent_colour, modifier = modifier, onClick = onClick, onLongClick = onLongClick)

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
        ) {
            Row(
                modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = if (onLongClick == null) null else {
                            {
                                vibrateShort()
                                onLongClick()
                            }
                        }
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val icon_col = icon_colour()
                Icon(icon, null, tint = if (icon_col.isUnspecified) LocalContentColor.current else icon_col)
                Text(label, fontSize = 15.sp, color = text_colour())
            }
        }
    }
}

class LongPressMenuData(
    val item: MediaItem,
    val thumb_shape: Shape,
    val actions: @Composable LongPressMenuActionProvider.(MediaItem) -> Unit
) {
    internal var thumb_size: IntSize? = null
    internal var thumb_position: Offset? = null
    internal var hide_thumb: Boolean by mutableStateOf(false)
}

fun Modifier.longPressMenuIcon(data: LongPressMenuData, enabled: Boolean = true): Modifier {
    if (!enabled) {
        return this.clip(data.thumb_shape)
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
        .clip(data.thumb_shape)
}

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
                        .clip(data.thumb_shape)
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

    if (show && data.thumb_position != null) {
        val density = LocalDensity.current
        val status_bar_height = getStatusBarHeight(MainActivity.context)

        val initial_pos = remember { with (density) { DpOffset(data.thumb_position!!.x.toDp(), data.thumb_position!!.y.toDp() - status_bar_height) } }
        val initial_size = remember { with (density) { DpSize(data.thumb_size!!.width.toDp(), data.thumb_size!!.height.toDp()) } }

        var fully_open by remember { mutableStateOf(false) }

        val pos = remember { Animatable(initial_pos, DpOffset.VectorConverter) }
        val width = remember { Animatable(initial_size.width.value) }
        val height = remember { Animatable(initial_size.height.value) }
        val panel_alpha = remember { Animatable(1f) }

        var target_position: Offset? by remember { mutableStateOf(null) }
        var target_size: IntSize? by remember { mutableStateOf(null) }

        var accent_colour by remember { mutableStateOf(Color.Unspecified) }

        fun applyPalette(item: MediaItem) {
            accent_colour = (item.getDefaultThemeColour() ?: MainActivity.theme.getBackground(false)).contrastAgainst(MainActivity.theme.getBackground(false), 0.2)
        }

        LaunchedEffect(Unit) {
            if (data.item is Song) {
                val theme_colour = data.item.registry.get<Int>("theme_colour")
                if (theme_colour != null) {
                    accent_colour = Color(theme_colour)
                    return@LaunchedEffect
                }
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

            val pos_target: DpOffset
            val width_target: Float
            val height_target: Float

            if (to_target) {
                with (density) {
                    pos_target = DpOffset(target_position!!.x.toDp(), target_position!!.y.toDp())
                    width_target = target_size!!.width.toDp().value
                    height_target = target_size!!.height.toDp().value
                }
            }
            else {
                pos_target = initial_pos
                width_target = initial_size.width.value
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

                if (!no_transition || to_target) {
                    listOf(
                        launch {
                            pos.animateTo(pos_target, tween(animation_duration))
                        },
                        launch {
                            width.animateTo(width_target, tween(animation_duration))
                        },
                        launch {
                            height.animateTo(height_target, tween(animation_duration))
                        }
                    ).joinAll()
                }

                fully_open = to_target
            }
        }

        LaunchedEffect(Unit) {
            animateValues(true)
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
                    .background(Color.Black.setAlpha(0.5 * panel_alpha.value))
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
                    Column(
                        modifier
                            .alpha(panel_alpha.value)
                            .background(MainActivity.theme.getBackground(false), shape)
                            .fillMaxWidth()
                            .padding(25.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                    .padding(horizontal = 15.dp)
                                , verticalArrangement = Arrangement.Center) {

                                if (data.item is Song) {
                                    Marquee(false) {
                                        Text(
                                            data.item.title,
                                            Modifier.fillMaxWidth(),
                                            color = MainActivity.theme.getOnBackground(false),
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                if (data.item !is Artist) {
                                    val artist = data.item.getAssociatedArtist()
                                    if (artist != null) {
                                        Marquee(false) {
                                            artist.PreviewLong(
                                                content_colour = MainActivity.theme.getOnBackgroundProvider(false),
                                                playerProvider,
                                                true,
                                                Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(thickness = Dp.Hairline, color = MainActivity.theme.getOnBackground(false))

                        val accent_colour_provider = remember (accent_colour) { { accent_colour } }

                        data.actions(LongPressMenuActionProvider(MainActivity.theme.getOnBackgroundProvider(false), accent_colour_provider, MainActivity.theme.getBackgroundProvider(false), playerProvider), data.item)

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
                        })

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
                            })
                        }
                    }
                }

                if (!fully_open) {
                    Box(
                        Modifier
                            .offset(pos.value.x, pos.value.y + status_bar_height)
                            .requiredSize(width.value.dp, height.value.dp)
                            .clip(data.thumb_shape)
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
