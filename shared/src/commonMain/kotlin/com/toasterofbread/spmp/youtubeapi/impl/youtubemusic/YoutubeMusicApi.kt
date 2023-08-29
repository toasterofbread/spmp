package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import SpMp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.JsonObject
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.Cache
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringArray
import com.toasterofbread.spmp.resources.getStringArraySafe
import com.toasterofbread.spmp.resources.getStringSafe
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeApi.PostBodyContext
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.formats.NewPipeVideoFormatsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.composable.YTMLoginPage
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMArtistWithParamsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMCreateYoutubeChannelEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMGetHomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMLoadArtistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMLoadPlaylistEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMLoadSongEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMPlaylistContinuationEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMRadioBuilderEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongLyricsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongRadioEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMSongRelatedContentEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMYoutubeChannelCreationFormEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.commons.text.StringSubstitutor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.InputStream
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream

private val PLAIN_HEADERS = listOf("accept-language", "user-agent", "accept-encoding", "content-encoding", "origin")

class RelatedGroup(val title: String, val items: List<MediaItem>?, val description: String?)

class YoutubeMusicApi(
    override val context: PlatformContext,
    val api_url: String = YoutubeApi.Type.YOUTUBE_MUSIC.getDefaultUrl(),
): YoutubeApi {
    init {
        check(!api_url.endsWith('/'))
    }

    override val db: Database get() = context.database

    private var init_job: Job? = null
    private var initialised: Boolean = false

    private lateinit var youtubei_headers: Headers

    private lateinit var youtubei_context: JsonObject
    private lateinit var youtubei_context_alt: JsonObject
    private lateinit var youtubei_context_android: JsonObject
    private lateinit var youtubei_context_mobile: JsonObject
    private lateinit var youtube_context_ui_language: JsonObject

    private val prefs_change_listener = object : PlatformPreferences.Listener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                Settings.KEY_YTM_AUTH.name -> onUserAuthStateChanged()
                Settings.KEY_LANG_DATA.name -> {
                    updateYtmContext()
                    Cache.reset()
                }
            }
        }
    }

    override suspend fun init() {
        coroutineScope {
            init_job = launch(Dispatchers.Default) {
                launch {
                    val headers_builder = Headers.Builder()

                    val headers = getStringArraySafe("ytm_headers", context)
                    var i = 0
                    while (i < headers.size) {
                        val key = headers[i++]
                        val value = headers[i++]
                        headers_builder.add(key, value)
                    }

                    headers_builder["accept-encoding"] = "gzip, deflate"
                    headers_builder["content-encoding"] = "gzip"
                    headers_builder["origin"] = api_url
                    headers_builder["user-agent"] = getUserAgent()

                    youtubei_headers = headers_builder.build()
                }
                launch {
                    context.getPrefs().addListener(prefs_change_listener)

                    NewPipe.init(object : Downloader() {
                        override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                            val url = request.url()
                            val request_body: RequestBody? = request.dataToSend()?.let {
                                it.toRequestBody(null, 0, it.size)
                            }

                            val request_builder = Request.Builder()
                                .method(request.httpMethod(), request_body).url(url)
                                .addHeader("User-Agent", getUserAgent())

                            for ((header_name, header_value_list) in request.headers()) {
                                if (header_value_list.size > 1) {
                                    request_builder.removeHeader(header_name)
                                    for (headerValue in header_value_list) {
                                        request_builder.addHeader(header_name, headerValue)
                                    }
                                } else if (header_value_list.size == 1) {
                                    request_builder.header(header_name, header_value_list[0])
                                }
                            }

                            val response = performRequest(request_builder.build(), true).getOrThrowHere()
                            if (response.code == 429) {
                                response.close()
                                throw ReCaptchaException("reCaptcha Challenge requested", url)
                            }

                            return org.schabi.newpipe.extractor.downloader.Response(
                                response.code,
                                response.message,
                                response.headers.toMultimap(),
                                response.body?.string(),
                                response.request.url.toString()
                            )
                        }
                    })
                }

                launch {
                    updateYtmContext()
                }
            }
        }
        initialised = true
    }

    private fun updateYtmContext() {
        val context_substitutor = StringSubstitutor(
            mapOf(
                "user_agent" to getUserAgent(),
                "hl" to SpMp.getDataLanguage(context)
            ),
            "\${", "}"
        )

        youtubei_context = klaxon.parseJsonObject(
            context_substitutor.replace(getStringSafe("ytm_context", context)).reader()
        )
        youtubei_context_alt = klaxon.parseJsonObject(
            context_substitutor.replace(getStringSafe("ytm_context_alt", context)).reader()
        )
        youtubei_context_android = klaxon.parseJsonObject(
            context_substitutor.replace(getStringSafe("ytm_context_android", context)).reader()
        )
        youtubei_context_mobile = klaxon.parseJsonObject(
            context_substitutor.replace(getStringSafe("ytm_context_mobile", context)).reader()
        )
        youtube_context_ui_language = klaxon.parseJsonObject(
            StringSubstitutor(
                mapOf(
                    "user_agent" to getUserAgent(),
                    "hl" to SpMp.getUiLanguage(context)
                ),
                "\${", "}"
            ).replace(getStringSafe("ytm_context", context)).reader()
        )
    }

    fun getUserAgent(): String = getStringSafe("ytm_user_agent", context)

    override fun PostBodyContext.getContextPostBody(): JsonObject =
        when (this) {
            PostBodyContext.BASE -> youtubei_context
            PostBodyContext.ALT -> youtubei_context_alt
            PostBodyContext.ANDROID -> youtubei_context_android
            PostBodyContext.MOBILE -> youtubei_context_mobile
            PostBodyContext.UI_LANGUAGE -> youtube_context_ui_language
        }

    override suspend fun Request.Builder.addAuthlessApiHeaders(include: List<String>?): Request.Builder {
        if (!include.isNullOrEmpty()) {
            val headers = include.ifEmpty { PLAIN_HEADERS }
            for (header in headers) {
                val value = youtubei_headers[header] ?: continue
                header(header, value)
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

    override fun Request.Builder.postWithBody(body: Map<String, Any?>?, context: PostBodyContext): Request.Builder {
        val final_body = context.getContextPostBody().toMutableMap()
        for (entry in body ?: emptyMap()) {
            final_body[entry.key] = entry.value
        }
        return post(
            klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
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

    override fun getResponseStream(response: Response): InputStream {
        return GZIPInputStream(response.body!!.byteStream())
    }

    private fun onUserAuthStateChanged() {
        user_auth_state = YoutubeApi.UserAuthState.unpackSetData(Settings.KEY_YTM_AUTH.get(context)).let { data ->
            if (data.first != null) YoutubeMusicAuthInfo(this, data.first!!, data.second)
            else null
        }
    }

    override var user_auth_state: YoutubeMusicAuthInfo? by mutableStateOf(
        YoutubeApi.UserAuthState.unpackSetData(Settings.KEY_YTM_AUTH.get(context)).let { data ->
            if (data.first != null) YoutubeMusicAuthInfo(this, data.first!!, data.second)
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

    override val VideoFormats = NewPipeVideoFormatsEndpoint(this)

    override val HomeFeed = YTMGetHomeFeedEndpoint(this)
    override val GenericFeedViewMorePage = YTMGenericFeedViewMorePageEndpoint(this)
    override val SongRadio = YTMSongRadioEndpoint(this)

    override val ArtistsWithParams = YTMArtistWithParamsEndpoint(this)

    override val PlaylistContinuation = YTMPlaylistContinuationEndpoint(this)

    override val RadioBuilder = YTMRadioBuilderEndpoint(this)

    override val SongRelatedContent = YTMSongRelatedContentEndpoint(this)
    override val SongLyrics = YTMSongLyricsEndpoint(this)

    override val Search = SearchEndpointImpl(this)
}
