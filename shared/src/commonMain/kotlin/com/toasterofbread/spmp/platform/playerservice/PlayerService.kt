package com.toasterofbread.spmp.platform.playerservice

import ProgramArguments
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import spms.socketapi.shared.SpMsPlayerRepeatMode
import spms.socketapi.shared.SpMsPlayerState

data class PlayerServiceLoadState(
    val loading: Boolean,
    val loading_message: String? = null,
    val error: Throwable? = null
)

interface PlayerService {
    val context: AppContext
    val service_player: PlayerServicePlayer

    fun onCreate()
    fun onDestroy()

    val load_state: PlayerServiceLoadState
    val connection_error: Throwable?
    val state: SpMsPlayerState
    val is_playing: Boolean
    val song_count: Int
    val current_song_index: Int
    val current_position_ms: Long
    val duration_ms: Long
    val has_focus: Boolean

    val radio_instance: RadioInstance

    var repeat_mode: SpMsPlayerRepeatMode
    var volume: Float

    fun isPlayingOverLatentDevice(): Boolean

    fun play()
    fun pause()
    fun playPause()

    fun seekTo(position_ms: Long)
    fun seekToSong(index: Int)
    fun seekToNext()
    fun seekToPrevious()
    fun undoSeek()

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun addSong(song: Song, index: Int)
    fun moveSong(from: Int, to: Int)
    fun removeSong(index: Int)

    fun addListener(listener: PlayerListener)
    fun removeListener(listener: PlayerListener)

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier, opacity: Float)

    @Composable
    fun LoadScreenExtraContent(modifier: Modifier, requestServiceChange: (PlayerServiceCompanion) -> Unit) {}
}

interface PlayerServiceCompanion {
    fun isAvailable(context: AppContext, launch_arguments: ProgramArguments): Boolean = true
    fun isServiceRunning(context: AppContext): Boolean

    fun connect(
        context: AppContext,
        launch_arguments: ProgramArguments,
        instance: PlayerService? = null,
        onConnected: (PlayerService) -> Unit,
        onDisconnected: () -> Unit
    ): Any

    fun disconnect(context: AppContext, connection: Any)
}
