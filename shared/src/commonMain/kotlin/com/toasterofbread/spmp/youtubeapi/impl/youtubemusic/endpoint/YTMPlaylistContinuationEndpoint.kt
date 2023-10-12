package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistRef
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.youtubeapi.endpoint.PlaylistContinuationEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMPlaylistContinuationEndpoint(override val api: YoutubeMusicApi): PlaylistContinuationEndpoint() {
    override suspend fun getPlaylistContinuation(
        initial: Boolean,
        token: String,
        skip_initial: Int,
    ): Result<Pair<List<MediaItemData>, String?>> = withContext(Dispatchers.IO) {
        if (initial) {
            val playlist = RemotePlaylistRef(token)
            playlist.loadData(api.context, false).onFailure {
                return@withContext Result.failure(it)
            }

            val items = playlist.Items.get(api.database) ?: return@withContext Result.failure(IllegalStateException("Items for loaded $playlist is null"))

            return@withContext Result.success(Pair(
                items.drop(skip_initial).map { it.getEmptyData() },
                playlist.Continuation.get(api.database)?.token
            ))
        }

        val hl = api.context.getDataLanguage()
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse?ctoken=$token&continuation=$token&type=next")
            .addAuthApiHeaders()
            .postWithBody()
            .build()

        val result = api.performRequest(request)
        val data: YoutubeiBrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        return@withContext runCatching {
            val shelf = data.continuationContents?.musicPlaylistShelfContinuation ?: return@runCatching Pair(emptyList(), null)

            val items: List<MediaItemData> =
                shelf.contents!!.mapNotNull { item ->
                    item.toMediaItemData(hl)?.first
                }

            val continuation: String? = shelf.continuations?.firstOrNull()?.nextContinuationData?.continuation

            return@runCatching Pair(items.drop(skip_initial), continuation)
        }
    }
}
