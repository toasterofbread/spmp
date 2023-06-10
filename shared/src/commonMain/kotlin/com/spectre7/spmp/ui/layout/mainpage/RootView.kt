package com.spectre7.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.modifier.background

const val MINIMISED_NOW_PLAYING_HEIGHT: Int = 64
const val MINIMISED_NOW_PLAYING_V_PADDING: Int = 7
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL: Float = 100f
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE: Float = 200f

@Composable
fun RootView(player: PlayerStateImpl) {
    player.LongPressMenu()

    LaunchedEffect(player.overlay_page) {
        if (player.overlay_page == null) {
            player.pill_menu.showing = true
            player.pill_menu.top = false
            player.pill_menu.left = false
            player.pill_menu.clearExtraActions()
            player.pill_menu.clearAlongsideActions()
            player.pill_menu.clearActionOverriders()
            player.pill_menu.setBackgroundColourOverride(null)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.current.background_provider)
    ) {
        Box {
            val expand_state = remember { mutableStateOf(false) }
            val overlay_open by remember { derivedStateOf { player.overlay_page != null } }

            player.pill_menu.PillMenu(
                if (overlay_open) 1 else 3,
                { index, action_count ->
                    ActionButton(
                        if (action_count == 1) Icons.Default.Close else
                            when (index) {
                                0 -> Icons.Default.Search
                                1 -> Icons.Default.MusicNote
                                else -> Icons.Default.Settings
                            }
                    ) {
                        player.setOverlayPage(if (action_count == 1) null else
                            when (index) {
                                0 -> PlayerOverlayPage.SearchPage
                                1 -> PlayerOverlayPage.LibraryPage
                                else -> PlayerOverlayPage.SettingsPage
                            }
                        )
                        expand_state.value = false
                    }
                },
                if (!overlay_open) expand_state else null,
                Theme.current.accent_provider,
                container_modifier = player.nowPlayingTopOffset(Modifier)
            )

            player.HomePage()
        }

        player.NowPlaying()
    }
}
