package com.toasterofbread.spmp.ui.layout.nowplaying

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.components.utils.composable.getBottom
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.nowplaying.container.NowPlayingContainer
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.maintab.NowPlayingMainTabPage
import com.toasterofbread.spmp.ui.layout.nowplaying.queue.NowPlayingQueuePage
import kotlin.math.*
import LocalNowPlayingExpansion

const val EXPANDED_THRESHOLD = 0.1f
const val POSITION_UPDATE_INTERVAL_MS: Long = 100

@Composable
fun NowPlaying(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current
    val density: Density = LocalDensity.current
    val form_factor: FormFactor by NowPlayingPage.observeFormFactor()
    val pages: List<NowPlayingPage> = remember(form_factor) { NowPlayingPage.ALL.filter { it.shouldShow(player, form_factor) } }

    val bottom_layout_slot: LayoutSlot =
        when (form_factor) {
            FormFactor.LANDSCAPE -> LandscapeLayoutSlot.BELOW_PLAYER
            FormFactor.PORTRAIT -> PortraitLayoutSlot.BELOW_PLAYER
        }

    player.np_bottom_bar_config = bottom_layout_slot.observeConfig { LayoutSlot.BelowPlayerConfig() }

    val show_bottom_slot_in_player: Boolean =
        player.np_bottom_bar_config?.show_in_player == true || bottom_layout_slot.mustShow()
    val show_bottom_slot_in_queue: Boolean =
        player.np_bottom_bar_config?.show_in_queue == true || bottom_layout_slot.mustShow()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier.requiredHeight(maxHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                player.player_showing,
                modifier,
                exit = slideOutVertically() { it },
                enter = slideInVertically() { it }
            ) {
                NowPlayingContainer(
                    pages,
                    shouldShowBottomBarInPage = { page ->
                        when (page) {
                            is NowPlayingMainTabPage -> show_bottom_slot_in_player
                            is NowPlayingQueuePage -> show_bottom_slot_in_queue
                            else -> false
                        }
                    },
                    getBottomBarHeight = { player.np_bottom_bar_height }
                )
            }

            val bottom_inset: Dp = WindowInsets.getBottom(player.np_overlay_menu == null)

            player.np_bottom_bar_showing = bottom_layout_slot.DisplayBar(
                0.dp,
                container_modifier =
                    Modifier
                        .onSizeChanged {
                            player.np_bottom_bar_height = with (density) {
                                it.height.toDp() - bottom_inset
                            }
                        },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        val bounded: Float = player.expansion.getBounded()
                        val slot_expansion: Float =
                            if (!show_bottom_slot_in_player) {
                                if (show_bottom_slot_in_queue) (bounded - 1f).absoluteValue.coerceIn(0f..1f)
                                else (1f - bounded).coerceAtLeast(0f)
                            }
                            else if (!show_bottom_slot_in_queue) (2f - bounded).coerceIn(0f..1f)
                            else 1f

                        IntOffset(
                            x = 0,
                            y = with (density) {
                                ((player.np_bottom_bar_height + bottom_inset).toPx() * (1f - slot_expansion)).roundToInt()
                            }
                        )
                    },
                content_padding = PaddingValues(
                    bottom = bottom_inset
                ),
                getParentBackgroundColour = {
                    player.getNPBackground()
                },
                getBackgroundColour = { background_colour ->
                    if (background_colour.alpha >= 0.5f) {
                        return@DisplayBar background_colour
                    }

                    val bounded: Float = expansion.getBounded()
                    val min_background_alpha: Float =
                        if (bounded > 1f) bounded - 1f
                        else 0f

                    return@DisplayBar player.getNPBackground().blendWith(background_colour, ourRatio = min_background_alpha)
                }
            )

            val bottom_bar_colour: ColourSource by bottom_layout_slot.rememberColourSource()
            LaunchedEffect(player.np_bottom_bar_showing) {
                player.bar_colour_state.nav_bar.setLevelColour(
                    if (player.np_bottom_bar_showing) bottom_bar_colour
                    else null,
                    BarColourState.NavBarLevel.BAR
                )
            }
        }
    }
}

enum class ThemeMode {
    BACKGROUND, ELEMENTS, NONE;

    companion object {
        val DEFAULT: ThemeMode =
            when (Platform.current) {
                Platform.ANDROID -> BACKGROUND
                Platform.DESKTOP -> BACKGROUND
                Platform.WEB -> BACKGROUND
            }
    }
}

private fun PlayerState.getBackgroundColourOverride(): Color {
    val form_factor: FormFactor = FormFactor.getCurrent(this)
    val pages: List<NowPlayingPage> = NowPlayingPage.ALL.filter { it.shouldShow(this, form_factor) }

    var current: Color? = pages.getOrNull(expansion.swipe_state.currentValue - 1)?.getPlayerBackgroundColourOverride(this)
    var target: Color? = pages.getOrNull(expansion.swipe_state.targetValue - 1)?.getPlayerBackgroundColourOverride(this)

    val default: Color =
        when (np_theme_mode) {
            ThemeMode.BACKGROUND -> theme.accent
            ThemeMode.ELEMENTS -> theme.card
            ThemeMode.NONE -> theme.card
        }

    if (current == null && target == null) {
        return default
    }

    if (current == null) {
        current = default
    }
    else if (target == null) {
        target = default
    }

    return target!!.blendWith(current, if (expansion.swipe_state.lastVelocity < 0 ) 1f - expansion.swipe_state.progress else expansion.swipe_state.progress)
}

private var derived_np_background: State<Color>? = null
private var derived_np_background_player: PlayerState? = null

fun PlayerState.getNPBackground(): Color {
    if (derived_np_background == null || derived_np_background_player != this) {
        derived_np_background = derivedStateOf { getBackgroundColourOverride() }
        derived_np_background_player = this
    }
    return derived_np_background!!.value
}

internal fun PlayerState.getNPOnBackground(): Color {
    return getBackgroundColourOverride().getContrasted()
//    val override: Color? = getBackgroundColourOverride()?.getPlayerBackgroundColourOverride(this)
//    if (override != null) {
//        return override.getContrasted()
//    }
//
//    return when (np_theme_mode) {
//        ThemeMode.BACKGROUND -> theme.onAccent
//        ThemeMode.ELEMENTS -> theme.accent
//        ThemeMode.NONE -> theme.onBackground
//    }
}

internal fun PlayerState.getNPAltBackground(theme_mode: ThemeMode = np_theme_mode): Color {
    return when (theme_mode) {
        ThemeMode.BACKGROUND -> getNPBackground().amplifyPercent(-0.4f, oppositePercent = -0.2f)
        else -> theme.background
    }
}

internal fun PlayerState.getNPAltOnBackground(): Color =
    getNPBackground().amplifyPercent(-0.4f, oppositePercent = -0.1f)
