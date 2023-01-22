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

    val response = client.newCall(request).execute()

    val is_audio = response.body!!.contentType().toString().startsWith("audio/")
    response.close()

    return response.code == 200 && is_audio
}

fun getVideoDownloadUrl(id: String, format_priority: List<Int>): Result<String> {

    val RESPONSE_DATA_START = "ytInitialPlayerResponse = "
    fun getUrl(): Result<String> {

        if (format_priority.isEmpty()) {
            throw RuntimeException()
        }

        val request = Request.Builder()
            .url("https://www.youtube.com/watch?v=$id")
            .header("Cookie", "CONSENT=YES+1")
            .header("User-Agent", DATA_API_USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return Result.failure(response)
        }

        val html = response.body!!.string()
        val start = html.indexOf(RESPONSE_DATA_START) + RESPONSE_DATA_START.length
        val end = html.indexOf("};", start) + 1

        val data = klaxon.parseJsonObject(html.substring(start, end).reader())
        val streaming_data = data.obj("streamingData")!!

        val found_formats: MutableMap<Int, JsonObject> = mutableMapOf()

        val formats = streaming_data.array<JsonObject>("adaptiveFormats")!! + streaming_data.array<JsonObject>("formats")!!
        for (format in formats) {
            val itag: Int = format.int("itag")!!
            val index = format_priority.indexOf(itag)

            if (index == -1) {
                continue
            }

            found_formats[itag] = format

            if (index == 0) {
                break
            }
        }

        for (format in format_priority) {
            val found = found_formats[format] ?: continue

            val url = found.string("url")
            if (url != null) {
                return Result.success(found.string("url")!!)
            }

            val decrypter_result = SignatureCypherDecrypter.fromPlayerPage(html)
            if (!decrypter_result.success) {
                return Result.failure(decrypter_result.exception)
            }

            val decrypted = decrypter_result.data.decryptSignatureCypher(found.string("signatureCipher")!!)
            return Result.success(decrypted)
        }

        return Result.failure(RuntimeException("Could not find a download url for video $id with itag in $format_priority"))
    }

    // For some reason the URL is occasionally invalid (GET either fails with 403 or yields the URL itself)
    // I can't tell why this occurs, but just getting the URL again always seems to produce a valid one
    var i = 0
    while (i++ < MAX_RETRIES) {
        val url = getUrl()
        if (!url.success || checkUrl(url.data)) {
            return url
        }
    }

    return Result.failure(RuntimeException("Could not find a download url for video $id with itag in $format_priority after $MAX_RETRIES"))
}

// Based on https://github.com/wayne931121/youtube_downloader
class SignatureCypherDecrypter(base_js: String) {
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
        fun fromPlayerPage(player_html: String): Result<SignatureCypherDecrypter> {
            val url_start = player_html.indexOf("\"jsUrl\":\"") + 9
            val url_end = player_html.indexOf(".js\"", url_start) + 3

            val request = Request.Builder()
                .url("https://www.youtube.com${player_html.substring(url_start, url_end)}")
                .header("User-Agent", DATA_API_USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (response.code != 200) {
                return Result.failure(response)
            }

            return Result.success(SignatureCypherDecrypter(response.body!!.string()))
        }
    }

    fun decryptSignatureCypher(signature_cypher: String): String {
        var url: String? = null
        var s: String? = null
        for (param in signature_cypher.split('&')) {
            val split = param.split("=")
            when (split[0]) {
                "url" -> url = URLDecoder.decode(split[1], "UTF-8")
                "s" -> s = URLDecoder.decode(split[1], "UTF-8")
            }
        }

        val cypher_list = s!!.toMutableList()
        for (execute in to_execute) {
            elements[execute.first]!!.invoke(cypher_list, execute.second)
        }

        val decrypted = URLEncoder.encode(cypher_list.joinToString(""), "UTF-8")
        return url!! + "&alr=yes&sig=$decrypted"
    }
}
