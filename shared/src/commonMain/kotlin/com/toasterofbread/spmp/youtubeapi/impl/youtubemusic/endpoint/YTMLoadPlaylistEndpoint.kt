package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProviderImpl
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.processDefaultResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response

private const val EMPTY_PLAYLIST_IMAGE_URL_PREFIX: String = "https://www.gstatic.com/youtube/media/ytm/images/pbg/playlist-empty-state"

class YTMLoadPlaylistEndpoint(override val api: YoutubeMusicApi): LoadPlaylistEndpoint() {
    override suspend fun loadPlaylist(
        playlist_data: RemotePlaylistData,
        continuation: MediaItemLayout.Continuation?,
    ): Result<RemotePlaylistData> = withContext(Dispatchers.IO) {
        if (continuation != null) {
            continuation.loadContinuation(api.context).fold(
                {
                    val (items, ctoken) = it
                    playlist_data.items = playlist_data.items?.plus(items as List<SongData>)

                    withContext(Dispatchers.IO) {
                        api.database.transaction {
                            for (playlist_item in items) {
                                playlist_item.saveToDatabase(api.database)
                                playlist_data.Items.addItem(playlist_item as Song, null, api.database)
                            }
                            playlist_data.Continuation.set(
                                ctoken?.let { token ->
                                    continuation.copy(token = token)
                                },
                                api.database
                            )
                        }
                    }

                    return@withContext Result.success(playlist_data)
                },
                { return@withContext Result.failure(it) }
            )
        }

        val browse_id: String =
            if (
                !playlist_data.id.startsWith("VL")
                && !playlist_data.id.startsWith("MPREb_")
                && playlist_data.browse_params == null
            ) "VL${playlist_data.id}"
            else playlist_data.id

        val hl = SpMp.data_language
        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mutableMapOf(
                    "browseId" to browse_id
                ).apply {
                    playlist_data.browse_params?.also { params ->
                        put("params", params)
                    }
                }
            )
            .build()

        val response: Response = api.performRequest(request)
            .getOrElse {
                return@withContext Result.failure(DataParseException.ofYoutubeJsonRequest(request, api, cause = it))
            }

        processDefaultResponse(playlist_data, response, hl, api)
            .getOrElse {
                return@withContext Result.failure(DataParseException.ofYoutubeJsonRequest(request, api, cause = it))
            }

        playlist_data.loaded = true

        val provider = playlist_data.thumbnail_provider
        if (provider is MediaItemThumbnailProviderImpl && (provider.url_a.startsWith(EMPTY_PLAYLIST_IMAGE_URL_PREFIX) || provider.url_b?.startsWith(EMPTY_PLAYLIST_IMAGE_URL_PREFIX) == true)) {
            playlist_data.thumbnail_provider = null
        }

        playlist_data.saveToDatabase(
            api.database,
            uncertain = playlist_data.playlist_type != PlaylistType.PLAYLIST
        )

        return@withContext Result.success(playlist_data)
    }
}
