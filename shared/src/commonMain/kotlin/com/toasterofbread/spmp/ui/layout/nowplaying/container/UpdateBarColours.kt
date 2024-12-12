package com.toasterofbread.spmp.ui.layout.nowplaying.container

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.getNPBackground
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.CustomColourSource
import dev.toastbits.composekit.components.utils.composable.getTop
import LocalPlayerState
import LocalNowPlayingExpansion

@Composable
internal fun UpdateBarColours(page_height: Dp) {
    val player: PlayerState = LocalPlayerState.current
    val expansion: PlayerExpansionState = LocalNowPlayingExpansion.current
    val background_colour: Color = player.getNPBackground()
    val status_bar_height: Dp = WindowInsets.statusBars.getTop()

    val status_bar_height_percent: Float =
        (status_bar_height.value * (if (player.context.isDisplayingAboveNavigationBar()) 1f else 0.75f)) / page_height.value

    val under_status_bar: Boolean by remember(status_bar_height) { derivedStateOf {
        1f - expansion.get() < status_bar_height_percent
    } }

    DisposableEffect(under_status_bar, background_colour) {
        player.bar_colour_state.status_bar.setLevelColour(
            if (under_status_bar) CustomColourSource(background_colour) else null,
            BarColourState.StatusBarLevel.PLAYER
        )

        onDispose {
            player.bar_colour_state.status_bar.setLevelColour(null, BarColourState.StatusBarLevel.PLAYER)
        }
    }

    DisposableEffect(background_colour) {
        player.bar_colour_state.nav_bar.setLevelColour(CustomColourSource(background_colour), BarColourState.NavBarLevel.PLAYER)

        onDispose {
            player.bar_colour_state.nav_bar.setLevelColour(null, BarColourState.NavBarLevel.PLAYER)
        }
    }
}
