package com.spectre7.spmp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.spectre7.spmp.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

enum class SERVICE_INTENT_ACTIONS { STOP, BUTTON_VOLUME }

fun ExoMediaItem.LocalConfiguration.getSong(): Song? {
    return when (val tag = tag) {
        is IndexedValue<*> -> tag.value as Song?
        is Song? -> tag
        else -> throw IllegalStateException()
    }
}

class PlayerServiceHost {

    private lateinit var service: PlayerService
    private var service_connected by mutableStateOf(false)

    private var service_intent: Intent? = null
    private var service_connection: ServiceConnection? = null

    private val context: Context get() = MainActivity.context

    init {
        assert(instance == null)
        instance = this
    }

    class PlayerStatus internal constructor(service: PlayerService) {
        private var player: ExoPlayer

        val playing: Boolean get() = player.isPlaying
        val position: Float get() = player.currentPosition.toFloat() / player.duration.toFloat()
        val position_seconds: Float get() = player.currentPosition / 1000f
        val duration: Float get() = player.duration / 1000f
        val song: Song? get() = player.currentMediaItem?.localConfiguration?.getSong()
        val index: Int get() = player.currentMediaItemIndex
        val shuffle: Boolean get() = player.shuffleModeEnabled
        val repeat_mode: Int get() = player.repeatMode
        val has_next: Boolean get() = player.hasNextMediaItem()
        val has_previous: Boolean get() = player.hasPreviousMediaItem()

        val m_queue = mutableStateListOf<Song>()
        var m_playing: Boolean by mutableStateOf(false)
        var m_position: Float by mutableStateOf(0f)
        var m_duration: Float by mutableStateOf(0f)
        var m_song: Song? by mutableStateOf(null)
        var m_index: Int by mutableStateOf(0)
        var m_shuffle: Boolean by mutableStateOf(false)
        var m_repeat_mode: Int by mutableStateOf(0)
        var m_has_next: Boolean by mutableStateOf(false)
        var m_has_previous: Boolean by mutableStateOf(false)

        init {
            player = service.player
            service.addQueueListener(object : PlayerQueueListener {
                override fun onSongAdded(song: Song, index: Int) {
                    m_queue.add(index, song)
                }
                override fun onSongRemoved(song: Song, index: Int) {
                    m_queue.removeAt(index)
                }
                override fun onSongMoved(from: Int, to: Int) {
                    m_queue.add(to, m_queue.removeAt(from))
                }
                override fun onCleared() {
                    m_queue.clear()
                }
            })

            player.addListener(object : Player.Listener {
                override fun onMediaItemTransition(
                    media_item: MediaItem?,
                    reason: Int
                ) {
                    m_song = media_item?.localConfiguration?.getSong()
                }

                override fun onIsPlayingChanged(is_playing: Boolean) {
                    m_playing = is_playing
                }

                override fun onShuffleModeEnabledChanged(shuffle_enabled: Boolean) {
                    m_shuffle = shuffle_enabled
                }

                override fun onRepeatModeChanged(repeat_mode: Int) {
                    m_repeat_mode = repeat_mode
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    m_has_previous = player.hasPreviousMediaItem()
                    m_has_next = player.hasNextMediaItem()
                    m_duration = duration
                    m_index = player.currentMediaItemIndex

                    if (player.currentMediaItemIndex > service.active_queue_index) {
                        service.active_queue_index = player.currentMediaItemIndex
                    }
                }
            })

            service.iterateSongs { _, song ->
                m_queue.add(song)
            }

            thread {
                runBlocking {
                    while (true) {
                        delay(100)
                        MainActivity.runInMainThread {
                            m_position = position
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var instance: PlayerServiceHost? = null
        lateinit var status: PlayerStatus

        val service: PlayerService get() = instance!!.service
        val player: ExoPlayer get() = service.player
        val service_connected: Boolean get() = instance?.service_connected ?: false
        val session_started: Boolean get() = instance?.service?.session_started ?: false

        fun isRunningAndFocused(): Boolean {
            if (instance == null) {
                return false
            }

            if (player.audioFocusState != Player.AUDIO_FOCUS_STATE_HAVE_FOCUS) {
                return false
            }

            return true
        }

        fun release() {
            instance?.release()
        }
    }

    private fun release() {
        if (service_connection != null) {
            context.unbindService(service_connection!!)
            service_connection = null
        }
    }

    fun startService(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        if (service_connected) {
            println("ALREADY CONNECTED")
            return
        }

        if (service_intent == null) {
            service_intent = Intent(context, PlayerService::class.java)
            service_connection = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                    service = (binder as PlayerService.PlayerBinder).getService()
                    status = PlayerStatus(service)
                    service_connected = true
                    onConnected?.invoke()
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    service_connected = false
                    onDisconnected?.invoke()
                }
            }
        }

        context.startService(service_intent)
        context.bindService(service_intent, service_connection!!, 0)
    }

    interface PlayerQueueListener {
        fun onSongAdded(song: Song, index: Int)
        fun onSongRemoved(song: Song, index: Int)
        fun onSongMoved(from: Int, to: Int)
        fun onCleared()
    }
}
