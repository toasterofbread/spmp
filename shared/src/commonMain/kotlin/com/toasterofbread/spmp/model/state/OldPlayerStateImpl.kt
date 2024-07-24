package com.toasterofbread.spmp.model.state

import ProgramArguments
import SpMp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.AppThemeManager
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.download.DownloadMethodSelectionDialog
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.playerservice.PlatformExternalPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlatformInternalPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceCompanion
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceLoadState
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.service.playercontroller.PlayerStatus
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenu
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuData
import com.toasterofbread.spmp.ui.component.multiselect.AppPageMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectInfoDisplayContent
import com.toasterofbread.spmp.ui.component.multiselect.MultiSelectItem
import com.toasterofbread.spmp.ui.layout.BarColourState
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.AppPageWithItem
import com.toasterofbread.spmp.ui.layout.apppage.SongAppPage
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MINIMISED_NOW_PLAYING_HEIGHT_DP
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.MainPageDisplay
import com.toasterofbread.spmp.ui.layout.artistpage.ArtistAppPage
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingTopOffsetSection
import com.toasterofbread.spmp.ui.layout.nowplaying.PlayerExpansionState
import com.toasterofbread.spmp.ui.layout.nowplaying.ThemeMode
import com.toasterofbread.spmp.ui.layout.nowplaying.container.npAnchorToDp
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.layout.playlistpage.PlaylistAppPage
import dev.toastbits.composekit.settings.ui.on_accent
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import dev.toastbits.composekit.utils.composable.getEnd
import dev.toastbits.composekit.utils.composable.getStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias DownloadRequestCallback = (DownloadStatus?) -> Unit

enum class FeedLoadState { PREINIT, NONE, LOADING, CONTINUING }

// This is an atrocity
class OldPlayerStateImpl(
    override val context: AppContext,
    override val coroutine_scope: CoroutineScope,
    initial_theme_mode: ThemeMode,
    initial_swipe_sensitivity: Float
) : OldPlayerState {
    override val database: Database get() = context.database
    override val settings: Settings get() = context.settings

    private var download_request_songs: List<Song>? by mutableStateOf(null)
    private var download_request_always_show_options: Boolean by mutableStateOf(false)
    private var download_request_callback: DownloadRequestCallback? by mutableStateOf(null)

    @Composable
    override fun withPlayerComposable(action: @Composable PlayerServicePlayer.() -> Unit) {
        LaunchedEffect(Unit) {
            connectService(onConnected = null)
        }

        _player?.service_player?.also {
            action(it)
        }
    }



    override suspend fun requestServiceChange(service_companion: PlayerServiceCompanion) = withContext(Dispatchers.Default) {
        synchronized(service_connected_listeners) {
            service_connection?.also { connection ->
                service_connection_companion!!.disconnect(context, connection)
                service_connection_companion = null
                service_connection = null

                _player?.also {
                    launch(Dispatchers.Main) {
                        it.onDestroy()
                    }
                    _player = null
                }
            }

            service_connecting = false
        }

        connectService(service_companion, onConnected = null)
    }

    override fun isRunningAndFocused(): Boolean {
        return controller?.has_focus == true
    }

    override fun onSongDownloadRequested(song: Song, always_show_options: Boolean, onCompleted: DownloadRequestCallback?) {
        onSongDownloadRequested(listOf(song), always_show_options, onCompleted)
    }
    override fun onSongDownloadRequested(songs: List<Song>, always_show_options: Boolean, callback: DownloadRequestCallback?) {
        download_request_songs = songs
        download_request_always_show_options = always_show_options
        download_request_callback = callback
    }
}

fun Dp.notUnspecified(): Dp =
    if (this.isUnspecified) 0.dp else this
