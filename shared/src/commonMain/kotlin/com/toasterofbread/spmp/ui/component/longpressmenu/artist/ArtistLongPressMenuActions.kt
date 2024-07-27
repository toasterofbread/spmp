package com.toasterofbread.spmp.ui.component.longpressmenu.artist

import LocalPlayerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.ui.component.longpressmenu.LongPressMenuActionProvider
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lpm_action_play
import spmp.shared.generated.resources.lpm_action_open_artist
import spmp.shared.generated.resources.lpm_action_play_after_1_song
import spmp.shared.generated.resources.lpm_action_play_after_x_songs

@Composable
fun LongPressMenuActionProvider.ArtistLongPressMenuActions(artist: MediaItem) {
    require(artist is Artist)
    val player = LocalPlayerState.current

    ActionButton(
        Icons.Default.PlayArrow,
        stringResource(Res.string.lpm_action_play),
        onClick = {
            player.playMediaItem(artist)
        },
        onAltClick = {
            player.playMediaItem(artist, shuffle = true)
        }
    )

    ActiveQueueIndexAction(
        { distance ->
            stringResource(if (distance == 1) Res.string.lpm_action_play_after_1_song else Res.string.lpm_action_play_after_x_songs).replace("\$x", distance.toString())
        },
        onClick = { active_queue_index ->
            player.playMediaItem(artist, at_index = active_queue_index + 1)
        },
        onLongClick = { active_queue_index ->
            player.playMediaItem(artist, at_index = active_queue_index + 1, shuffle = true)
        }
    )

    ActionButton(Icons.Default.Person, stringResource(Res.string.lpm_action_open_artist), onClick = {
        player.openMediaItem(artist,)
    })
}
