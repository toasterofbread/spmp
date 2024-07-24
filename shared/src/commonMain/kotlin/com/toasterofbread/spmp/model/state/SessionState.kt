package com.toasterofbread.spmp.model.state

import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceLoadState
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.service.playercontroller.PlayerStatus

interface SessionState {
    val session_started: Boolean
    val controller: PlayerService?
    val service_connected: Boolean
    val service_load_state: PlayerServiceLoadState?
    val status: PlayerStatus

    fun withPlayer(action: PlayerServicePlayer.() -> Unit)
    fun interactService(action: (player: PlayerService) -> Unit)

    fun onStart()
    fun onStop()
    fun release()
}
