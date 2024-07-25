package com.toasterofbread.spmp.model.state

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.playlist.Playlist
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceCompanion
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceLoadState
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.service.playercontroller.PlayerStatus

interface SessionState {
    val session_started: Boolean
    val controller: PlayerService?
    val service_connected: Boolean
    val service_load_state: PlayerServiceLoadState?
    val status: PlayerStatus

    fun isRunningAndFocused(): Boolean

    fun onStart()
    fun onStop()
    fun release()

    fun interactService(action: (state: PlayerService) -> Unit)
    suspend fun requestServiceChange(service_companion: PlayerServiceCompanion)

    fun withPlayer(action: PlayerServicePlayer.() -> Unit)
    @Composable
    fun withPlayerComposable(action: @Composable() (PlayerServicePlayer.() -> Unit))

    fun playMediaItem(item: MediaItem, shuffle: Boolean = false, at_index: Int = 0)
    fun playPlaylist(playlist: Playlist, from_index: Int = 0)
}
