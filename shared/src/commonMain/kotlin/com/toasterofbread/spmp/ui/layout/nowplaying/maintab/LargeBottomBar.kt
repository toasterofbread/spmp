package com.toasterofbread.spmp.ui.layout.nowplaying.maintab

import LocalPlayerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.ui.component.LikeDislikeButton
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

@Composable
internal fun LargeBottomBar(modifier: Modifier = Modifier) {
    val player: PlayerState = LocalPlayerState.current
    val auth_state: YoutubeApi.UserAuthState? = player.context.ytapi.user_auth_state
    val current_song: Song? by player.status.song_state

    val button_colour: Color = LocalContentColor.current.copy(alpha = 0.5f)

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (auth_state != null) {
            current_song?.also { song ->
                LikeDislikeButton(song, auth_state) { button_colour }
            }
        }

        Spacer(Modifier.fillMaxWidth().weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton({ player.expansion.close() }, Modifier.bounceOnClick().appHover(true)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = button_colour)
            }
        }
    }
}
