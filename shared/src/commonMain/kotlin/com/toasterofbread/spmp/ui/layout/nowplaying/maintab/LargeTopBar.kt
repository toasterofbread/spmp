package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

@Composable
internal fun LargeTopBar(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val current_song: Song? by player.status.song_state

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
    }
}
