package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.toasterofbread.composesettings.ui.SettingsInterface
import com.toasterofbread.composesettings.ui.item.SettingsValueState
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.library.PlaylistsPage
import com.toasterofbread.spmp.ui.layout.library.SongsPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPage
import com.toasterofbread.spmp.ui.layout.prefspage.PrefsPageCategory
import com.toasterofbread.spmp.ui.layout.prefspage.getPrefsPageSettingsInterface
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderPage

interface PlayerOverlayPage {
    @Composable
    fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit)

    fun getItem(): MediaItem? = null

    companion object {
        fun getViewMorePage(browse_id: String, title: String?): PlayerOverlayPage = when (browse_id) {
            "FEmusic_listen_again", "FEmusic_mixed_for_you", "FEmusic_new_releases_albums" -> GenericFeedViewMorePage(browse_id, title)
            "FEmusic_moods_and_genres" -> TODO(browse_id)
            "FEmusic_charts" -> TODO(browse_id)
            else -> throw NotImplementedError(browse_id)
        }

        val RadioBuilderPage = object : PlayerOverlayPage {
            @Composable
            override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                RadioBuilderPage(
                    bottom_padding,
                    Modifier.fillMaxSize(),
                    close
                )
            }
        }

        val SettingsPage = object : PlayerOverlayPage {
            val current_category: MutableState<PrefsPageCategory?> = mutableStateOf(null)
            val pill_menu: PillMenu = PillMenu(follow_player = true)
            val ytm_auth: SettingsValueState<Set<String>> =
                SettingsValueState<Set<String>>(
                    Settings.KEY_YTM_AUTH.name
                ).init(Settings.prefs, Settings.Companion::provideDefault)
            val settings_interface: SettingsInterface =
                getPrefsPageSettingsInterface(pill_menu, ytm_auth, { current_category.value }, { current_category.value = null })

            @Composable
            override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
                PrefsPage(bottom_padding, current_category, pill_menu, settings_interface, ytm_auth, Modifier.fillMaxSize(), close)
            }
        }

        val PlaylistsPage = PlaylistsPage()
        val SongsPage = SongsPage()
    }
}
