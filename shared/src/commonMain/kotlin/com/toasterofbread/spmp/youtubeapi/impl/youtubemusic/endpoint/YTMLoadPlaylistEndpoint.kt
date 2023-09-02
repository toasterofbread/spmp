package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.processDefaultResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

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
                        api.db.transaction {
                            for (playlist_item in items) {
                                playlist_item.saveToDatabase(api.db)
                                playlist_data.Items.addItem(playlist_item as Song, null, api.db)
                            }
                            playlist_data.Continuation.set(
                                ctoken?.let { token ->
                                    continuation.copy(token = token)
                                },
                                api.db
                            )
                        }
                    }

                    return@withContext Result.success(playlist_data)
                },
                { return@withContext Result.failure(it) }
            )
        }

        val hl = SpMp.data_language
        val request: Request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                if (!playlist_data.id.startsWith("VL") && !playlist_data.id.startsWith("MPREb_"))
                    mapOf(
                        "browseId" to "VL${playlist_data.id}"
                    )
                else
                    mapOf(
                        "browseId" to playlist_data.id
                    )
            )
            .build()

        val result = api.performRequest(request).fold(
            { response ->
                processDefaultResponse(playlist_data, response, hl, api)
            },
            { error ->
                Result.failure(error)
            }
        )

        return@withContext result.fold(
            {
                playlist_data.loaded = true
                playlist_data.saveToDatabase(
                    api.db,
                    uncertain = playlist_data.playlist_type != PlaylistType.PLAYLIST
                )
                Result.success(playlist_data)
            },
            {
                Result.failure(it)
            }
        )
    }
}
