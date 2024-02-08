package com.toasterofbread.spmp.youtubeapi

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.composable.LoginPage
import com.toasterofbread.spmp.youtubeapi.endpoint.*
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.unimplemented.UnimplementedYoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.failure
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.Reader

class EndpointNotImplementedException(endpoint: YoutubeApi.Endpoint?):
    RuntimeException("YoutubeApi endpoint is not implemented: ${endpoint?.getIdentifier()}")

fun Response.getReader(api: YoutubeApi?): Reader {
    if (api != null) {
        return api.getResponseReader(this)
    }
    else {
        return body!!.charStream()
    }
}

fun OkHttpClient.executeResult(request: Request, allow_fail_response: Boolean = false, api: YoutubeApi? = null): Result<Response> {
    try {
        val response: Response = newCall(request).execute()
        if (response.isSuccessful || allow_fail_response) {
            return Result.success(response)
        }
        return Result.failure(request, response, api)
    }
    catch (e: Throwable) {
        return Result.failure(e)
    }
}

inline fun <reified T> Gson.fromJson(reader: Reader): T {
    return fromJson(reader, T::class.java)
}
inline fun <reified T> Gson.fromJson(string: String): T {
    return fromJson(string, T::class.java)
}
inline fun <reified T> Gson.fromMap(map: Map<*, *>): T {
    return fromJson(toJsonTree(map), T::class.java)
}

fun <T: YoutubeApi.Implementable> T.implementedOrNull(): T? =
    if (isImplemented()) this
    else null

interface YoutubeApi {
    val context: AppContext

    val database: Database get() = context.database
    val gson: Gson get() = Gson()

    enum class Type {
        YOUTUBE_MUSIC,
        UNIMPLEMENTED_FOR_TESTING;

        fun isSelectable(): Boolean = this != UNIMPLEMENTED_FOR_TESTING

        fun getDefaultUrl(): String =
            when(this) {
                YOUTUBE_MUSIC -> "https://music.youtube.com"
                UNIMPLEMENTED_FOR_TESTING -> ""
            }

        fun instantiate(context: AppContext, api_url: String): YoutubeApi =
            when(this) {
                YOUTUBE_MUSIC -> YoutubeMusicApi(context, api_url)
                UNIMPLEMENTED_FOR_TESTING -> UnimplementedYoutubeApi(context)
            }

        companion object {
            val DEFAULT: Type = YOUTUBE_MUSIC
        }
    }

    suspend fun init()

    enum class PostBodyContext {
        BASE,
        ANDROID_MUSIC,
        ANDROID,
        MOBILE,
        UI_LANGUAGE
    }
    suspend fun PostBodyContext.getContextPostBody(): JsonObject

    fun Request.Builder.endpointUrl(
        endpoint: String,
    ): Request.Builder

    suspend fun Request.Builder.addAuthlessApiHeaders(
        include: List<String>? = null
    ): Request.Builder

    suspend fun Request.Builder.addAuthApiHeaders(
        include: List<String>? = null
    ): Request.Builder {
        addAuthlessApiHeaders(include)
        user_auth_state?.addHeadersToRequest(this, include)
        return this
    }

    suspend fun Request.Builder.postWithBody(
        body: Map<String, Any?>? = null,
        context: PostBodyContext = PostBodyContext.BASE,
    ): Request.Builder

    fun performRequest(request: Request, allow_fail_response: Boolean = false, from_api: Boolean = true): Result<Response>

    interface Implementable {
        fun isImplemented(): Boolean = true
        fun getIdentifier(): String =
            this::class.java.typeName.split('.', limit = 4).lastOrNull()
            ?: this::class.java.typeName
        fun getNotImplementedMessage(): String = "Implementable not implemented:\n${getIdentifier()}"
        fun <T: Implementable> T.implementedOrNull(): T? = if (isImplemented()) this else null
    }

    abstract class Endpoint: Implementable {
        abstract val api: YoutubeApi
        override fun getNotImplementedMessage(): String = "Endpoint not implemented:\n${getIdentifier()}"

        fun Request.Builder.endpointUrl(endpoint: String): Request.Builder =
            with (api) {
                endpointUrl(endpoint)
            }

        open suspend fun Request.Builder.addAuthApiHeaders(include: List<String>? = null): Request.Builder =
            with (api) {
                addAuthApiHeaders(include)
            }

        open suspend fun Request.Builder.addApiHeadersNoAuth(include: List<String>? = null): Request.Builder =
            with (api) {
                addAuthlessApiHeaders(include)
            }

        suspend fun Request.Builder.postWithBody(body: Map<String, Any?>? = null, context: PostBodyContext = PostBodyContext.BASE): Request.Builder =
            with (api) {
                postWithBody(body, context)
            }

        inline fun <reified T> Result<Response>.parseJsonResponse(
            used_api: YoutubeApi? = api,
            onFailure: (Throwable) -> T
        ): T {
            return try {
                fold(
                    { response ->
                        response.getReader(used_api).use { stream ->
                            api.gson.fromJson(stream, T::class.java)
                        }
                    },
                    { error ->
                        onFailure(error)
                    }
                )
            }
            catch (e: Throwable) {
                onFailure(e)
            }
        }
    }

    fun getResponseReader(response: Response): Reader

    // -- User auth ---
    val user_auth_state: UserAuthState?
    val UpdateUserAuthState: UserAuthStateEndpoint
    val YoutubeChannelCreationForm: YoutubeChannelCreationFormEndpoint
    val CreateYoutubeChannel: CreateYoutubeChannelEndpoint
    val LoginPage: LoginPage

    // --- MediaItems ---
    val LoadSong: LoadSongEndpoint
    val LoadArtist: LoadArtistEndpoint
    val LoadPlaylist: LoadPlaylistEndpoint

    // --- Video formats ---
    val VideoFormats: VideoFormatsEndpoint

    // --- Feed ---
    val HomeFeed: HomeFeedEndpoint
    val GenericFeedViewMorePage: GenericFeedViewMorePageEndpoint
    val SongRadio: SongRadioEndpoint

    // --- Artists ---
    val ArtistWithParams: ArtistWithParamsEndpoint
    val ArtistRadio: ArtistRadioEndpoint
    val ArtistShuffle: ArtistShuffleEndpoint

    // --- Playlists ---
    val PlaylistContinuation: PlaylistContinuationEndpoint

    // --- Search ---
    val Search: SearchEndpoint
    val SearchSuggestions: SearchSuggestionsEndpoint

    // --- Radio builder ---
    val RadioBuilder: RadioBuilderEndpoint

    // --- Song content ---
    val SongRelatedContent: SongRelatedContentEndpoint
    val SongLyrics: SongLyricsEndpoint

    abstract class UserAuthState(
        headers: Headers
    ) {
        abstract val api: YoutubeApi
        abstract val own_channel: Artist?
        val headers: Headers

        init {
            val headers_builder: Headers.Builder = Headers.Builder()

            val headers_to_keep: List<String> = listOf("cookie", "authorization", "x-goog-authuser")
            for (header in headers_to_keep) {
                val value: String = headers[header] ?: continue
                if (header == "cookie") {
                    val filtered_cookies: String = filterCookieString(value) {
                        it.startsWith("__Secure-")
                    }

                    headers_builder.add("cookie", filtered_cookies)
                    continue
                }

                headers_builder.add(header, value)
            }

            this.headers = headers_builder.build()
        }

        private enum class ValueType { CHANNEL, HEADER }

        fun getSetData(): Set<String> {
            return packSetData(own_channel, headers)
        }

        private fun filterCookieString(cookies_string: String, shouldKeepCookie: (String) -> Boolean): String {
            var ret: String = ""
            val cookies: List<String> = cookies_string.split(';')
                .mapNotNull {
                    it.trim().takeIf { it.isNotEmpty() }
                }

            for (cookie in cookies) {
                val (name, value) = cookie.split('=', limit = 2)

                if (!shouldKeepCookie(name)) {
                    continue
                }

                ret += "$name=$value;"
            }

            return ret
        }

        companion object {
            fun packSetData(own_channel: Artist?, headers: Headers): Set<String> {
                val set: MutableSet<String> = mutableSetOf()
                own_channel?.also { channel ->
                    set.add(
                        ValueType.CHANNEL.ordinal.toString() + channel.id
                    )
                }

                for (header in headers) {
                    set.add(
                        ValueType.HEADER.ordinal.toString() + "${header.first}=${header.second}"
                    )
                }

                return set
            }

            fun unpackSetData(set: Set<String>, context: AppContext): Pair<Artist?, Headers> {
                var artist: Artist? = null
                val headers_builder = Headers.Builder()

                for (item in set) {
                    val value = item.substring(1)
                    when (ValueType.entries[item.take(1).toInt()]) {
                        ValueType.CHANNEL -> {
                            if (artist == null) {
                                artist = ArtistRef(value).apply {
                                    createDbEntry(context.database)
                                }
                            }
                        }
                        ValueType.HEADER -> {
                            val split: List<String> = value.split('=', limit = 2)
                            headers_builder.add(split[0], split[1])
                        }
                    }
                }

                return Pair(artist, headers_builder.build())
            }
        }

        fun addHeadersToRequest(builder: Request.Builder, include: List<String>? = null) {
            if (!include.isNullOrEmpty()) {
                for (header_key in include) {
                    val value = headers[header_key] ?: continue
                    builder.header(header_key, value)
                }
            }
            else {
                for (header in headers) {
                    builder.header(header.first, header.second)
                }
            }
        }

        abstract class UserAuthEndpoint: Endpoint() {
            protected abstract val auth: UserAuthState
            override val api: YoutubeApi get() = auth.api

            suspend fun Request.Builder.addAuthlessApiHeaders(include: List<String>?): Request.Builder =
                with (api) {
                    addAuthlessApiHeaders(include)
                }

            override suspend fun Request.Builder.addAuthApiHeaders(include: List<String>?): Request.Builder {
                addApiHeadersNoAuth()
                auth.addHeadersToRequest(this, include)
                return this
            }
        }

        // --- Account playlists ---
        abstract val AccountPlaylists: AccountPlaylistsEndpoint
        abstract val CreateAccountPlaylist: CreateAccountPlaylistEndpoint
        abstract val DeleteAccountPlaylist: DeleteAccountPlaylistEndpoint
        abstract val AccountPlaylistEditor: AccountPlaylistEditorEndpoint
        abstract val AccountPlaylistAddSongs: AccountPlaylistAddSongsEndpoint

        // --- Account liked items ---
        abstract val LikedAlbums: LikedAlbumsEndpoint
        abstract val LikedArtists: LikedArtistsEndpoint
        abstract val LikedPlaylists: LikedPlaylistsEndpoint

        // --- Interaction ---
        abstract val SubscribedToArtist: SubscribedToArtistEndpoint
        abstract val SetSubscribedToArtist: SetSubscribedToArtistEndpoint
        abstract val SongLiked: SongLikedEndpoint
        abstract val SetSongLiked: SetSongLikedEndpoint
        abstract val MarkSongAsWatched: MarkSongAsWatchedEndpoint
    }
}
