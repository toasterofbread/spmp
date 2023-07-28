package com.toasterofbread.spmp.api

import SpMp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beust.klaxon.Converter
import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.model.Cache
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.text.StringSubstitutor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.InputStream
import java.io.Reader
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException
import kotlin.concurrent.thread
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse

const val DEFAULT_CONNECT_TIMEOUT = 10000
private val PLAIN_HEADERS = listOf("accept-language", "user-agent", "accept-encoding", "content-encoding", "origin")
private val YOUTUBE_JSON_DATA_KEYS_TO_REMOVE = listOf("responseContext", "trackingParams", "clickTrackingParams", "serializedShareEntity", "serializedContextData", "loggingContext")

class DataParseException(private val causeDataProvider: suspend () -> Result<String>, message: String? = null, cause: Throwable? = null): RuntimeException(message, cause) {
    private var cause_data: String? = null
    suspend fun getCauseData(): Result<String> {
        val data = cause_data
        if (data != null) {
            return Result.success(data)
        }

        val result = causeDataProvider()
        cause_data = result.getOrNull()
        return result
    }

    companion object {
        fun ofYoutubeJsonRequest(
            request: Request,
            message: String? = null,
            cause: Throwable? = null,
            klaxon: Klaxon = Klaxon(),
            getResponseStream: (Response) -> Reader = { it.getStream().reader() },
            keys_to_remove: List<String> = YOUTUBE_JSON_DATA_KEYS_TO_REMOVE
        ) = DataParseException(
            { runCatching {
                val json_object = withContext(Dispatchers.IO) {
                    val stream = getResponseStream(Api.request(request).getOrThrow())
                    stream.use {
                        klaxon.parseJsonObject(it)
                    }
                }

                // Remove unneeded keys from JSON object
                val items: MutableList<Any> = mutableListOf(json_object)

                while (items.isNotEmpty()) {
                    val obj = items.removeLast()

                    if (obj is Collection<*>) {
                        items.addAll(obj as Collection<Any>)
                        continue
                    }

                    check(obj is JsonObject)

                    for (key in keys_to_remove) {
                        obj.remove(key)
                    }

                    for (value in obj.values) {
                        if (value is JsonObject) {
                            items.add(value)
                        }
                        else if (value is Collection<*>) {
                            items.addAll(value.filterIsInstance<JsonObject>())
                        }
                    }
                }

                json_object.toJsonString(true)
            }},
            message,
            cause
        )
    }
}

fun <T> Result.Companion.failure(response: Response, is_gzip: Boolean = true): Result<T> {
    var body: String
    if (is_gzip) {
        try {
            val stream = response.getStream()
            body = stream.reader().readText()
            stream.close()
        }
        catch (e: ZipException) {
            body = response.body!!.string()
        }
    }
    else {
        body = response.body!!.string()
    }

    response.close()
    return failure(RuntimeException(body))
}

inline fun <I, O> Result<I>.cast(transform: (I) -> O = { it as O }): Result<O> {
    return fold(
        { runCatching { transform(it) } },
        { Result.failure(it) }
    )
}

fun <T> Result<T>.unit(): Result<Unit> {
    return fold(
        { Result.success(Unit) },
        { Result.failure(it) }
    )
}

fun <T> Result<T>.getOrThrowHere(): T =
    fold(
        { it },
        { throw Exception(it) }
    )

fun <T> Result<T>.getOrReport(error_key: String): T? {
    return fold(
        {
            it
        },
        {
            SpMp.error_manager.onError(error_key, it)
            null
        }
    )
}

class Api {

    companion object {
        private val client: OkHttpClient =
            OkHttpClient.Builder()
                .callTimeout(Duration.ofMillis(DEFAULT_CONNECT_TIMEOUT.toLong()))
                .retryOnConnectionFailure(false)
                .protocols(listOf(Protocol.HTTP_1_1))
                .build()
                .also {
                    Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE
                }

        val user_agent: String get() = getString("ytm_user_agent")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val enum_converter = object : Converter {
            override fun canConvert(cls: Class<*>): Boolean {
                return cls.isEnum
            }

            override fun fromJson(jv: JsonValue): Enum<*>? {
                val cls = jv.propertyClass
                if (cls !is Class<*> || !cls.isEnum) {
                    throw IllegalArgumentException("Could not convert $jv to enum")
                }

                val name = jv.inside as String? ?: return null
                val field = cls.declaredFields
                    .firstOrNull { it.name == name || it.getAnnotation(Json::class.java)?.name == name }
                    ?: throw IllegalArgumentException("Could not find enum value for $name")

                return field.get(null) as Enum<*>
            }

            override fun toJson(value: Any): String {
                return "\"${(value as Enum<*>).name}\""
            }
        }
//        private val mediaitem_converter = object : Converter {
//            override fun canConvert(cls: Class<*>): Boolean {
//                return MediaItem::class.java.isAssignableFrom(cls)
//            }
//
//            override fun fromJson(jv: JsonValue): Any? {
//                if (jv.array == null) {
//                    return null
//                }
//
//                try {
//                    return MediaItem.fromDataItems(jv.array!!.toList(), klaxon)
//                }
//                catch (e: Exception) {
//                    throw RuntimeException("Couldn't parse MediaItem (${jv.obj})", e)
//                }
//            }
//
//            override fun toJson(value: Any): String {
//                if (value !is MediaItem) {
//                    throw KlaxonException("Value $value is not a MediaItem")
//                }
//
//                val string = StringBuilder("[${value.type.ordinal},\"${value.id}\"")
//
//                for (item in value.getSerialisedData(klaxon)) {
//                    string.append(',')
//                    string.append(item)
//                }
//
//                string.append(']')
//                return string.toString()
//            }
//        }
//        private val mediaitem_ref_converter = object : Converter {
//            override fun canConvert(cls: Class<*>): Boolean {
//                return MediaItem::class.java.isAssignableFrom(cls)
//            }
//
//            override fun fromJson(jv: JsonValue): Any? {
//                if (jv.array == null) {
//                    return null
//                }
//
//                try {
//                    return MediaItem.fromDataItems(jv.array!!.toList(), klaxon.converter(this))
//                }
//                catch (e: Exception) {
//                    throw RuntimeException("Couldn't parse MediaItem ($jv)", e)
//                }
//            }
//
//            override fun toJson(value: Any): String {
//                if (value !is MediaItem) {
//                    throw KlaxonException("Value $value is not a MediaItem")
//                }
//                return "[${value.type.ordinal},\"${value.id}\"]"
//            }
//        }

        val klaxon: Klaxon get() = Klaxon()
            .converter(enum_converter)
//            .converter(mediaitem_ref_converter)

//        val mediaitem_klaxon: Klaxon get() = Klaxon()
//            .converter(enum_converter)
//            .converter(mediaitem_converter)

        enum class YoutubeiContextType {
            BASE,
            ALT,
            ANDROID,
            MOBILE,
            UI_LANGUAGE;

            fun getContext(): JsonObject {
                return when (this) {
                    BASE -> youtubei_context
                    ALT -> youtubei_context_alt
                    ANDROID -> youtubei_context_android
                    MOBILE -> youtubei_context_mobile
                    UI_LANGUAGE -> youtube_context_ui_language
                }
            }
        }

        var ytm_auth: YoutubeMusicAuthInfo by mutableStateOf(
            Settings.get<Set<String>>(Settings.KEY_YTM_AUTH).let {
                if (it is YoutubeMusicAuthInfo) it else YoutubeMusicAuthInfo(it)
            }
        )
            private set

        val ytm_authenticated: Boolean get() = ytm_auth.initialised

        private lateinit var youtubei_context: JsonObject
        private lateinit var youtubei_context_alt: JsonObject
        private lateinit var youtubei_context_android: JsonObject
        private lateinit var youtubei_context_mobile: JsonObject
        private lateinit var youtube_context_ui_language: JsonObject

        private var youtubei_headers: Headers? = null
        private var header_update_thread: Thread? = null

        private val prefs_change_listener = object : ProjectPreferences.Listener {
            override fun onChanged(prefs: ProjectPreferences, key: String) {
                when (key) {
                    Settings.KEY_YTM_AUTH.name -> onYtmAuthChanged()
                    Settings.KEY_LANG_DATA.name -> {
                        updateYtmContext()
                        Cache.reset()
                    }
                }
            }
        }

        fun request(request: Request, allow_fail_response: Boolean = false, is_gzip: Boolean = true): Result<Response> {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful || allow_fail_response) {
                    return Result.success(response)
                }
                return Result.failure(response, is_gzip)
            }
            catch (e: Throwable) {
                return Result.failure(e)
            }
        }

        fun Response.getStream(): InputStream {
            return GZIPInputStream(body!!.byteStream())
        }

        private fun updateYtmContext() {
            val context_substitutor = StringSubstitutor(
                mapOf(
                    "user_agent" to user_agent,
                    "hl" to SpMp.data_language
                ),
                "\${", "}"
            )

            youtubei_context = klaxon.parseJsonObject(
                context_substitutor.replace(getString("ytm_context")).reader()
            )
            youtubei_context_alt = klaxon.parseJsonObject(
                context_substitutor.replace(getString("ytm_context_alt")).reader()
            )
            youtubei_context_android = klaxon.parseJsonObject(
                context_substitutor.replace(getString("ytm_context_android")).reader()
            )
            youtubei_context_mobile = klaxon.parseJsonObject(
                context_substitutor.replace(getString("ytm_context_mobile")).reader()
            )
            youtube_context_ui_language = klaxon.parseJsonObject(
                StringSubstitutor(
                    mapOf(
                        "user_agent" to user_agent,
                        "hl" to SpMp.ui_language
                    ),
                    "\${", "}"
                ).replace(getString("ytm_context")).reader()
            )
        }

        @Synchronized
        private fun onYtmAuthChanged(): Thread {
            header_update_thread?.also { thread ->
                if (thread.isAlive) {
                    return thread
                }
            }

            header_update_thread = thread {
                val auth_state = Settings.get<Set<String>>(Settings.KEY_YTM_AUTH)
                ytm_auth = if (auth_state is YoutubeMusicAuthInfo) auth_state else YoutubeMusicAuthInfo(auth_state)

                val headers_builder = Headers.Builder().add("user-agent", user_agent)

                if (ytm_auth.initialised) {
                    headers_builder["cookie"] = ytm_auth.cookie
                    for (header in ytm_auth.headers) {
                        headers_builder[header.key] = header.value
                    }
                }
                else {
                    val headers = getStringArray("ytm_headers")
                    var i = 0
                    while (i < headers.size) {
                        val key = headers[i++]
                        val value = headers[i++]
                        headers_builder[key] = value
                    }
                }

                headers_builder["accept-encoding"] = "gzip, deflate"
                headers_builder["content-encoding"] = "gzip"
                headers_builder["origin"] = "https://music.youtube.com"
                headers_builder["user-agent"] = user_agent

                youtubei_headers = headers_builder.build()
            }
            return header_update_thread!!
        }

        fun initialise() {
            updateYtmContext()
            onYtmAuthChanged()

            Settings.prefs.addListener(prefs_change_listener)

            NewPipe.init(object : Downloader() {
                override fun execute(request: NewPipeRequest): NewPipeResponse {
                    val url = request.url()
                    val request_body: RequestBody? = request.dataToSend()?.let {
                        it.toRequestBody(null, 0, it.size)
                    }

                    val request_builder = Request.Builder()
                        .method(request.httpMethod(), request_body).url(url)
                        .addHeader("User-Agent", user_agent)

                    for ((headerName, headerValueList) in request.headers()) {
                        if (headerValueList.size > 1) {
                            request_builder.removeHeader(headerName)
                            for (headerValue in headerValueList) {
                                request_builder.addHeader(headerName, headerValue)
                            }
                        } else if (headerValueList.size == 1) {
                            request_builder.header(headerName, headerValueList[0])
                        }
                    }

                    val response = request(request_builder.build(), true).getOrThrowHere()
                    if (response.code == 429) {
                        response.close()
                        throw ReCaptchaException("reCaptcha Challenge requested", url)
                    }

                    return NewPipeResponse(response.code,
                        response.message,
                        response.headers.toMultimap(),
                        response.body?.string(),
                        response.request.url.toString()
                    )
                }
            })
        }

        internal fun Request.Builder.addYtHeaders(plain: Boolean = false, include: List<String>? = null): Request.Builder {
            if (youtubei_headers == null) {
                header_update_thread!!.join()
            }

            if (plain || !include.isNullOrEmpty()) {
                val headers =
                    if (!include.isNullOrEmpty()) include
                    else PLAIN_HEADERS
                for (header in headers) {
                    val value = youtubei_headers!![header] ?: continue
                    header(header, value)
                }
            }
            else {
                headers(youtubei_headers!!)
            }
            return this
        }

        internal fun Request.Builder.ytUrl(endpoint: String): Request.Builder {
            val joiner = if (endpoint.contains('?')) '&' else '?'
            return url("https://music.youtube.com$endpoint${joiner}prettyPrint=false")
        }

        internal fun getYoutubeiRequestBody(body: Map<String, Any?>? = null, context: YoutubeiContextType = YoutubeiContextType.BASE): RequestBody {
            val final_body = context.getContext().toMutableMap()
            for (entry in body ?: emptyMap()) {
                final_body[entry.key] = entry.value
            }
            return klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
        }
    }
}
