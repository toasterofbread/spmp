package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import SpMp
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.endpoint.GenericFeedViewMorePageEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMAccountPlaylistAddSongsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMAccountPlaylistEditorEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMAccountPlaylistsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMCreateAccountPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMDeleteAccountPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMMarkSongAsWatchedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSetSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSetSubscribedToArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongLikedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSubscribedToArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.model.YoutubeiBrowseResponse
import com.toasterofbread.utils.common.lazyAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Request

class YoutubeChannelNotCreatedException(
    val headers: Headers,
    val channel_creation_token: String?
): RuntimeException()

class YoutubeMusicAuthInfo private constructor(
    override val api: YoutubeMusicApi,
    override val own_channel: Artist,
    override val headers: Headers
): YoutubeApi.UserAuthState {
    companion object {
        val REQUIRED_HEADERS = listOf("authorization", "cookie")

        fun create(api: YoutubeMusicApi, own_channel: Artist, headers: Headers): YoutubeMusicAuthInfo {
            own_channel.createDbEntry(api.database)
            return YoutubeMusicAuthInfo(api, own_channel, headers)
        }
    }

    override val AccountPlaylists = YTMAccountPlaylistsEndpoint(this)
    override val CreateAccountPlaylist = YTMCreateAccountPlaylistEndpoint(this)
    override val DeleteAccountPlaylist = YTMDeleteAccountPlaylistEndpoint(this)
    override val SubscribedToArtist = YTMSubscribedToArtistEndpoint(this)
    override val SetSubscribedToArtist = YTMSetSubscribedToArtistEndpoint(this)
    override val SongLiked = YTMSongLikedEndpoint(this)
    override val SetSongLiked = YTMSetSongLikedEndpoint(this)
    override val MarkSongAsWatched = YTMMarkSongAsWatchedEndpoint(this)
    override val AccountPlaylistEditor = YTMAccountPlaylistEditorEndpoint(this)
    override val AccountPlaylistAddSongs = YTMAccountPlaylistAddSongsEndpoint(this)
}

class YTMGenericFeedViewMorePageEndpoint(override val api: YoutubeApi): GenericFeedViewMorePageEndpoint() {
    override suspend fun getGenericFeedViewMorePage(browse_id: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val hl = SpMp.data_language
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf("browseId" to browse_id)
            )
            .build()

        val result = api.performRequest(request)
        val data: YoutubeiBrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val items = data
            .contents!!
            .singleColumnBrowseResultsRenderer!!
            .tabs
            .first()
            .tabRenderer
            .content!!
            .sectionListRenderer
            .contents!!
            .first()
            .getMediaItems(hl)

        api.database.transaction {
            for (item in items) {
                item.saveToDatabase(api.database)
            }
        }

        return@withContext Result.success(items)
    }
}
