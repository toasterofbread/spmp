package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getString
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

    val response = DataApi.request(request, true).getOrNull() ?: return false

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
    var stream_url: String? = url
    val audio_only: Boolean get() = mimeType.startsWith("audio")
    var matched_quality: Song.AudioQuality? = null

    fun loadStreamUrl(video_id: String) {
        if (stream_url != null) {
            return
        }
        if (url != null) {
            stream_url = url
            return
        }

        for (i in 0 until MAX_RETRIES) {
            val decrypter = SignatureCipherDecrypter.fromNothing("https://music.youtube.com/watch?v=$video_id", i == 0).getOrThrow()
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

fun getVideoFormats(id: String, filter: (YoutubeVideoFormat) -> Boolean = { true }): Result<List<YoutubeVideoFormat>> {

    val RESPONSE_DATA_START = "ytInitialPlayerResponse = "
    val request = Request.Builder()
        .url("https://www.youtube.com/watch?v=$id")
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", DATA_API_USER_AGENT)
        .build()

//    val r = Request.Builder()
//        .url("https://music.youtube.com/youtubei/v1/player?key=${getString(R.string.yt_i_api_key)}")
//        .post(DataApi.getYoutubeiRequestBody("""{
//            "videoId": "$id",
//            "playlistId": null
//        }""", true))
//        .build()
//
//    val resp = DataApi.client.newCall(r).execute()
//    println("R $resp")
//    println("R ${resp.body!!.string()}")

    fun getFormats(): Result<Pair<SignatureCipherDecrypter, List<YoutubeVideoFormat>>> {
        val result = DataApi.request(request)
        if (result.isFailure) {
            return result.cast()
        }

        val html = result.getOrThrow().body!!.string()

        val decrypter_result = SignatureCipherDecrypter.fromPlayerPage(html)
        if (!decrypter_result.isSuccess) {
            return decrypter_result.cast()
        }

        val start = html.indexOf(RESPONSE_DATA_START) + RESPONSE_DATA_START.length
        val end = html.indexOf("};", start) + 1
        val streaming_data = DataApi.klaxon.parseJsonObject(html.substring(start, end).reader()).obj("streamingData")!!

        return Result.success(Pair(
            decrypter_result.data,
            DataApi.klaxon.parseFromJsonArray<YoutubeVideoFormat>(streaming_data.array<JsonObject>("adaptiveFormats")!!)!!
            + DataApi.klaxon.parseFromJsonArray(streaming_data.array<JsonObject>("formats")!!)!!
        ))
    }

    // For some reason the URL is occasionally invalid (GET either fails with 403 or yields the URL itself)
    // I can't tell why this occurs, but just getting the URL again always seems to produce a valid one
    for (i in 0 until MAX_RETRIES) {
        val result = getFormats()
        if (!result.isSuccess) {
            return result.cast()
        }

        val (decrypter, formats) = result.data

        val ret: MutableList<YoutubeVideoFormat> = mutableListOf()
        var valid: Boolean = true

        for (format in formats) {
            if (!filter(format)) {
                continue
            }

            format.stream_url = format.url ?: decrypter.decryptSignatureCipher(format.signatureCipher!!)
            if (ret.isEmpty() && !checkUrl(format.stream_url!!)) {
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
                .header("User-Agent", DATA_API_USER_AGENT)
                .build()

            val result = DataApi.request(request)
            if (result.isFailure) {
                return result.cast()
            }

            return Result.success(SignatureCipherDecrypter(result.getOrThrow().body!!.string()))
        }

        fun fromNothing(player_url: String, allow_cached: Boolean = true): Result<SignatureCipherDecrypter> {
            if (cached_instance != null && allow_cached) {
                return Result.success(cached_instance!!)
            }

            val request = Request.Builder()
                .url(player_url)
                .header("Cookie", "CONSENT=YES+1")
                .header("User-Agent", DATA_API_USER_AGENT)
                .build()
        
            val response_result = DataApi.request(request)
            if (response_result.isFailure) {
                return response_result.cast()
            }

            val result = fromPlayerPage(response_result.getOrThrow().body!!.string())
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
