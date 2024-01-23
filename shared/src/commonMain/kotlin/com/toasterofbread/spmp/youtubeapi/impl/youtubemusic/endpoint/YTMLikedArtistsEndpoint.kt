package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.youtubeapi.endpoint.LikedAlbumsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiShelfContentsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response

class YTMLikedAlbumsEndpoint(override val auth: YoutubeMusicAuthInfo): LikedAlbumsEndpoint() {
    override suspend fun getLikedAlbums(): Result<List<RemotePlaylistData>> {
        val result: Result<List<RemotePlaylistData>> = withContext(Dispatchers.IO) {
            val hl: String = api.context.getDataLanguage()
            val request: Request = Request.Builder()
                .endpointUrl("/youtubei/v1/browse")
                .addAuthApiHeaders()
                .postWithBody(mapOf("browseId" to "FEmusic_liked_albums"))
                .build()

            val result: Result<Response> = api.performRequest(request)
            val data: YoutubeiBrowseResponse = result.parseJsonResponse {
                return@withContext Result.failure(it)
            }

            val playlist_data: List<YoutubeiShelfContentsItem> = try {
                data.contents!!
                    .singleColumnBrowseResultsRenderer!!
                    .tabs
                    .first()
                    .tabRenderer
                    .content!!
                    .sectionListRenderer!!
                    .contents!!
                    .first()
                    .gridRenderer!!
                    .items
            }
            catch (e: Throwable) {
                return@withContext Result.failure(e)
            }

            val albums: List<RemotePlaylistData> = playlist_data.mapNotNull {
                val item: MediaItemData? = it.toMediaItemData(hl)?.first
                if (item !is RemotePlaylistData || item.playlist_type != PlaylistType.ALBUM) {
                    return@mapNotNull null
                }
                return@mapNotNull item
            }

            return@withContext Result.success(albums)
        }

        result.onSuccess { albums ->
            withContext(Dispatchers.IO) {
                with(api.context.database) {
                    transaction {
                        for (album in albums.asReversed()) {
                            album.saveToDatabase(this@with)
                        }
                    }
                }
            }
        }

        return result
    }
}
