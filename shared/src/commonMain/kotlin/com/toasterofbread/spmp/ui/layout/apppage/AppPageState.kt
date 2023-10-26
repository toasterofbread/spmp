package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.library.LibraryAppPage
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import com.toasterofbread.spmp.ui.layout.apppage.songfeedpage.SongFeedAppPage

class AppPageState(val player: PlayerState) {
    val SongFeed = SongFeedAppPage(this)
    val Library = LibraryAppPage(this)
    val Search = SearchAppPage(this, context)
    val RadioBuilder = RadioBuilderAppPage(this)

    private var settings: SettingsAppPage? = null
    val Settings: SettingsAppPage
        @Composable get() {
            if (settings == null) {
                settings = SettingsAppPage(this, LocalPlayerState.current.nowPlayingTopOffset(Modifier))
            }
            return settings!!
        }

    val Default: AppPage = SongFeed
    val context: AppContext get() = player.context

    var current_page by mutableStateOf(Default)

    fun setPage(page: AppPage? = null, from_current: Boolean, going_back: Boolean): Boolean {
        val new_page = page ?: Default
        if (new_page != current_page) {
            val old_page = current_page
            current_page = new_page

            old_page.onClosed(new_page)

            if (!going_back) {
                new_page.onOpened(
                    if (from_current) (old_page as? AppPageWithItem)?.item
                    else null
                )
            }
            return true
        }
        return false
    }

    fun getViewMorePage(browse_id: String, title: String?): AppPage = when (browse_id) {
        "FEmusic_listen_again", "FEmusic_mixed_for_you", "FEmusic_new_releases_albums" -> GenericFeedViewMoreAppPage(this, browse_id, title)
        "FEmusic_moods_and_genres" -> TODO(browse_id)
        "FEmusic_charts" -> TODO(browse_id)
        else -> throw NotImplementedError(browse_id)
    }
}
