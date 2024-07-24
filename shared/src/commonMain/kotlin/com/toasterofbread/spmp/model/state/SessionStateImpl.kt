package com.toasterofbread.spmp.model.state

import ProgramArguments
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.platform.playerservice.PlatformExternalPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlatformInternalPlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceCompanion
import com.toasterofbread.spmp.platform.playerservice.PlayerServiceLoadState
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.service.playercontroller.PlayerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SessionStateImpl(
    private val context: AppContext,
    private val coroutine_scope: CoroutineScope,
    val launch_arguments: ProgramArguments,
    private val low_memory_listener: () -> Unit = {}
): SessionState {
    private var _player: PlayerService? by mutableStateOf(null)

    override val controller: PlayerService? get() = _player
    override val session_started: Boolean get() = _player?.service_player?.session_started == true

    override val service_connected: Boolean get() = _player?.load_state?.let { !it.loading && it.error == null } ?: false
    override val service_load_state: PlayerServiceLoadState? get() = _player?.load_state

    override val status: PlayerStatus = PlayerStatus()

    override fun withPlayer(action: PlayerServicePlayer.() -> Unit) {
        _player?.also {
            action(it.service_player)
            return
        }

        coroutine_scope.launch {
            connectService {
                action(it.service_player)
            }
        }
    }

    override fun interactService(action: (player: PlayerService) -> Unit) {
        synchronized(service_connected_listeners) {
            _player?.also {
                action(it)
                return
            }

            service_connected_listeners.add {
                action(_player!!)
            }
        }
    }

    override fun onStart() {
        SpMp.addLowMemoryListener(low_memory_listener)

        coroutine_scope.launch {
            if (getServiceCompanion().isServiceRunning(context)) {
                connectService()
            }
            else {
                coroutine_scope.launch {
                    if (PersistentQueueHandler.isPopulatedQueueSaved(context)) {
                        connectService()
                    }
                }
            }
        }
    }

    override fun onStop() {
        SpMp.removeLowMemoryListener(low_memory_listener)
    }

    override fun release() {
        service_connection?.also {
            service_connection_companion?.disconnect(context, it)
        }
        service_connection = null
        service_connection_companion = null
        _player = null
    }

    private var service_connecting: Boolean = false
    private var service_connected_listeners: MutableList<(PlayerService) -> Unit> = mutableListOf()
    private var service_connection: Any? = null
    private var service_connection_companion: PlayerServiceCompanion? = null

    private fun getServiceCompanion(): PlayerServiceCompanion =
        if (!PlatformInternalPlayerService.isServiceAttached(context) && (!PlatformInternalPlayerService.isAvailable(context, launch_arguments) || context.settings.platform.ENABLE_EXTERNAL_SERVER_MODE.get()))
            PlatformExternalPlayerService
        else PlatformInternalPlayerService

    private suspend fun connectService(
        service_companion_override: PlayerServiceCompanion? = null,
        onConnected: ((PlayerService) -> Unit)? = null
    ) {
        val service_companion: PlayerServiceCompanion = service_companion_override ?: getServiceCompanion()

        synchronized(service_connected_listeners) {
            if (service_connecting) {
                if (onConnected != null) {
                    service_connected_listeners.add(onConnected)
                }
                return
            }

            _player?.also { service ->
                onConnected?.invoke(service)
                return
            }

            service_connection_companion = service_companion

            service_connecting = true
            service_connection = service_companion.connect(
                context,
                launch_arguments,
                _player,
                { service ->
                    synchronized(service_connected_listeners) {
                        _player = service
                        status.setPlayer(service)
                        service_connecting = false

                        onConnected?.invoke(service)
                        for (listener in service_connected_listeners) {
                            listener.invoke(service)
                        }
                        service_connected_listeners.clear()
                    }
                },
                {
                    service_connecting = false
                }
            )
        }
    }
}
