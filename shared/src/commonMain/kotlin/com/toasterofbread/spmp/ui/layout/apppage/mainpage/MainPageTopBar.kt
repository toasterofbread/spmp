package com.toasterofbread.spmp.ui.layout.apppage.mainpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
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
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.toastercomposetools.platform.composable.platformClickable
import com.toasterofbread.toastercomposetools.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.layout.radiobuilder.RADIO_BUILDER_ICON_WIDTH_DP
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderIcon

@Composable
fun MainPageTopBar(content_padding: PaddingValues, modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current

    Column(modifier.padding(content_padding)) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Crossfade(player.app_page == player.app_page_state.Library) { library_open ->
                if (library_open) {
                    IconButton({ player.openAppPage(player.app_page_state.RadioBuilder) }) {
                        RadioBuilderIcon()
                    }
                }
                else {
                    val settings_page = player.app_page_state.Settings
                    IconButton({ player.openAppPage(settings_page) }) {
                        Icon(Icons.Default.Settings, null, Modifier.width(RADIO_BUILDER_ICON_WIDTH_DP.dp))
                    }
                }
            }

            player.top_bar.MusicTopBarWithVisualiser(
                Settings.INTERNAL_TOPBAR_MODE_HOME,
                Modifier.fillMaxSize().weight(1f),
                hide_while_inactive = false,
                can_show_visualiser = true
            )

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

        val multiselect_context = player.main_multiselect_context
        AnimatedVisibility(multiselect_context.is_active || player.app_page.showTopBarContent(), Modifier.animateContentSize()) {
            Crossfade(if (multiselect_context.is_active) null else player.app_page) { page ->
                if (page == null) {
                    multiselect_context.InfoDisplay(Modifier.fillMaxWidth())
                }
                else {
                    page.TopBarContent(Modifier.fillMaxWidth()) { player.navigateBack() }
                }
            }
        }

        WaveBorder(Modifier.requiredWidth(player.screen_size.width))
    }
}
