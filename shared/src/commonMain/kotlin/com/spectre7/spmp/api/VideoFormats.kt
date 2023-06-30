package com.toasterofbread.spmp.api

import com.toasterofbread.spmp.model.mediaitem.enums.SongAudioQuality
import com.toasterofbread.spmp.resources.getString
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder

private const val MAX_RETRIES = 5

private fun checkUrl(url: String): Boolean {
    val request = Request.Builder()
        .url(url)
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", Api.user_agent)
        .build()

    val response = Api.request(request, true).getOrNull() ?: return false
    val is_invalid = response.body!!.contentType().toString().startsWith("text/")
    response.close()

    return response.code == 200 && !is_invalid
}

data class YoutubeVideoFormat(
    val itag: Int? = null,
    val mimeType: String,
    val bitrate: Int,
    val signatureCipher: String? = null,
    val url: String? = null,
) {
    val identifier: Int = itag ?: bitrate
    var stream_url: String? = url
    val audio_only: Boolean get() = mimeType.startsWith("audio")
    var matched_quality: SongAudioQuality? = null

    fun loadStreamUrl(video_id: String): Throwable? {
        if (stream_url != null) {
            return null
        }
        if (url != null) {
            stream_url = url
            return null
        }

        for (i in 0 until MAX_RETRIES) {
            val decrypter = SignatureCipherDecrypter.fromNothing("https://music.youtube.com/watch?v=$video_id", i == 0).getOrThrowHere()
            stream_url = decrypter.decryptSignatureCipher(signatureCipher!!)
            if (checkUrl(stream_url!!)) {
                break
            }

            if (i + 1 == MAX_RETRIES) {
                stream_url = null
                return RuntimeException("Could not load formats for video $video_id after $MAX_RETRIES attempts")
            }
        }

        return null
    }
}

fun testVideoFormatMethods(ids: List<String>, filter: ((YoutubeVideoFormat) -> Boolean)? = null) {
    val methods: Map<String, (id: String) -> Result<List<YoutubeVideoFormat>>> = mapOf(
        Pair("NewPipe", { id -> getVideoFormats(id, filter) }),
        Pair("Piped API", { id -> getVideoFormatsFallback1(id, filter) }),
        Pair("Youtubei", { id -> getVideoFormatsFallback2(id, filter) }),
        Pair("Youtube player", { id -> getVideoFormatsFallback3(id, filter) })
    )

    println("--- Begin test ---")

    val totals: MutableMap<String, Long> = methods.mapValues { 0L }.toMutableMap()
    for (id in ids) {
        println("Testing id $id")
        for (method in methods) {
            println("Testing method ${method.key}")
            val start = System.currentTimeMillis()
            method.value.invoke(id)
            totals[method.key] = totals[method.key]!! + (System.currentTimeMillis() - start)
        }
    }

    println("Test results:")

    for (total in totals) {
        println("${total.key}: ${(total.value.toFloat() / ids.size) / 1000f}s")
    }

    println("--- End test ---")
}

private fun VideoStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}

private fun AudioStream.toYoutubeVideoFormat(): YoutubeVideoFormat {
    return YoutubeVideoFormat(itag, format!!.mimeType, bitrate, url = content)
}

fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)? = null): Result<List<YoutubeVideoFormat>> {
    val stream_info: StreamInfo

    try {
        stream_info = StreamInfo.getInfo(
            NewPipe.getService(0).getStreamExtractor(
                YoutubeStreamLinkHandlerFactory.getInstance().fromId(id)
            )
        )
    }
    catch (e: ParsingException) {
        return Result.failure(e)
    }

    return Result.success(
        stream_info.audioStreams.mapNotNull { stream ->
            val format = stream.toYoutubeVideoFormat()
            if (filter?.invoke(format) == false) {
                return@mapNotNull null
            }
            return@mapNotNull format
        }
        + stream_info.videoStreams.mapNotNull { stream ->
            val format = stream.toYoutubeVideoFormat()
            if (filter?.invoke(format) == false) {
                return@mapNotNull null
            }
            return@mapNotNull format
        }
    )
}

private data class PipedStreamsResponse(
    val audioStreams: List<YoutubeVideoFormat>,
    val relatedStreams: List<RelatedStream>,
) {
    data class RelatedStream(val url: String, val type: String)
}

fun getVideoFormatsFallback1(id: String, filter: ((YoutubeVideoFormat) -> Boolean)? = null): Result<List<YoutubeVideoFormat>> {
    val request = Request.Builder().url("https://pipedapi.syncpundit.io/streams/$id").build()
    val result = Api.request(request)

    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val response: PipedStreamsResponse = Api.klaxon.parse(stream)!!
    stream.close()

    return Result.success(response.audioStreams.let { if (filter != null) it.filter(filter) else it })
}

private data class FormatsResponse(
    val playabilityStatus: PlayabilityStatus,
    val streamingData: StreamingData? = null,
) {
    val is_ok: Boolean get() = playabilityStatus.status == "OK"
    data class StreamingData(val formats: List<YoutubeVideoFormat>, val adaptiveFormats: List<YoutubeVideoFormat>)
}
data class PlayabilityStatus(val status: String)

private fun buildVideoFormatsRequest(id: String, alt: Boolean): Request {
    return Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/player?key=${getString("yt_i_api_key")}")
        .post(Api.getYoutubeiRequestBody(
            mapOf(
                "videoId" to id,
                "playlistId" to null
            ),
            context = if (alt) Api.Companion.YoutubeiContextType.ALT else Api.Companion.YoutubeiContextType.BASE
        ))
        .build()
}

fun getVideoFormatsFallback2(id: String, filter: ((YoutubeVideoFormat) -> Boolean)? = null): Result<List<YoutubeVideoFormat>> {
    var result = Api.request(buildVideoFormatsRequest(id, false))
    var formats: FormatsResponse? = null

    if (result.isSuccess) {
        val stream = result.getOrThrow().body!!.charStream()
        formats = Api.klaxon.parse(stream)!!
        stream.close()
    }

    if (formats?.streamingData == null) {
        result = Api.request(buildVideoFormatsRequest(id, true))
        if (result.isFailure) {
            return result.cast()
        }

        val stream = result.getOrThrow().body!!.charStream()
        formats = Api.klaxon.parse(stream)!!
        stream.close()
    }

    if (formats.streamingData == null) {
        return Result.failure(IOException(formats.playabilityStatus.status))
    }

    val streaming_data = formats.streamingData!!
    val ret: MutableList<YoutubeVideoFormat> = mutableListOf()
    var decrypter: SignatureCipherDecrypter? = null

    for (i in 0 until streaming_data.formats.size + streaming_data.adaptiveFormats.size) {
        val format = if (i < streaming_data.formats.size) streaming_data.formats[i] else streaming_data.adaptiveFormats[i - streaming_data.formats.size]
        if (filter != null && !filter(format)) {
            continue
        }

        if (format.url == null) {
            if (decrypter == null) {
                decrypter = SignatureCipherDecrypter.fromNothing("https://music.youtube.com/watch?v=$id").getOrThrowHere()
            }
        }

        val error = format.loadStreamUrl(id)
        if (error != null) {
            return Result.failure(error)
        }
//        format.stream_url = format.url ?: decrypter!!.decryptSignatureCipher(format.signatureCipher!!)
//        println("${format.itag} | ${format.url != null} | ${format.stream_url}")

        ret.add(format)
    }

    return Result.success(ret)
}

private fun getVideoFormatsFallback3(id: String, filter: ((YoutubeVideoFormat) -> Boolean)? = null): Result<List<YoutubeVideoFormat>> {

    val RESPONSE_DATA_START = "ytInitialPlayerResponse = "
    val request = Request.Builder()
        .url("https://www.youtube.com/watch?v=$id")
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", Api.user_agent)
        .build()

    fun getFormats(): Result<Pair<SignatureCipherDecrypter, List<YoutubeVideoFormat>>> {
        var result = Api.request(request)
        if (result.isFailure) {
            return result.cast()
        }

        val html = result.getOrThrowHere().body!!.string()

        val decrypter_result = SignatureCipherDecrypter.fromPlayerPage(html)
        if (!decrypter_result.isSuccess) {
            return decrypter_result.cast()
        }

        val start = html.indexOf(RESPONSE_DATA_START) + RESPONSE_DATA_START.length
        val end = html.indexOf("};", start) + 1

        var streaming_data: FormatsResponse = Api.klaxon.parse(html.substring(start, end).reader())!!
        if (!streaming_data.is_ok) {
            result = Api.request(buildVideoFormatsRequest(id, true))

            if (!result.isSuccess) {
                return result.cast()
            }

            val stream = result.getOrThrow().body!!.charStream()
            streaming_data = Api.klaxon.parse(stream)!!
            stream.close()

            if (!streaming_data.is_ok) {
                return Result.failure(Exception(streaming_data.playabilityStatus.status))
            }
        }

        return Result.success(Pair(
            decrypter_result.data,
            streaming_data.streamingData!!.adaptiveFormats + streaming_data.streamingData!!.formats
        ))
    }

    val ret: MutableList<YoutubeVideoFormat> = mutableListOf()

    // For some reason the URL is occasionally invalid (GET either fails with 403 or yields the URL itself)
    // I can't tell why this occurs, but just getting the URL again always seems to produce a valid one
    for (i in 0 until MAX_RETRIES) {
        val result = getFormats()
        if (!result.isSuccess) {
            return result.cast()
        }

        val (decrypter, formats) = result.data
        var valid: Boolean = true

        for (format in formats) {
            if ((filter != null && !filter(format)) || ret.any { it.identifier == format.identifier }) {
                continue
            }

            format.stream_url = format.url ?: decrypter.decryptSignatureCipher(format.signatureCipher!!)
            if (format.url == null && !checkUrl(format.stream_url!!)) {
                valid = false
                break
            }

            ret.add(format)
        }

        if (valid) {
            return Result.success(ret)
        }
    }

    return Result.failure(RuntimeException("Could not load formats for video $id after $MAX_RETRIES attempts"))
}

// Based on https://github.com/wayne931121/youtube_downloader
class SignatureCipherDecrypter(base_js: String) {
    private val to_execute: MutableList<Pair<String, Int>> = mutableListOf()
    private val elements: MutableMap<String, (sig: MutableList<Char>, arg: Int) -> Unit> = mutableMapOf()

    private val DECRYPT_START_MATCH = "=function(a){a=a.split(\"\");"
    private val DECRYPT_END_MATCH = "return a.join(\"\")};"

    init {
        var start = base_js.indexOf(DECRYPT_START_MATCH) + DECRYPT_START_MATCH.length
        var end = base_js.indexOf(DECRYPT_END_MATCH, start)

        val decrypt_functions = base_js.substring(start, end).split(';')
        if (decrypt_functions.isEmpty()) {
            throw RuntimeException()
        }

        var main_func: String = ""
        for (function in decrypt_functions) {
            if (function.isBlank()) {
                continue
            }

            val dot_i = function.indexOf('.')
            main_func = function.substring(0, dot_i)

            val bracket_i = function.indexOf('(', dot_i + 1)
            val element_func = function.substring(dot_i + 1, bracket_i)

            val arg_i = function.indexOf(',', bracket_i + 1) + 1
            val argument = function.substring(arg_i, function.indexOf(')', arg_i)).toInt()

            to_execute.add(Pair(element_func, argument))
        }

        start = base_js.indexOf("var $main_func={")
        end = base_js.indexOf("};", start + 6 + main_func.length) + 2

        val core = base_js.substring(start, end)

        fun addElementFunc(func: String, action: (MutableList<Char>, Int) -> Unit) {
            end = core.indexOf(func)
            elements[core.substring(end - 2, end).trim()] = action
        }

        addElementFunc(":function(a){a.reverse()}") { sig: MutableList<Char>, arg: Int ->
            sig.reverse()
        }

        addElementFunc(":function(a,b){var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c}") { sig: MutableList<Char>, arg: Int ->
            val c = sig[0]
            sig[0] = sig[arg % sig.size]
            sig[arg % sig.size] = c
        }

        addElementFunc(":function(a,b){a.splice(0,b)}") { sig: MutableList<Char>, arg: Int ->
            for (i in (0 until arg).reversed()) {
                sig.removeAt(i)
            }
        }
    }

    companion object {
        private var cached_instance: SignatureCipherDecrypter? = null

        fun fromPlayerPage(player_html: String): Result<SignatureCipherDecrypter> {
            val url_start = player_html.indexOf("\"jsUrl\":\"") + 9
            val url_end = player_html.indexOf(".js\"", url_start) + 3

            val request = Request.Builder()
                .url("https://www.youtube.com${player_html.substring(url_start, url_end)}")
                .header("User-Agent", Api.user_agent)
                .build()

            val result = Api.request(request)
            if (result.isFailure) {
                return result.cast()
            }

            return Result.success(SignatureCipherDecrypter(result.getOrThrowHere().body!!.string()))
        }

        fun fromNothing(player_url: String, allow_cached: Boolean = true): Result<SignatureCipherDecrypter> {
            if (cached_instance != null && allow_cached) {
                return Result.success(cached_instance!!)
            }

            val request = Request.Builder()
                .url(player_url)
                .header("Cookie", "CONSENT=YES+1")
                .header("User-Agent", Api.user_agent)
                .build()

            val response_result = Api.request(request)
            if (response_result.isFailure) {
                return response_result.cast()
            }

            val result = fromPlayerPage(response_result.getOrThrowHere().body!!.string())
            if (result.isSuccess) {
                cached_instance = result.data
            }
            return result
        }
    }

    fun decryptSignatureCipher(signature_cipher: String): String {
        var url: String? = null
        var s: String? = null
        for (param in signature_cipher.split('&')) {
            val split = param.split("=")
            when (split[0]) {
                "url" -> url = URLDecoder.decode(split[1], "UTF-8")
                "s" -> s = URLDecoder.decode(split[1], "UTF-8")
            }
        }

        val cipher_list = s!!.toMutableList()
        for (execute in to_execute) {
            elements[execute.first]!!.invoke(cipher_list, execute.second)
        }

        val decrypted = URLEncoder.encode(cipher_list.joinToString(""), "UTF-8")
        return url!! + "&alr=yes&sig=$decrypted"
    }
}
