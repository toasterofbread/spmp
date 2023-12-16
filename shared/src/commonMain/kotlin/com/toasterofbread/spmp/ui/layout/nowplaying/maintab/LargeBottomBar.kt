package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.getValue
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LyricsLineDisplay
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover

@Composable
internal fun LargeBottomBar(
    modifier: Modifier = Modifier,
    inset_start: Dp = Dp.Unspecified,
    inset_end: Dp = Dp.Unspecified,
    inset_depth: Dp = 0.dp
) {
    val player: PlayerState = LocalPlayerState.current
    val current_song: Song? by player.status.song_state

    val button_colour: Color = LocalContentColor.current.copy(alpha = 0.5f)

    CompositionLocalProvider(LocalContentColor provides button_colour) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .thenIf(
                        inset_depth > 0.dp,
                        elseAction = {
                            width(IntrinsicSize.Min)
                        }
                    ) {
                        align(Alignment.Top)
                        .offset(x = inset_start, y = inset_depth)
                        .width(inset_end - inset_start)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                NowPlayingMainTabActionButtons.OpenExternalButton(current_song)
                NowPlayingMainTabActionButtons.LikeDislikeButton(current_song, Modifier.minimumInteractiveComponentSize())

                Spacer(Modifier.fillMaxWidth().weight(1f))

                NowPlayingMainTabActionButtons.DownloadButton(current_song)
                NowPlayingMainTabActionButtons.RadioButton(current_song)
            }

            Row(
                Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lyrics_state: SongLyricsLoader.ItemState? = remember(current_song?.id) { current_song?.let { SongLyricsLoader.getItemState(it, player.context) } }
                val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, false)

                Crossfade(lyrics_state?.lyrics, Modifier.fillMaxWidth().weight(1f)) { lyrics ->
                    if (lyrics == null) {
                        return@Crossfade
                    }

                    LyricsLineDisplay(
                        lyrics = lyrics,
                        getTime = {
                            (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
                        }
                    )
                }

                IconButton({ player.expansion.close() }, Modifier.bounceOnClick().appHover(true)) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = button_colour)
                }
            }
        }
    }
}
