package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.MainActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

const val DATA_API_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

internal val client = OkHttpClient()
internal val klaxon: Klaxon = Klaxon()

private var base_context: JsonObject? = null

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

internal fun getYTMHeaders(): Headers {
    val headers = Headers.Builder()
    headers.add("user-agent", DATA_API_USER_AGENT)
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

internal fun getYoutubeiRequestBody(body: String? = null): RequestBody {
    if (base_context == null) {
        base_context = klaxon.parseJsonObject(
            """
            {
                "context": {
                    "client":{
                        "hl": "${MainActivity.ui_language}",
                        "platform": "DESKTOP",
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20221031.00.00-canary_control",
                        "userAgent": "$DATA_API_USER_AGENT",
                        "acceptHeader": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    },
                    "user":{
                        "lockedSafetyMode": false
                    },
                    "request":{
                        "useSsl": true,
                        "internalExperimentFlags": [],
                        "consistencyTokenJars": []
                    }
                }
            }
            """.reader()
        )
    }


    val final_body = if (body != null) base_context!! + klaxon.parseJsonObject(body.reader()) else base_context
    return klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
}

internal fun convertBrowseId(browse_id: String): Result<String> {
    if (!browse_id.startsWith("MPREb_")) {
        return Result.success(browse_id)
    }

    val request = Request.Builder()
        .url("https://music.youtube.com/browse/$browse_id")
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", DATA_API_USER_AGENT)
        .build()

    val result = client.newCall(request).execute()
    if (result.code != 200) {
        return Result.failure(result)
    }

    val text = result.body!!.string()

    val target = "urlCanonical\\x22:\\x22https:\\/\\/music.youtube.com\\/playlist?list\\x3d"
    val start = text.indexOf(target) + target.length
    val end = text.indexOf("\\", start + 1)

    return Result.success(text.substring(start, end))
}