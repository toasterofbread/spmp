package com.toasterofbread.spmp.ui.layout.contentbar.element

import kotlinx.serialization.json.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LyricsLineDisplay
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.composekit.utils.common.getValue
import LocalPlayerState

class ContentBarElementLyrics(data: JsonObject?): ContentBarElement {
    override fun getData(): ContentBarElementData =
        ContentBarElementData(type = ContentBarElement.Type.LYRICS)

    @Composable
    override fun Element(vertical: Boolean, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val current_song: Song? by player.status.song_state

        val lyrics_state: SongLyricsLoader.ItemState? = remember(current_song?.id) { current_song?.let { SongLyricsLoader.getItemState(it, player.context) } }
        val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, true)

        Crossfade(lyrics_state?.lyrics, modifier.fillMaxWidth()) { lyrics ->
            if (lyrics?.synced != true) {
                return@Crossfade
            }

            LyricsLineDisplay(
                lyrics = lyrics,
                getTime = {
                    (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
                }
            )
        }
    }

    @Composable
    override fun Configuration(modifier: Modifier, onModification: () -> Unit) {
    }
}
