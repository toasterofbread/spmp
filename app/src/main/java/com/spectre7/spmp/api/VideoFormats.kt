package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import okhttp3.*
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Duration

private const val MAX_RETRIES = 5

private fun checkUrl(url: String): Boolean {
    val request = Request.Builder()
        .url(url)
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", DATA_API_USER_AGENT)
        .build()

    val response = DataApi.client.newCall(request).execute()

    val is_invalid = response.body!!.contentType().toString().startsWith("text/")
    response.close()

    return response.code == 200 && !is_invalid
}

data class YoutubeVideoFormat (
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val quality: String,
    val qualityLabel: String? = null,
    val audioQuality: String? = null,
    val signatureCipher: String? = null,
    val url: String? = null
) {
    var stream_url: String? = null
    val audio_only: Boolean get() = mimeType.startsWith("audio")

    fun loadStreamUrl(video_id: String) {
        if (stream_url != null) {
            return
        }
        if (url != null) {
            stream_url = url
            return
        }

        for (i in 0 until MAX_RETRIES) {
            val decrypter = SignatureCipherDecrypter.fromNothing("https://music.youtube.com/watch?v=$video_id", i == 0).getDataOrThrow()
            stream_url = decrypter.decryptSignatureCipher(signatureCipher!!)
            if (checkUrl(stream_url!!)) {
                break
            }

            if (i + 1 == MAX_RETRIES) {
                stream_url = null
                throw RuntimeException("Could not load formats for video $video_id after $MAX_RETRIES attempts")
            }
        }
    }
}

fun getVideoFormats(id: String, selectFormat: (List<YoutubeVideoFormat>) -> YoutubeVideoFormat): DataApi.Result<YoutubeVideoFormat> {

    val RESPONSE_DATA_START = "ytInitialPlayerResponse = "
    val request = Request.Builder()
        .url("https://www.youtube.com/watch?v=$id")
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", DATA_API_USER_AGENT)
        .build()
    
    fun getFormats(itag: Int?): DataApi.Result<Pair<SignatureCipherDecrypter, List<YoutubeVideoFormat>>> {
        val response = DataApi.client.newCall(request).execute()
        if (response.code != 200) {
            return DataApi.Result.failure(response)
        }

        val html = response.body!!.string()

        val decrypter_result = SignatureCipherDecrypter.fromPlayerPage(html)
        if (!decrypter_result.success) {
            return DataApi.Result.failure(decrypter_result.exception)
        }

        val start = html.indexOf(RESPONSE_DATA_START) + RESPONSE_DATA_START.length
        val end = html.indexOf("};", start) + 1
        val streaming_data = DataApi.klaxon.parseJsonObject(html.substring(start, end).reader()).obj("streamingData")!!

        if (itag != null) {
            for (group in listOf("adaptiveFormats", "formats")) {
                for (format in streaming_data.array<JsonObject>(group) ?: listOf()) {
                    if (format.int("itag") == itag) {
                        return DataApi.Result.success(Pair(
                            decrypter_result.data,
                            listOf(DataApi.klaxon.parseFromJsonObject(format)!!)
                        ))
                    }
                }
            }
            return DataApi.Result.failure(RuntimeException("$itag | ${DataApi.klaxon.toJsonString(streaming_data)}"))
        }
        else {
            return DataApi.Result.success(Pair(
                decrypter_result.data,
                DataApi.klaxon.parseFromJsonArray<YoutubeVideoFormat>(streaming_data.array<JsonObject>("adaptiveFormats")!!)!!
                + DataApi.klaxon.parseFromJsonArray(streaming_data.array<JsonObject>("formats")!!)!!
            ))
        }
    }

    var itag: Int? = null

    // For some reason the URL is occasionally invalid (GET either fails with 403 or yields the URL itself)
    // I can't tell why this occurs, but just getting the URL again always seems to produce a valid one
    for (i in 0 until MAX_RETRIES) {
        val result = getFormats(itag)
        if (!result.success) {
            return DataApi.Result.failure(result.exception)
        }

        val (decrypter, formats) = result.data

        val selected_format = if (itag == null) selectFormat(formats) else formats.first()
        itag = selected_format.itag

        selected_format.stream_url = selected_format.url ?: decrypter.decryptSignatureCipher(selected_format.signatureCipher!!)

        if (checkUrl(selected_format.stream_url!!)) {
            return DataApi.Result.success(selected_format)
        }
    }

    return DataApi.Result.failure(RuntimeException("Could not load formats for video $id after $MAX_RETRIES attempts"))
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

        fun fromPlayerPage(player_html: String): DataApi.Result<SignatureCipherDecrypter> {
            val url_start = player_html.indexOf("\"jsUrl\":\"") + 9
            val url_end = player_html.indexOf(".js\"", url_start) + 3

            val request = Request.Builder()
                .url("https://www.youtube.com${player_html.substring(url_start, url_end)}")
                .header("User-Agent", DATA_API_USER_AGENT)
                .build()

            val response = DataApi.client.newCall(request).execute()
            if (response.code != 200) {
                return DataApi.Result.failure(response)
            }

            return DataApi.Result.success(SignatureCipherDecrypter(response.body!!.string()))
        }

        fun fromNothing(player_url: String, allow_cached: Boolean = true): DataApi.Result<SignatureCipherDecrypter> {
            if (cached_instance != null && allow_cached) {
                return DataApi.Result.success(cached_instance!!)
            }

            val request = Request.Builder()
                .url(player_url)
                .header("Cookie", "CONSENT=YES+1")
                .header("User-Agent", DATA_API_USER_AGENT)
                .build()
        
            val response = DataApi.client.newCall(request).execute()
            if (response.code != 200) {
                return DataApi.Result.failure(response)
            }

            val result = fromPlayerPage(response.body!!.string())
            if (result.success) {
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
