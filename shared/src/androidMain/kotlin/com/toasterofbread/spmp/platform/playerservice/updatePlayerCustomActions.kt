package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.shared.R
import androidx.media3.session.CommandButton
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import com.toasterofbread.spmp.platform.PlayerServiceCommand
import com.toasterofbread.spmp.resources.getStringTODO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun ForegroundPlayerService.updatePlayerCustomActions(song_liked: SongLikedStatus? = null) {
    coroutine_scope.launch(Dispatchers.Main) {
        val actions: MutableList<CommandButton> = mutableListOf()

        val liked: SongLikedStatus? = song_liked ?: current_song?.Liked?.get(context.database)
        if (liked != null) {
            actions.add(
                CommandButton.Builder()
                    .setDisplayName(
                        when (liked) {
                            SongLikedStatus.NEUTRAL -> getStringTODO("Like")
                            SongLikedStatus.LIKED -> getStringTODO("Remove like")
                            SongLikedStatus.DISLIKED -> getStringTODO("Remove dislike")
                        }
                    )
                    .setSessionCommand(
                        PlayerServiceCommand.SetLiked(
                            when (liked) {
                                SongLikedStatus.NEUTRAL -> SongLikedStatus.LIKED
                                SongLikedStatus.LIKED, SongLikedStatus.DISLIKED -> SongLikedStatus.NEUTRAL
                            }
                        ).getSessionCommand()
                    )
                    .setIconResId(
                        when (liked) {
                            SongLikedStatus.NEUTRAL -> R.drawable.ic_thumb_up_off
                            SongLikedStatus.LIKED -> R.drawable.ic_thumb_up
                            SongLikedStatus.DISLIKED -> R.drawable.ic_thumb_down
                        }
                    )
                    .build()
            )
        }

        media_session.setCustomLayout(actions)
    }
}
