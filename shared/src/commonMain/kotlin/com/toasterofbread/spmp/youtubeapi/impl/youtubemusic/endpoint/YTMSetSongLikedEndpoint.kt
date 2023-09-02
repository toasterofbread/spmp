package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.song.toLong
import com.toasterofbread.spmp.youtubeapi.endpoint.SetSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSetSongLikedEndpoint(override val auth: YoutubeMusicAuthInfo): SetSongLikedEndpoint() {
    override suspend fun setSongLiked(song: Song, liked: SongLikedStatus): Result<Unit> = withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/" + when (liked) {
                SongLikedStatus.NEUTRAL -> "like/removelike"
                SongLikedStatus.LIKED -> "like/like"
                SongLikedStatus.DISLIKED -> "like/dislike"
            })
            .addAuthApiHeaders()
            .postWithBody(
                mapOf("target" to mapOf("videoId" to song.id))
            )
            .build()

        return@withContext api.performRequest(request).fold(
            {
                runCatching {
                    api.db.songQueries.updatelikedById(liked.toLong(), song.id)
                    it.close()
                }
            },
            { Result.failure(it) }
        )
    }
}
