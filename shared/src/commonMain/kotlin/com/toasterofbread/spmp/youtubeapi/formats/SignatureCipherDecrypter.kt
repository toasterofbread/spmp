package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.executeResult
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrThrowHere
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder

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
                .header("User-Agent", getString("ytm_user_agent"))
                .build()

            val result = OkHttpClient().executeResult(request)
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
                .header("User-Agent", getString("ytm_user_agent"))
                .build()

            val response_result = OkHttpClient().executeResult(request)
            if (response_result.isFailure) {
                return response_result.cast()
            }

            val result = fromPlayerPage(response_result.getOrThrowHere().body!!.string())
            if (result.isSuccess) {
                cached_instance = result.getOrThrowHere()
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
