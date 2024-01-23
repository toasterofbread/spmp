package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.youtubeapi.endpoint.LikedArtistsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiShelfContentsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response

class YTMLikedArtistsEndpoint(override val auth: YoutubeMusicAuthInfo): LikedArtistsEndpoint() {
    override suspend fun getLikedArtists(): Result<List<ArtistData>> {
        val result: Result<List<ArtistData>> = withContext(Dispatchers.IO) {
            val hl: String = api.context.getDataLanguage()
            val request: Request = Request.Builder()
                .endpointUrl("/youtubei/v1/browse")
                .addAuthApiHeaders()
                .postWithBody(mapOf("browseId" to "FEmusic_library_corpus_track_artists"))
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
                    .musicShelfRenderer!!
                    .contents!!
            }
            catch (e: Throwable) {
                return@withContext Result.failure(e)
            }

            val artists: List<ArtistData> = playlist_data.mapNotNull {
                val item: MediaItemData? = it.toMediaItemData(hl)?.first
                if (item !is ArtistData) {
                    return@mapNotNull null
                }
                return@mapNotNull item
            }

            return@withContext Result.success(artists)
        }

        result.onSuccess { artists ->
            withContext(Dispatchers.IO) {
                with(api.context.database) {
                    transaction {
                        for (artist in artists.asReversed()) {
                            artist.saveToDatabase(this@with)
                        }
                    }
                }
            }
        }

        return result
    }
}
