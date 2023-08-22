package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistAddSongsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.unit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMAccountPlaylistAddSongsEndpoint(override val auth: YoutubeMusicAuthInfo): AccountPlaylistAddSongsEndpoint() {
    override suspend fun addSongs(playlist: Playlist, song_ids: Collection<String>): Result<Unit> {
        if (song_ids.isEmpty()) {
            return Result.success(Unit)
        }

        val actions: List<Map<String, String>> = song_ids.map { id ->
            mapOf(
                "action" to "ACTION_ADD_VIDEO",
                "addedVideoId" to id,
                "dedupeOption" to "DEDUPE_OPTION_SKIP"
            )
        }

        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse/edit_playlist")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "playlistId" to Playlist.formatYoutubeId(playlist.id),
                    "actions" to actions
                )
            )
            .build()

        return withContext(Dispatchers.IO) {
            val result = api.performRequest(request)
            result.getOrNull()?.close()
            return@withContext result.unit()
        }
    }
}
