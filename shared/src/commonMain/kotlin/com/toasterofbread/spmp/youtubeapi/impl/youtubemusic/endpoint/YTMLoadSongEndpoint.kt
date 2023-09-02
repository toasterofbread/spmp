package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadSongEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.processDefaultResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private data class PlayerData(
    val videoDetails: VideoDetails? = null,
) {
    class VideoDetails(
        val title: String,
        val channelId: String,
    )
}

class YTMLoadSongEndpoint(override val api: YoutubeMusicApi): LoadSongEndpoint() {
    override suspend fun loadSong(song_data: SongData): Result<SongData> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        var request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/next")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "enablePersistentPlaylistPanel" to true,
                    "isAudioOnly" to true,
                    "videoId" to song_data.id,
                )
            )
            .build()

        val next_result = api.performRequest(request).fold(
            { response ->
                processDefaultResponse(song_data, response, hl, api)
            },
            { null }
        )

        if (next_result?.isSuccess != true) {
            // 'next' endpoint has no artist, use 'player' instead
            request = Request.Builder()
                .endpointUrl("/youtubei/v1/player")
                .addAuthApiHeaders()
                .postWithBody(
                    mapOf("videoId" to song_data.id)
                )
                .build()

            val result = api.performRequest(request)
            val video_data: PlayerData = result.parseJsonResponse {
                return@withContext Result.failure(it)
            }

            if (video_data.videoDetails == null) {
                return@withContext Result.success(song_data)
            }

            song_data.title = video_data.videoDetails.title
            song_data.artist = ArtistRef(video_data.videoDetails.channelId)
        }

        song_data.loaded = true
        song_data.saveToDatabase(api.db)

        return@withContext Result.success(song_data)
    }
}
