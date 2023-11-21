package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.PlatformPreferencesListener
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.AuthSettings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.model.settings.category.SystemSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.resources.getStringArraySafe
import com.toasterofbread.spmp.resources.getStringSafe
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeApi.PostBodyContext
import com.toasterofbread.spmp.youtubeapi.endpoint.ArtistRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpoint
import com.toasterofbread.spmp.youtubeapi.formats.VideoFormatsEndpointType
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.composable.YTMLoginPage
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMArtistRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMCreateYoutubeChannelEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMGetHomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMLoadArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMLoadPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMLoadSongEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMPlaylistContinuationEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMRadioBuilderEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSearchEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSearchSuggestionsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongLyricsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongRelatedContentEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMYoutubeChannelCreationFormEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.commons.text.StringSubstitutor
import java.io.Reader
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

internal val PLAIN_HEADERS = listOf("accept-language", "user-agent", "accept-encoding", "content-encoding", "origin")

class RelatedGroup(val title: String, val items: List<MediaItem>?, val description: String?)

data class YoutubeMusicApi(
    override val context: AppContext,
    val api_url: String = YoutubeApi.Type.YOUTUBE_MUSIC.getDefaultUrl(),
): YoutubeApi {
    init {
        check(!api_url.endsWith('/'))
    }

    override val database: Database get() = context.database

    private var initialised: Boolean = false

    private lateinit var youtubei_headers: Headers

    private lateinit var youtubei_context: JsonObject
    private lateinit var youtubei_context_android_music: JsonObject
    private lateinit var youtubei_context_android: JsonObject
    private lateinit var youtubei_context_mobile: JsonObject
    private lateinit var youtube_context_ui_language: JsonObject

    private val prefs_change_listener = object : PlatformPreferencesListener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                AuthSettings.Key.YTM_AUTH.getName() -> onUserAuthStateChanged()
                SystemSettings.Key.LANG_DATA.getName() -> {
                    updateYtmContext()
                }
            }
        }
    }

    private fun buildHeaders() {
        val headers_builder = Headers.Builder()

        val headers = getStringArraySafe("ytm_headers", context)
        var i = 0
        while (i < headers.size) {
            val key = headers[i++]
            val value = headers[i++]
            headers_builder.add(key, value)
        }

        headers_builder["origin"] = api_url
        headers_builder["user-agent"] = getUserAgent()

        youtubei_headers = headers_builder.build()
    }

    private val init_lock = Mutex()
    override suspend fun init() {
        init_lock.withLock {
            if (initialised) {
                return
            }

            coroutineScope {
                launch(Dispatchers.Default) {
                    launch {
                        buildHeaders()
                    }
                    launch {
                        context.getPrefs().addListener(prefs_change_listener)
                    }
                    launch {
                        updateYtmContext()
                    }
                }
            }

            initialised = true
        }
    }

    private fun updateYtmContext() {
        val context_substitutor = StringSubstitutor(
            mapOf(
                "user_agent" to getUserAgent(),
                "hl" to context.getDataLanguage().split("-", limit = 2).firstOrNull()
            ),
            "\${", "}"
        )

        youtubei_context = JsonParser.parseString(
            context_substitutor.replace(getStringSafe("ytm_context", context))
        ).asJsonObject
        youtubei_context_android_music = JsonParser.parseString(
            context_substitutor.replace(getStringSafe("ytm_context_android_music", context))
        ).asJsonObject
        youtubei_context_android = JsonParser.parseString(
            context_substitutor.replace(getStringSafe("ytm_context_android", context))
        ).asJsonObject
        youtubei_context_mobile = JsonParser.parseString(
            context_substitutor.replace(getStringSafe("ytm_context_mobile", context))
        ).asJsonObject
        youtube_context_ui_language = gson.fromJson(
            StringSubstitutor(
                mapOf(
                    "user_agent" to getUserAgent(),
                    "hl" to context.getUiLanguage()
                ),
                "\${", "}"
            ).replace(getStringSafe("ytm_context", context)).reader(),
            youtubei_context::class.java
        )
    }

    fun getUserAgent(): String = getStringSafe("ytm_user_agent", context)

    override suspend fun PostBodyContext.getContextPostBody(): JsonObject {
        init()
        return when (this) {
            PostBodyContext.BASE -> youtubei_context
            PostBodyContext.ANDROID_MUSIC -> youtubei_context_android_music
            PostBodyContext.ANDROID -> youtubei_context_android
            PostBodyContext.MOBILE -> youtubei_context_mobile
            PostBodyContext.UI_LANGUAGE -> youtube_context_ui_language
        }
    }

    override suspend fun Request.Builder.addAuthlessApiHeaders(include: List<String>?): Request.Builder {
        init()

        if (!include.isNullOrEmpty()) {
            for (header_key in include) {
                val value = youtubei_headers[header_key] ?: continue
                header(header_key, value)
            }
        }
        else {
            for (header in youtubei_headers) {
                header(header.first, header.second)
            }
        }

        return this
    }

    override fun Request.Builder.endpointUrl(endpoint: String): Request.Builder {
        val joiner = if (endpoint.contains('?')) '&' else '?'
        return url("$api_url$endpoint${joiner}prettyPrint=false")
    }

    override suspend fun Request.Builder.postWithBody(body: Map<String, Any?>?, context: PostBodyContext): Request.Builder {
        val final_body: JsonObject = context.getContextPostBody().deepCopy()

        for (entry in body ?: emptyMap()) {
            final_body.remove(entry.key)
            final_body.add(entry.key, gson.toJsonTree(entry.value))
        }
        return post(
            gson.toJson(final_body).toRequestBody("application/json".toMediaType())
        )
    }

    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(Duration.ofMillis(DEFAULT_CONNECT_TIMEOUT.toLong()))
            .retryOnConnectionFailure(false)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
            .also {
                Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE
            }

    override fun performRequest(request: Request, allow_fail_response: Boolean, from_api: Boolean): Result<Response> {
        return client.executeResult(request, allow_fail_response, if (from_api) this else null)
    }

    override fun getResponseReader(response: Response): Reader {
        return response.body!!.charStream()
    }

    private fun onUserAuthStateChanged() {
        user_auth_state = YoutubeApi.UserAuthState.unpackSetData(AuthSettings.Key.YTM_AUTH.get(context), context).let { data ->
            if (data.first != null) YoutubeMusicAuthInfo.create(this, data.first!!, data.second)
            else null
        }
    }

    override var user_auth_state: YoutubeMusicAuthInfo? by mutableStateOf(
        YoutubeApi.UserAuthState.unpackSetData(AuthSettings.Key.YTM_AUTH.get(context), context).let { data ->
            if (data.first != null) YoutubeMusicAuthInfo.create(this, data.first!!, data.second)
            else null
        }
    )
        private set

    override val UpdateUserAuthState = YTMUserAuthStateEndpoint(this)
    override val YoutubeChannelCreationForm = YTMYoutubeChannelCreationFormEndpoint(this)
    override val CreateYoutubeChannel = YTMCreateYoutubeChannelEndpoint(this)
    override val LoginPage = YTMLoginPage(this)

    override val LoadSong = YTMLoadSongEndpoint(this)
    override val LoadArtist = YTMLoadArtistEndpoint(this)
    override val LoadPlaylist = YTMLoadPlaylistEndpoint(this)

    override val VideoFormats: VideoFormatsEndpoint
        get() = Settings.getEnum<VideoFormatsEndpointType>(StreamingSettings.Key.VIDEO_FORMATS_METHOD).instantiate(this)

    override val HomeFeed = YTMGetHomeFeedEndpoint(this)
    override val GenericFeedViewMorePage = YTMGenericFeedViewMorePageEndpoint(this)
    override val SongRadio = YTMSongRadioEndpoint(this)

    override val ArtistWithParams = YTMArtistWithParamsEndpoint(this)
    override val ArtistRadio: ArtistRadioEndpoint = YTMArtistRadioEndpoint(this)

    override val PlaylistContinuation = YTMPlaylistContinuationEndpoint(this)

    override val RadioBuilder = YTMRadioBuilderEndpoint(this)

    override val SongRelatedContent = YTMSongRelatedContentEndpoint(this)
    override val SongLyrics = YTMSongLyricsEndpoint(this)

    override val Search = YTMSearchEndpoint(this)
    override val SearchSuggestions = YTMSearchSuggestionsEndpoint(this)
}
