package com.toasterofbread.spmp.ui.layout.mainpage

import LocalPlayerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsInterface
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.platform.getNavigationBarHeightDp
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPageCategory
import com.toasterofbread.spmp.ui.layout.prefspage.getPrefsPageSettingsInterface
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderPage

interface OverlayPage {
    @Composable
    fun Page(previous_item: MediaItem?, close: () -> Unit)

    @Composable
    fun getContentPadding(): PaddingValues {
        val player = LocalPlayerState.current

        val bottom_padding = with(LocalDensity.current) {
            (
                if (player.session_started) MINIMISED_NOW_PLAYING_HEIGHT_DP.dp
                else 0.dp
            ) + player.context.getNavigationBarHeightDp() + player.getDefaultVerticalPadding() + (player.context.getImeInsets()?.getBottom(this@with)?.toDp() ?: 0.dp)
        }

        val horizontal_padding = player.getDefaultHorizontalPadding()
        val vertical_padding = player.getDefaultVerticalPadding()

        return PaddingValues(
            top = vertical_padding + player.context.getStatusBarHeightDp(),
            bottom = vertical_padding + bottom_padding,
            start = horizontal_padding,
            end = horizontal_padding
        )
    }

    fun getItem(): MediaItem? = null

    companion object {
        fun getViewMorePage(browse_id: String, title: String?): OverlayPage = when (browse_id) {
            "FEmusic_listen_again", "FEmusic_mixed_for_you", "FEmusic_new_releases_albums" -> GenericFeedViewMorePage(browse_id, title)
            "FEmusic_moods_and_genres" -> TODO(browse_id)
            "FEmusic_charts" -> TODO(browse_id)
            else -> throw NotImplementedError(browse_id)
        }

        val RadioBuilderPage = object : OverlayPage {
            @Composable
            override fun Page(previous_item: MediaItem?, close: () -> Unit) {
                RadioBuilderPage(
                    getContentPadding(),
                    Modifier.fillMaxSize(),
                    close
                )
            }
        }

        val SettingsPage = object : OverlayPage {
            val current_category: MutableState<PrefsPageCategory?> = mutableStateOf(null)
            val pill_menu: PillMenu = PillMenu(follow_player = true)
            val ytm_auth: SettingsValueState<Set<String>> =
                SettingsValueState<Set<String>>(
                    Settings.KEY_YTM_AUTH.name
                ).init(Settings.prefs, Settings.Companion::provideDefault)
            val settings_interface: SettingsInterface =
                getPrefsPageSettingsInterface(pill_menu, ytm_auth, { current_category.value }, { current_category.value = null })

            @Composable
            override fun Page(previous_item: MediaItem?, close: () -> Unit) {
                PrefsPage(getContentPadding(), current_category, pill_menu, settings_interface, ytm_auth, Modifier.fillMaxSize(), close)
            }
        }
    }
}
