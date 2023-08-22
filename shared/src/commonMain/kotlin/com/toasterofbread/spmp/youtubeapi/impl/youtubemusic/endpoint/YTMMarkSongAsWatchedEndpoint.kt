package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.MarkSongAsWatchedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.unit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.Random

private data class PlaybackTrackingRepsonse(
    val playbackTracking: PlaybackTracking
) {
    val playback_url: String get() = playbackTracking.videostatsPlaybackUrl.baseUrl

    data class PlaybackTracking(
        val videostatsPlaybackUrl: TrackingUrl
    )
    data class TrackingUrl(val baseUrl: String)
}

private const val CPN_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
private fun generateCpn(): String {
    return (0 until 16).map { CPN_ALPHABET[Random().nextInt(256) and 63] }.joinToString("")
}

class YTMMarkSongAsWatchedEndpoint(override val auth: YoutubeMusicAuthInfo): MarkSongAsWatchedEndpoint() {
    override suspend fun markSongAsWatched(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        var result = api.performRequest(buildPlayerRequest(song.id, false))
        if (result.isFailure) {
            result = api.performRequest(buildPlayerRequest(song.id, true))
        }

        val data: PlaybackTrackingRepsonse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        check(data.playback_url.contains("s.youtube.com")) { data.playback_url }

        val playback_url = data.playback_url.replace("s.youtube.com", "music.youtube.com")
            .toHttpUrl().newBuilder()
            .setQueryParameter("ver", "2")
            .setQueryParameter("c", "WEB_REMIX")
            .setQueryParameter("cpn", generateCpn())
            .build()

        val request = Request.Builder()
            .url(playback_url)
            .addAuthApiHeaders(include = listOf("cookie", "user-agent"))
            .build()

        return@withContext api.performRequest(request).unit()
    }

    private suspend fun buildPlayerRequest(id: String, alt: Boolean): Request {
        return Request.Builder()
            .endpointUrl("/youtubei/v1/player")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf("videoId" to id),
                if (alt) YoutubeApi.PostBodyContext.ALT else YoutubeApi.PostBodyContext.BASE
            )
            .build()
    }
}
