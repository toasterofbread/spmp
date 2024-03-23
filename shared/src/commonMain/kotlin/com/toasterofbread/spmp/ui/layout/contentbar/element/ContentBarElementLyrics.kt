package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LyricsLineDisplay
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.composable.AlignableCrossfade
import kotlinx.serialization.Serializable

// TODO | Settings
// LYRICS_LINGER,
// LYRICS_SHOW_FURIGANA

@Serializable
data class ContentBarElementLyrics(
    override val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    override val size: Int = DEFAULT_SIZE,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.LYRICS

    override fun copyWithSize(size_mode: ContentBarElement.SizeMode, size: Int): ContentBarElement =
        copy(size_mode = size_mode, size = size)

    @Composable
    override fun isDisplaying(): Boolean = displaying

    private var displaying: Boolean by mutableStateOf(false)

    @Composable
    override fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val current_song: Song? by player.status.song_state

        val lyrics_state: SongLyricsLoader.ItemState? = remember(current_song?.id) { current_song?.let { SongLyricsLoader.getItemState(it, player.context) } }
        val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, true)

        displaying = lyrics_state?.lyrics?.synced == true

        AlignableCrossfade(
            lyrics_state?.lyrics,
            modifier,
            contentAlignment = Alignment.Center
        ) { lyrics ->
            if (lyrics?.synced != true) {
                return@AlignableCrossfade
            }

            LyricsLineDisplay(
                lyrics = lyrics,
                getTime = {
                    (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
                }
            )
        }
    }
}
