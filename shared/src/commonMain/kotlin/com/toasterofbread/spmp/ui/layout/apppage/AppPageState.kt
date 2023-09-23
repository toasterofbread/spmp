package com.toasterofbread.spmp.ui.layout.apppage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.layout.apppage.library.LibraryAppPage
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.SettingsAppPage
import com.toasterofbread.spmp.ui.layout.apppage.songfeedpage.SongFeedAppPage

class AppPageState(val context: PlatformContext) {
    val SongFeed = SongFeedAppPage(this)
    val Library = LibraryAppPage(this)
    val Search = SearchAppPage(this, context)
    val Settings by lazy { SettingsAppPage(this) }
    val RadioBuilder = RadioBuilderAppPage(this)

    val Default: AppPage = SongFeed

    var current_page by mutableStateOf(Default)

    fun setPage(page: AppPage? = null, from_current: Boolean, going_back: Boolean): Boolean {
        val new_page = page ?: Default
        if (new_page != current_page) {
            val old_page = current_page
            current_page = new_page
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
