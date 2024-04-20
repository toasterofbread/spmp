package com.toasterofbread.spmp.ui.layout.nowplaying.container

import LocalPlayerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.layout.onSizeChanged
import dev.toastbits.composekit.platform.composable.BackHandler
import dev.toastbits.composekit.platform.Platform
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.composekit.utils.common.blendWith
import dev.toastbits.composekit.utils.composable.getTop
import dev.toastbits.composekit.utils.composable.getBottom
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingPage.Companion.bottom_padding
import com.toasterofbread.spmp.ui.layout.nowplaying.container.npAnchorToDp
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
    val expansion: NowPlayingExpansionState = LocalNowPlayingExpansion.current
    val coroutine_scope: CoroutineScope = rememberCoroutineScope()
    val form_factor: FormFactor = NowPlayingPage.getFormFactor(player)

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
                    (-swipe_state.offset.npAnchorToDp(this, player.context) - bottom_padding).roundToPx()
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
            .playerBackground { page_height }
    ) {
        Box {
            val show_wave: Boolean by player.settings.theme.SHOW_EXPANDED_PLAYER_WAVE.observe()
            if (show_wave) {
                WaveBackground(
                    page_height,
                    Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f)
                        .thenIf(shouldShowBottomBarInPage(pages.first())) {
                            offset {
                                IntOffset(
                                    0,
                                    -getBottomBarHeight().roundToPx()
                                )
                            }
                        }
                )
            }

            if (form_factor == FormFactor.LANDSCAPE) {
                LandscapePlayerBackground(page_height)
            }

            MinimisedProgressBar(2.dp)

            Column(Modifier.zIndex(2f)) {
                for ((index, page) in pages.withIndex()) {
                    var this_page_height: Dp = (
                        if (shouldShowBottomBarInPage(page)) page_height - getBottomBarHeight()
                        else page_height
                    ) - bottom_inset

                    page.Page(
                        this_page_height,
                        top_bar,
                        PaddingValues(
                            top = WindowInsets.getTop(),
                            // bottom = bottom_inset
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
                                        if (bounded > 1f) (-(page_height) * ((pages.size / 2f) - bounded)).roundToPx()
                                        else 0
                                    )
                                }
                                else {
                                    var offset: Dp = bottom_inset
                                    if (shouldShowBottomBarInPage(pages.first())) {
                                        offset += getBottomBarHeight()
                                    }
                                    IntOffset(
                                        0,
                                        offset.roundToPx()
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}
