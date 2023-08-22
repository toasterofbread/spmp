package com.toasterofbread.spmp.youtubeapi.impl.unimplemented

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.beust.klaxon.JsonObject
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.RadioBuilderArtist
import com.toasterofbread.spmp.youtubeapi.RadioBuilderEndpoint
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.spmp.youtubeapi.SongRelatedContentEndpoint
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.composable.LoginPage
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistWithParamsRow
import com.toasterofbread.spmp.youtubeapi.endpoint.CreateYoutubeChannelEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.GenericFeedViewMorePageEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedLoadResult
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.LoadSongEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.PlaylistContinuationEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchResults
import com.toasterofbread.spmp.youtubeapi.endpoint.SongLyricsEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.SongRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.UserAuthStateEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.YoutubeChannelCreationFormEndpoint
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.RelatedGroup
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream

class UnimplementedYoutubeApi(
    override val context: PlatformContext
): YoutubeApi {

    override suspend fun init() {}

    override fun YoutubeApi.PostBodyContext.getContextPostBody(): JsonObject {
        throw NotImplementedError()
    }

    override fun Request.Builder.endpointUrl(endpoint: String): Request.Builder {
        throw NotImplementedError()
    }

    override suspend fun Request.Builder.addAuthlessApiHeaders(include: List<String>?): Request.Builder {
        throw NotImplementedError()
    }

    override fun Request.Builder.postWithBody(body: Map<String, Any?>?, context: YoutubeApi.PostBodyContext): Request.Builder {
        throw NotImplementedError()
    }

    override fun performRequest(request: Request, allow_fail_response: Boolean, from_api: Boolean): Result<Response> {
        throw NotImplementedError()
    }

    override fun getResponseStream(response: Response): InputStream {
        throw NotImplementedError()
    }

    override val user_auth_state: YoutubeApi.UserAuthState? = null
    override val UpdateUserAuthState = object : UserAuthStateEndpoint() {
        override fun isImplemented(): Boolean = false
        override fun byChannelAndHeaders(own_channel: Artist?, headers: Headers): Result<YoutubeApi.UserAuthState> {
            TODO("Not yet implemented")
        }

        override suspend fun byHeaders(headers: Headers): Result<YoutubeApi.UserAuthState> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }

    override val YoutubeChannelCreationForm = object : YoutubeChannelCreationFormEndpoint() {
        override fun isImplemented(): Boolean = false
        override suspend fun getForm(headers: Headers, channel_creation_token: String): Result<YoutubeAccountCreationForm.ChannelCreationForm> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val CreateYoutubeChannel = object : CreateYoutubeChannelEndpoint() {
        override fun isImplemented(): Boolean = false
        override suspend fun createYoutubeChannel(headers: Headers, channel_creation_token: String, params: Map<String, String>): Result<Artist> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val LoginPage = object : LoginPage() {
        override fun isImplemented(): Boolean = false
        @Composable
        override fun LoginPage(modifier: Modifier, confirm_param: Any?, onFinished: (Result<YoutubeApi.UserAuthState>?) -> Unit) {
            TODO("Not yet implemented")
        }
        @Composable
        override fun LoginConfirmationDialog(info_only: Boolean, onFinished: (param: Any?) -> Unit) {
            TODO("Not yet implemented")
        }
        override fun getTitle(confirm_param: Any?): String? {
            TODO("Not yet implemented")
        }
        override fun getIcon(confirm_param: Any?): ImageVector? {
            TODO("Not yet implemented")
        }
    }
    override val LoadSong = object : LoadSongEndpoint() {
        override fun isImplemented(): Boolean = false
        override suspend fun loadSong(song_data: SongData): Result<Unit> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val LoadArtist = object : LoadArtistEndpoint() {
        override fun isImplemented(): Boolean = false
        override suspend fun loadArtist(artist_data: ArtistData): Result<Unit> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val LoadPlaylist = object : LoadPlaylistEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun loadPlaylist(playlist_data: PlaylistData, continuation: MediaItemLayout.Continuation?): Result<Unit> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val VideoFormats = object : VideoFormatsEndpoint() {
    override fun isImplemented(): Boolean = false
        override fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)?): Result<List<YoutubeVideoFormat>> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val HomeFeed = object : HomeFeedEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getHomeFeed(min_rows: Int, allow_cached: Boolean, params: String?, continuation: String?): Result<HomeFeedLoadResult> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val GenericFeedViewMorePage = object : GenericFeedViewMorePageEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getGenericFeedViewMorePage(browse_id: String): Result<List<MediaItem>> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val SongRadio = object : SongRadioEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getSongRadio(video_id: String, continuation: String?, filters: List<RadioBuilderModifier>): Result<RadioData> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val ArtistsWithParams = object : ArtistWithParamsEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun loadArtistWithParams(artist_id: String, browse_params: String): Result<List<ArtistWithParamsRow>> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val PlaylistContinuation = object : PlaylistContinuationEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getPlaylistContinuation(initial: Boolean, token: String, skip_initial: Int): Result<Pair<List<MediaItemData>, String?>> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val Search = object : SearchEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun searchMusic(query: String, params: String?): Result<SearchResults> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val RadioBuilder = object : RadioBuilderEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getRadioBuilderArtists(selectThumbnail: (List<MediaItemThumbnailProvider.Thumbnail>) -> MediaItemThumbnailProvider.Thumbnail): Result<List<RadioBuilderArtist>> {
            TODO("Not yet implemented")
        }
        override fun buildRadioToken(artists: Set<RadioBuilderArtist>, modifiers: Set<RadioBuilderModifier?>): String {
            TODO("Not yet implemented")
        }
        override suspend fun getBuiltRadio(radio_token: String, context: PlatformContext): Result<PlaylistData?> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val SongRelatedContent = object : SongRelatedContentEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getSongRelated(song: Song): Result<List<RelatedGroup>> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
    override val SongLyrics = object : SongLyricsEndpoint() {
    override fun isImplemented(): Boolean = false
        override suspend fun getSongLyrics(lyrics_id: String): Result<String> {
            TODO("Not yet implemented")
        }
        override val api = this@UnimplementedYoutubeApi
    }
}
