package com.spectre7.spmp.api

import com.beust.klaxon.Klaxon
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import com.chaquo.python.Python

const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

class Result<T> private constructor() {

    private var _data: T? = null
    private var _exception: Exception? = null

    val data: T get() = _data!!
    val exception: Exception get() = _exception!!
    val success: Boolean get() = _exception == null

    fun getDataOrThrow(): T {
        if (!success) {
            throw exception
        }
        return data
    }

    companion object {
        fun <T> success(data: T): Result<T> {
            return Result<T>().also {
                it._data = data
            }
        }

        fun <T> failure(exception: Exception): Result<T> {
            return Result<T>().also {
                it._exception = exception
            }
        }

        fun <T> failure(response: Response): Result<T> {
            return Result<T>().also {
                it._exception = RuntimeException("${response.message}: ${response.body!!.string()}")
            }
        }
    }
}

internal val client = OkHttpClient()
internal val klaxon: Klaxon = Klaxon()
//internal val ytd = Python.getInstance().getModule("yt_dlp").callAttr("YoutubeDL")

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
