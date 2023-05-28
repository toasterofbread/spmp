package com.spectre7.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.modifier.background

const val MINIMISED_NOW_PLAYING_HEIGHT: Int = 64
const val MINIMISED_NOW_PLAYING_V_PADDING: Int = 10
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_SMALL: Float = 100f
const val MEDIAITEM_PREVIEW_SQUARE_SIZE_LARGE: Float = 200f

enum class OverlayPage { SEARCH, SETTINGS, MEDIAITEM, VIEW_MORE_URL, LIBRARY, RADIO_BUILDER, YTM_LOGIN, YTM_MANUAL_LOGIN }

@Composable
fun PlayerView(player: PlayerStateImpl) {
    player.LongPressMenu()

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

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
                                0 -> OverlayPage.SEARCH
                                1 -> OverlayPage.LIBRARY
                                else -> OverlayPage.SETTINGS
                            }
                        )
                        expand_state.value = false
                    }
                },
                if (!overlay_open) expand_state else null,
                Theme.current.accent_provider,
                container_modifier = player.nowPlayingTopOffset(Modifier)
            )

            player.HomeFeed()
        }

        player.NowPlaying()
    }
}
