package com.toasterofbread.spmp.model.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu

interface PlayerState {
    val np_theme_mode: ThemeMode
    val np_swipe_sensitivity: Float
    var np_bottom_bar_config: LayoutSlot.BelowPlayerConfig?
    var np_bottom_bar_showing: Boolean
    var np_bottom_bar_height: Dp
    var np_overlay_menu: PlayerOverlayMenu?
    val expansion: PlayerExpansionState

    var hide_player: Boolean
    fun isPlayerShowing(): Boolean

    fun switchNowPlayingPage(page: Int)
    fun navigateNpOverlayMenuBack()
    fun openNpOverlayMenu(menu: PlayerOverlayMenu?)

    fun getNowPlayingExpansionOffset(density: Density): Dp

    @Composable
    fun nowPlayingTopOffset(
        base: Modifier,
        section: NowPlayingTopOffsetSection,
        apply_spacing: Boolean,
        displaying: Boolean
    ): Modifier

    @Composable
    fun nowPlayingBottomPadding(
        include_np: Boolean,
        include_top_items: Boolean
    ): Dp
}
