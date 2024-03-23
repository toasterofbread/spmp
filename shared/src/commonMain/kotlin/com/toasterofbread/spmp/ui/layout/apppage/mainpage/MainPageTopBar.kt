package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.spmp.model.settings.category.InternalSettings
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import com.toasterofbread.spmp.ui.layout.radiobuilder.RADIO_BUILDER_ICON_WIDTH_DP
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderIcon
import com.toasterofbread.spmp.ui.layout.contentbar.DisplayBar
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.PortraitLayoutSlot

@Composable
fun MainPageTopBar(content_padding: PaddingValues, modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current

    Column(modifier.padding(content_padding)) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Crossfade(player.app_page == player.app_page_state.Library) { library_open ->
                if (library_open) {
                    IconButton({ player.openAppPage(player.app_page_state.RadioBuilder) }) {
                        RadioBuilderIcon()
                    }
                }
                else {
                    val settings_page: SettingsAppPage = player.app_page_state.Settings
                    IconButton({ player.openAppPage(settings_page) }) {
                        Icon(Icons.Default.Settings, null, Modifier.width(RADIO_BUILDER_ICON_WIDTH_DP.dp))
                    }
                }
            }

            // player.top_bar.MusicTopBarWithVisualiser(
            //     InternalSettings.Key.TOPBAR_MODE_HOME,
            //     Modifier.fillMaxSize().weight(1f),
            //     hide_while_inactive = false,
            //     can_show_visualiser = true
            // )

            Crossfade(player.app_page == player.app_page_state.Library) { library_open ->
                if (library_open) {
                    IconButton({ player.openAppPage(player.app_page_state.Default) }) {
                        Icon(Icons.Default.List, null)
                    }
                }
                else {
                    IconButton({}) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            Modifier.platformClickable(
                                onClick = { player.openAppPage(player.app_page_state.Library) },
                                onAltClick = {
                                    player.openAppPage(player.app_page_state.RadioBuilder)
                                    player.context.vibrateShort()
                                },
                                indication = rememberRipple(false)
                            )
                        )
                    }
                }
            }
        }

        player.main_multiselect_context.InfoDisplay(
            Modifier.fillMaxWidth(),
            show_alt_content = player.app_page.showTopBarContent(),
            altContent = {
                PortraitLayoutSlot.LOWER_TOP_BAR.DisplayBar(0.dp, Modifier.fillMaxWidth())
            }
        )

        WaveBorder(Modifier.requiredWidth(player.screen_size.width))
    }
}
