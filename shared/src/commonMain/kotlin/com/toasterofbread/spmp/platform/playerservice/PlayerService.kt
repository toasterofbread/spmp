package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import dev.toastbits.spms.player.Player
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PlayerServiceLoadState(
    val loading: Boolean,
    val loading_message: String? = null,
    val error: Throwable? = null
)

interface PlayerService: Player {
    val context: AppContext
    val service_player: PlayerServicePlayer

    fun onCreate()
    fun onDestroy()

    val radio_instance: RadioInstance
    val volume: Float

    fun isPlayingOverLatentDevice(): Boolean
    fun undoSeek()

    val load_state: PlayerServiceLoadState

    fun getSong(): Song?
    fun getSong(index: Int): Song?

    fun addSong(song: Song, index: Int): Int

    fun addListener(listener: PlayerListener)
    fun removeListener(listener: PlayerListener)

    @Composable
    fun Visualiser(colour: Color, modifier: Modifier, opacity: Float)

    @Composable
    fun PersistentContent(requestServiceChange: (PlayerServiceCompanion) -> Unit) {}
    @Composable
    fun LoadScreenExtraContent(item_modifier: Modifier, requestServiceChange: (PlayerServiceCompanion) -> Unit) {}
}

fun PlayerService.seekToPreviousOrRepeat() {
    context.coroutineScope.launch {
        val threshold_s: Float = context.settings.Behaviour.REPEAT_SONG_ON_PREVIOUS_THRESHOLD_S.get()
        val threshold: Duration? =
            if (threshold_s < 0f) null
            else (threshold_s * 1000).roundToLong().milliseconds

        seekToPrevious(threshold)
    }
}
