package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatus
import com.toasterofbread.spmp.youtubeapi.endpoint.SongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private data class PlayerLikeResponse(
    val playerOverlays: PlayerOverlays
) {
    val status: LikeButtonRenderer get() = playerOverlays.playerOverlayRenderer.actions.single().likeButtonRenderer

    class PlayerOverlays(val playerOverlayRenderer: PlayerOverlayRenderer)
    data class PlayerOverlayRenderer(val actions: List<Action>)
    data class Action(val likeButtonRenderer: LikeButtonRenderer)
    data class LikeButtonRenderer(val likeStatus: String, val likesAllowed: Boolean)
}

class YTMSongLikedEndpoint(override val auth: YoutubeMusicAuthInfo): SongLikedEndpoint() {
    override suspend fun getSongLiked(song: Song): Result<SongLikedStatus> = withContext(Dispatchers.IO) {
        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/next")
            .addAuthApiHeaders()
            .postWithBody(mapOf("videoId" to song.id))
            .build()

        val result = api.performRequest(request)
        val data: PlayerLikeResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        return@withContext Result.success(when (data.status.likeStatus) {
            "LIKE" -> SongLikedStatus.LIKED
            "DISLIKE" -> SongLikedStatus.DISLIKED
            "INDIFFERENT" -> SongLikedStatus.NEUTRAL
            else -> throw NotImplementedError(data.status.likeStatus)
        })
    }
}
