package com.toasterofbread.spmp.model.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceCompanion
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import kotlinx.coroutines.CoroutineScope

interface OldPlayerState {
    val context: AppContext
    val coroutine_scope: CoroutineScope
    val database: Database
    val settings: Settings

    @Composable
    fun withPlayerComposable(action: @Composable() (PlayerServicePlayer.() -> Unit))

    suspend fun requestServiceChange(service_companion: PlayerServiceCompanion)
    fun isRunningAndFocused(): Boolean
    fun onSongDownloadRequested(
        song: Song,
        always_show_options: Boolean = false,
        onCompleted: DownloadRequestCallback? = null
    )

    fun onSongDownloadRequested(
        songs: List<Song>,
        always_show_options: Boolean = false,
        callback: DownloadRequestCallback? = null
    )
}
