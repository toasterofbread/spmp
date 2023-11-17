package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProviderImpl
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.processDefaultResponse
import com.toasterofbread.composekit.utils.common.indexOfOrNull
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.youtubeapi.model.Header
import com.toasterofbread.spmp.youtubeapi.model.HeaderRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.net.URI

private const val EMPTY_PLAYLIST_IMAGE_URL_PREFIX: String = "https://www.gstatic.com/youtube/media/ytm/images/pbg/playlist-empty-state"

private data class PlaylistUrlResponse(
    val microformat: Microformat?,
    val header: Header?
) {
    data class Microformat(val microformatDataRenderer: MicroformatDataRenderer)
    data class MicroformatDataRenderer(val urlCanonical: String?)
}

private fun formatBrowseId(browse_id: String): String =
    if (
        !browse_id.startsWith("VL")
        && !browse_id.startsWith("MPREb_")
    ) "VL$browse_id"
    else browse_id

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

        var browse_id: String =
            if (playlist_data.browse_params == null) formatBrowseId(playlist_data.id)
            else playlist_data.id

        var playlist_url: String? = playlist_data.playlist_url ?: playlist_data.PlaylistUrl.get(api.database)

        if (playlist_url == null) {
            val request = Request.Builder()
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

            val response: PlaylistUrlResponse = api.performRequest(request).parseJsonResponse {
                return@withContext Result.failure(it)
            }

            playlist_url = response.microformat?.microformatDataRenderer?.urlCanonical
            playlist_data.playlist_url = playlist_url

            val header_renderer: HeaderRenderer? = response.header?.getRenderer()
            if (header_renderer != null) {
                playlist_data.title = header_renderer.title?.firstTextOrNull()
                playlist_data.thumbnail_provider = MediaItemThumbnailProvider.fromThumbnails(header_renderer.getThumbnails())
            }
        }

        if (playlist_url != null) {
            val start: Int = playlist_url.indexOf("?list=") + 6
            val end: Int =
                playlist_url.indexOfOrNull("&", start) ?: playlist_url.length
            browse_id = formatBrowseId(playlist_url.substring(start, end))
        }
        
        val hl: String = api.context.getDataLanguage()
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

        val provider: MediaItemThumbnailProvider? = playlist_data.thumbnail_provider
        if (provider is MediaItemThumbnailProviderImpl && (provider.url_a.startsWith(EMPTY_PLAYLIST_IMAGE_URL_PREFIX) || provider.url_b?.startsWith(EMPTY_PLAYLIST_IMAGE_URL_PREFIX) == true)) {
            playlist_data.thumbnail_provider = null
        }

        playlist_data.loaded = true
        playlist_data.saveToDatabase(
            api.database,
            uncertain = playlist_data.playlist_type != PlaylistType.PLAYLIST,
            subitems_uncertain = true
        )

        return@withContext Result.success(playlist_data)
    }
}
