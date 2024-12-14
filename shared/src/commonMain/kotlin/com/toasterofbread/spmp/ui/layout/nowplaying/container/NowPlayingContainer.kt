package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.thenIf
import dev.toastbits.composekit.util.blendWith
import dev.toastbits.composekit.util.getContrasted
import dev.toastbits.composekit.components.utils.composable.getTop
import dev.toastbits.composekit.components.utils.composable.getBottom
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPOnBackground
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.BarColourState
import kotlinx.coroutines.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun NowPlayingContainer(
    pages: List<NowPlayingPage>,
    shouldShowBottomBarInPage: (NowPlayingPage) -> Boolean,
    getBottomBarHeight: () -> Dp,
    modifier: Modifier = Modifier,
) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()

    val swipe_state: AnchoredDraggableState<Int> = expansion.swipe_state
    val top_bar: NowPlayingTopBar = remember { NowPlayingTopBar() }
    val bottom_inset: Dp = WindowInsets.getBottom(player.np_overlay_menu == null)

    val page_height: Dp by remember(bottom_inset) { derivedStateOf { player.screen_size.height } }
    if (page_height <= 0.dp) {
        return
    }

    UpdateAnchors(swipe_state, pages, page_height, coroutine_scope)
    UpdateBarColours(page_height)

    val is_shut: Boolean by remember { derivedStateOf { swipe_state.targetValue == 0 } }
    BackHandler({ !is_shut }, priority = 1) {
        coroutine_scope.launch {
            expansion.scroll(-1)
        }
    }

    val swipe_interaction_source: MutableInteractionSource = remember { MutableInteractionSource() }

    val enable_swipe_gesture: Boolean = !Platform.DESKTOP.isCurrent()
    val swipe_modifier: Modifier = remember(swipe_state, swipe_interaction_source, enable_swipe_gesture) {
        Modifier.anchoredDraggable(
            state = swipe_state,
            orientation = Orientation.Vertical,
            reverseDirection = true,
            interactionSource = swipe_interaction_source,
            enabled = enable_swipe_gesture
        )
    }

    Column(
        modifier
            .fillMaxWidth()
            .requiredHeight(page_height * pages.size)
            .offset(
                y = (page_height * (1f + ((pages.size - 1) / 2f)))
            )
            .offset {
                if (swipe_state.offset.isNaN()) {
                    return@offset IntOffset.Zero
                }

                val bottom_padding: Dp =
                    (getBottomBarHeight() + (bottom_inset * 1)) * (1f - expansion.get()).coerceIn(0f .. 1f)

                IntOffset(
                    0,
                    (-swipe_state.offset.npAnchorToDp(this, player.context, player.np_swipe_sensitivity) - bottom_padding).roundToPx()
                )
            }
            .then(swipe_modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (is_shut) {
                    coroutine_scope.launch {
                        expansion.scrollTo(if (swipe_state.targetValue == 0) 1 else 0)
                    }
                }
            }
            .playerOverscroll(swipe_state, swipe_interaction_source)
    ) {
        CompositionLocalProvider(LocalContentColor provides player.getNPOnBackground()) {
            Box {
                val page_heights: List<Dp> =
                    pages.mapIndexed { index, page ->
                        (
                            if (shouldShowBottomBarInPage(page)) page_height - getBottomBarHeight()
                            else page_height
                        )// - bottom_inset
                    }

                PlayerBackground(
                    page_heights.first() + 1.dp,
                    Modifier
                        .zIndex(1f)
                        .fillMaxWidth()
                        .requiredHeight(page_height)
                )

                MinimisedProgressBar(2.dp)

                Column(Modifier.zIndex(2f)) {
                    for ((index, page) in pages.withIndex()) {
                        val this_page_height: Dp = page_heights[index]

                        page.Page(
                            this_page_height,
                            top_bar,
                            PaddingValues(
                                top = WindowInsets.getTop(),
                                bottom = bottom_inset
                            ),
                            swipe_modifier,
                            Modifier
                                .fillMaxWidth()
                                .requiredHeight(this_page_height)
                                .offset {
                                    if (index == 0) {
                                        val bounded: Float = expansion.getBounded()
                                        IntOffset(
                                            0,
                                            if (bounded > 1f) (-page_height * ((pages.size / 2f) - bounded)).roundToPx()
                                            else 0
                                        )
                                    }
                                    else if (shouldShowBottomBarInPage(pages.first())) {
                                        IntOffset(
                                            0,
                                            getBottomBarHeight().roundToPx()
                                        )
                                    }
                                    else IntOffset.Zero
                                }
                        )
                    }
                }
            }
        }
    }
}
