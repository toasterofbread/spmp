package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistShuffleEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextContinuationResponse
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response

class YTMArtistShuffleEndpoint(override val api: YoutubeMusicApi): ArtistShuffleEndpoint() {
    override suspend fun getArtistShuffle(artist: Artist, continuation: String?): Result<RadioData> = withContext(Dispatchers.IO) {
        artist.loadData(api.context, populate_data = false).onFailure {
            return@withContext Result.failure(RuntimeException(it))
        }

        val shuffle_playlist_id: String? = artist.ShufflePlaylistId.get(api.database)
            ?: return@withContext Result.failure(RuntimeException("ShufflePlaylistId not loaded for artist $artist"))

        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/next")
            .addAuthApiHeaders()
            .postWithBody(
                mutableMapOf(
                    "enablePersistentPlaylistPanel" to true,
                    "playlistId" to shuffle_playlist_id
                )
                .also {
                    if (continuation != null) {
                        it["continuation"] = continuation
                    }
                }
            )
            .build()

        val result: Result<Response> = api.performRequest(request)
        val radio: YoutubeiNextResponse.PlaylistPanelRenderer?

        if (continuation == null) {
            val data: YoutubeiNextResponse = result.parseJsonResponse {
                return@withContext Result.failure(
                    com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException.ofYoutubeJsonRequest(
                        request,
                        api,
                        cause = it
                    )
                )
            }

            val renderer: YoutubeiNextResponse.MusicQueueRenderer = data
                .contents
                .singleColumnMusicWatchNextResultsRenderer
                .tabbedRenderer
                .watchNextTabbedResultsRenderer
                .tabs
                .first()
                .tabRenderer
                .content!!
                .musicQueueRenderer

            radio = renderer.content?.playlistPanelRenderer
        }
        else {
            val data: YoutubeiNextContinuationResponse = result.parseJsonResponse {
                return@withContext Result.failure(
                    com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.DataParseException.ofYoutubeJsonRequest(
                        request,
                        api,
                        cause = it
                    )
                )
            }

            radio = data
                .continuationContents
                .playlistPanelContinuation
        }

        return@withContext Result.success(
            RadioData(
                radio?.contents?.map { item ->
                    val renderer = item.getRenderer()
                    val song = SongData(renderer.videoId)

                    song.title = renderer.title.first_text

                    renderer.getArtist(song, api.context).fold(
                        { artist ->
                            if (artist != null) {
                                song.artist = artist
                            }
                        },
                        { return@withContext Result.failure(it) }
                    )

                    return@map song
                } ?: emptyList(),
                radio?.continuations?.firstOrNull()?.data?.continuation
            )
        )
    }
}
