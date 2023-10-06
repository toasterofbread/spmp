package com.toasterofbread.spmp.platform.playerservice

import SpMp
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.db.incrementPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.MediaPlayerState
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.service.playercontroller.DiscordStatusHandler
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask

private const val UPDATE_INTERVAL: Long = 5000 // ms
//private const val VOL_NOTIF_SHOW_DURATION: Long = 1000
private const val SONG_MARK_WATCHED_POSITION = 1000 // ms

data class PlayerServiceState(
    val stop_after_current_song: Boolean = false,
    val radio_state: RadioInstance.RadioState = RadioInstance.RadioState()
) {
    companion object
}

expect abstract class PlatformPlayerService() {
    val context: PlatformContext
    val service_player: PlayerServicePlayer

    open fun onCreate()
    open fun onDestroy()

    open class Listener() {
        open fun onSongTransition(song: Song?, manual: Boolean)
        open fun onStateChanged(state: MediaPlayerState)
        open fun onPlayingChanged(is_playing: Boolean)
        open fun onRepeatModeChanged(repeat_mode: MediaPlayerRepeatMode)
        open fun onVolumeChanged(volume: Float)
        open fun onDurationChanged(duration_ms: Long)
        open fun onSeeked(position_ms: Long)
        open fun onUndoStateChanged()

        open fun onSongAdded(index: Int, song: Song)
        open fun onSongRemoved(index: Int)
        open fun onSongMoved(from: Int, to: Int)

        open fun onEvents()
    }

    val state: MediaPlayerState
    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val has_focus: Boolean

    val radio_state: RadioInstance.RadioState

    var repeat_mode: MediaPlayerRepeatMode
    var volume: Float

    open fun play()
    open fun pause()
    open fun playPause()

    open fun seekTo(position_ms: Long)
    open fun seekToSong(index: Int)
    open fun seekToNext()
    open fun seekToPrevious()

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun addSong(song: Song)
    fun addSong(song: Song, index: Int)
    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    abstract fun savePersistentQueue()

    var service_state: PlayerServiceState
}

class PlayerService: PlatformPlayerService() {
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)

    private var update_timer: Timer? = null

    private var song_marked_as_watched: Boolean = false
    private var tracking_song_index = 0

    private val prefs_listener = object : PlatformPreferences.Listener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                Settings.KEY_DISCORD_ACCOUNT_TOKEN.name -> {
                    discord_status.onDiscordAccountTokenChanged()
                }
//                Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name -> {
//                    vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)
//                }
            }
        }
    }

    private val player_listener = object : Listener() {
        var current_song: Song? = null

        val song_metadata_listener = Query.Listener {
            discord_status.updateDiscordStatus(current_song)
        }

        override fun onSongTransition(song: Song?, manual: Boolean) {
            if (manual) {
                service_state = service_state.copy(stop_after_current_song = false)
            }

            with(context.database) {
                current_song?.also { current ->
                    mediaItemQueries.titleById(current.id).removeListener(song_metadata_listener)
                    songQueries.artistById(current.id).removeListener(song_metadata_listener)
                }
                current_song = song
                current_song?.also { current ->
                    mediaItemQueries.titleById(current.id).addListener(song_metadata_listener)
                    songQueries.artistById(current.id).addListener(song_metadata_listener)
                }
            }

            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }

            if (current_song_index == tracking_song_index + 1) {
                onSongEnded()
            }
            tracking_song_index = current_song_index
            song_marked_as_watched = false

            radio.checkRadioContinuation()
            discord_status.updateDiscordStatus(song)

            play()
        }

        override fun onSongMoved(from: Int, to: Int) {
            service_player.radio.checkRadioContinuation()
            radio.instance.onSongMoved(from, to)
        }

        override fun onSongRemoved(index: Int) {
            radio.checkRadioContinuation()
            radio.instance.onSongRemoved(index)
        }

        override fun onStateChanged(state: MediaPlayerState) {
            if (state == MediaPlayerState.ENDED) {
                onSongEnded()
            }
        }
    }

    private fun onSongEnded() {
        if (service_state.stop_after_current_song) {
            pause()
            service_state = service_state.copy(stop_after_current_song = false)
        }
    }

    override fun onCreate() {
        super.onCreate()

        addListener(player_listener)
        context.getPrefs().addListener(prefs_listener)

        radio = object : RadioHandler(service_player, context) {
            override fun onInstanceStateChanged(state: RadioInstance.RadioState) {
                this@PlayerService.service_state = this@PlayerService.service_state.copy(
                    radio_state = state
                )
            }
        }
        persistent_queue = PersistentQueueHandler(service_player, context)
        discord_status = DiscordStatusHandler(service_player, context)

        if (update_timer == null) {
            update_timer = createUpdateTimer()
        }

        discord_status.onDiscordAccountTokenChanged()
    }

    override fun onDestroy() {
        coroutine_scope.cancel()

        removeListener(player_listener)
        context.getPrefs().removeListener(prefs_listener)

        discord_status.release()

        update_timer?.cancel()
        update_timer = null

        super.onDestroy()
    }

    override fun savePersistentQueue() {
        coroutine_scope.launch {
            persistent_queue.savePersistentQueue()
        }
    }

    private fun createUpdateTimer(): Timer {
        return Timer().apply {
            scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        coroutine_scope.launch(Dispatchers.Main) {
                            persistent_queue.savePersistentQueue()
                            markWatched()
                        }
                    }

                    suspend fun markWatched() = withContext(Dispatchers.Main) {
                        if (
                            !song_marked_as_watched
                            && is_playing
                            && current_position_ms >= SONG_MARK_WATCHED_POSITION
                        ) {
                            song_marked_as_watched = true

                            val song = getSong() ?: return@withContext

                            withContext(Dispatchers.IO) {
                                song.incrementPlayCount(context)

                                val mark_endpoint = context.ytapi.user_auth_state?.MarkSongAsWatched
                                if (mark_endpoint?.isImplemented() == true && Settings.KEY_ADD_SONGS_TO_HISTORY.get(context)) {
                                    val result = mark_endpoint.markSongAsWatched(song)
                                    if (result.isFailure) {
                                        SpMp.error_manager.onError("autoMarkSongAsWatched", result.exceptionOrNull()!!)
                                    }
                                }
                            }
                        }
                    }
                },
                0,
                UPDATE_INTERVAL
            )
        }
    }
}
