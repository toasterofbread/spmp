package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.platform.vibrateShort
import com.toasterofbread.spmp.ui.component.MusicTopBarWithVisualiser
import com.toasterofbread.spmp.ui.component.WaveBorder
import com.toasterofbread.spmp.ui.layout.YoutubeMusicLoginConfirmation
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderIcon

@Composable
fun MainPageTopBar(modifier: Modifier = Modifier) {
    val player = LocalPlayerState.current

    Column(modifier) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            RadioBuilderButton()

            MusicTopBarWithVisualiser(
                Settings.INTERNAL_TOPBAR_MODE_HOME,
                Modifier.fillMaxSize().weight(1f),
                hide_while_inactive = false,
                can_show_visualiser = true
            )

            var show_login_confirmation by remember { mutableStateOf(false) }
            if (show_login_confirmation) {
                YoutubeMusicLoginConfirmation { manual ->
                    show_login_confirmation = false
                    if (manual == true) player.setOverlayPage(PlayerOverlayPage.YtmLoginPage(true))
                    else if (manual == false) player.setOverlayPage(PlayerOverlayPage.YtmLoginPage())
                }
            }

            Crossfade(player.main_page == player.main_page_state.Library) { library_open ->
                if (library_open) {
                    IconButton({ player.navigateBack() }) {
                        Icon(Icons.Default.List, null)
                    }
                }
                else {
                    IconButton({}) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            Modifier.platformClickable(
                                onClick = { player.setMainPage(player.main_page_state.Library) },
                                onAltClick = {
                                    player.setOverlayPage(PlayerOverlayPage.SettingsPage)
                                    SpMp.context.vibrateShort()
                                },
                                indication = rememberRipple(false)
                            )
                        )
                    }
                }
            }
        }

        val multiselect_context = player.main_multiselect_context
        AnimatedVisibility(multiselect_context.is_active || player.main_page.showTopBarContent(), Modifier.animateContentSize()) {
            Crossfade(if (multiselect_context.is_active) null else player.main_page) { page ->
                if (page == null) {
                    multiselect_context.InfoDisplay(Modifier.fillMaxWidth())
                }
                else {
                    page.TopBarContent(Modifier.fillMaxWidth()) { player.navigateBack() }
                }
            }
        }

        WaveBorder(Modifier.requiredWidth(SpMp.context.getScreenWidth()))
    }
}

@Composable
private fun RadioBuilderButton() {
    val player = LocalPlayerState.current
    IconButton({ player.setOverlayPage(PlayerOverlayPage.RadioBuilderPage) }) {
        RadioBuilderIcon()
    }
}
