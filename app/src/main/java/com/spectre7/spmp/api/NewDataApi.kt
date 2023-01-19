package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.*
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.chaquo.python.Python

const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

data class Result<T> {

    private val _data: T?,
    val data: T get() = _data!!
    
    private val _exception: Exception? = null
    val exception: Exception get() = error!!

    constructor(data: T?, error: Exception) {
        _data = data
        _exception = error
    }

    constructor(data: T?, response: Response) {
        _data = data
        _exception = RuntimeException("${response.message}: ${response.body!!.string()}")
    }

    fun throw() {
        if (!success) {
            throw exception
        }
    }

    val success: Boolean = error == null
}

internal val client = OkHttpClient()
internal val klaxon: Klaxon = Klaxon()
internal val ytd = Python.getInstance().getModule("yt_dlp").callAttr("YoutubeDL")

internal fun getYTMHeaders(): Headers {
    val headers = Headers.Builder()
    headers.add("user-agent", USER_AGENT)
    headers.add("accept", "*/*")
    headers.add("accept-language", "en")
    headers.add("content-type", "application/json")
    headers.add("x-goog-visitor-id", "Cgt1TjR0ckUtOVNXOCiKnOmaBg%3D%3D")
    headers.add("x-youtube-client-name", "67")
    headers.add("x-youtube-client-version", "1.20221019.01.00")
    headers.add("authorization", "SAPISIDHASH 1666862603_ad3286857ed8177c1e0f0f16fc678aaff93ad310")
    headers.add("x-goog-authuser", "1")
    headers.add("x-origin", "https://music.youtube.com")
    headers.add("origin", "https://music.youtube.com")
    headers.add("alt-used", "music.youtube.com")
    headers.add("connection", "keep-alive")
    headers.add("cookie", "***REMOVED***")
    headers.add("sec-fetch-dest", "empty")
    headers.add("sec-fetch-mode", "same-origin")
    headers.add("sec-fetch-site", "same-origin")
    headers.add("pragma", "no-cache")
    headers.add("cache-control", "no-cache")
    headers.add("te", "trailers")
    return headers.build()
}
