package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import SpMp
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.AccountPlaylistsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.CreateAccountPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.DeleteAccountPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.unit
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMAccountPlaylistsEndpoint(override val auth: YoutubeMusicAuthInfo): AccountPlaylistsEndpoint() {
    override suspend fun getAccountPlaylists(): Result<List<RemotePlaylistData>> {
        val result: Result<List<RemotePlaylistData>> = withContext(Dispatchers.IO) {
            val hl = SpMp.data_language
            val request = Request.Builder()
                .endpointUrl("/youtubei/v1/browse")
                .addAuthApiHeaders()
                .postWithBody(mapOf("browseId" to "FEmusic_liked_playlists"))
                .build()

            val result = api.performRequest(request)
            val data: YoutubeiBrowseResponse = result.parseJsonResponse {
                return@withContext Result.failure(it)
            }

            val playlist_data = try {
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

            val playlists: List<RemotePlaylistData> = playlist_data.mapNotNull {
                // Skip 'New playlist' item
                if (it.musicTwoRowItemRenderer?.navigationEndpoint?.browseEndpoint == null) {
                    return@mapNotNull null
                }

                val item = it.toMediaItemData(hl)?.first
                if (item !is RemotePlaylistData) {
                    return@mapNotNull null
                }

                for (menu_item in it.musicTwoRowItemRenderer.menu?.menuRenderer?.items?.asReversed() ?: emptyList()) {
                    if (menu_item.menuNavigationItemRenderer?.icon?.iconType == "DELETE") {
                        item.owner = auth.own_channel
                        break
                    }
                }

                return@mapNotNull item
            }

            return@withContext Result.success(playlists)
        }

        result.onSuccess { playlists ->
            withContext(Dispatchers.IO) {
                with(api.context.database) {
                    transaction {
                        playlistQueries.clearOwners()
                        for (playlist in playlists.asReversed()) {
                            playlist.saveToDatabase(this@with)
                        }
                    }
                }
            }
        }

        return result
    }
}

private class PlaylistCreateResponse(val playlistId: String)

class YTMCreateAccountPlaylistEndpoint(override val auth: YoutubeMusicAuthInfo): CreateAccountPlaylistEndpoint() {
    override suspend fun createAccountPlaylist(title: String, description: String): Result<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/playlist/create")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf("title" to title, "description" to description),
                YoutubeApi.PostBodyContext.UI_LANGUAGE
            )
            .build()

        val result = api.performRequest(request)
        val data: PlaylistCreateResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        return@withContext Result.success(data.playlistId)
    }
}

class YTMDeleteAccountPlaylistEndpoint(override val auth: YoutubeMusicAuthInfo): DeleteAccountPlaylistEndpoint() {
    override suspend fun deleteAccountPlaylist(playlist_id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/playlist/delete")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf(
                    "playlistId" to RemotePlaylist.formatYoutubeId(playlist_id)
                )
            )
            .build()

        return@withContext api.performRequest(request).unit()
    }
}
