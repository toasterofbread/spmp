package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.ytmkt.endpoint.SetSongLikedEndpoint
import dev.toastbits.ytmkt.model.external.SongLikedStatus

enum class PlayerServiceNotificationCustomAction {
    LIKE,
    UNLIKE;

    suspend fun execute(player: PlayerService, context: AppContext) {
        when (this) {
            LIKE,
            UNLIKE -> {
                val song: Song = player.getSong() ?: return

                val target_liked: SongLikedStatus =
                    when (this) {
                        LIKE -> SongLikedStatus.LIKED
                        UNLIKE -> SongLikedStatus.NEUTRAL
                    }

                val set_liked_endpoint: SetSongLikedEndpoint? = context.ytapi.user_auth_state?.SetSongLiked
                song.updateLiked(target_liked, set_liked_endpoint, context).getOrThrow()
            }
        }
    }
}
