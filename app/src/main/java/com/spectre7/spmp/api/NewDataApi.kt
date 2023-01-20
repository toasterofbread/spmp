package com.spectre7.spmp.api

import com.beust.klaxon.Klaxon
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import com.chaquo.python.Python
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

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
    headers.add("cookie", "PREF=tz=Europe.London&f6=40000400&f5=30000&f7=1&repeat=NONE&volume=60&f3=8&autoplay=true; __Secure-3PSIDCC=AIKkIs1HzUCBLGiDGnM7upTqnkIuJFGKsO09NZKhr-6HF3VwRiHeeGNYNNo2Lhk1dduN8P27ZXy9; s_gl=GB; LOGIN_INFO=AFmmF2swRAIgZ035p6PjI532M15GF53l6UlfPen5HwkDpu7ZEle29vACIGNtXbi8xtRJ7Y8pT1tqah7SqKR_GnzcwOryhVxgUeXF:QUQ3MjNmel9JRGpUeGowRmpmM3picUpNalFleGFibkRYV1dubXdXenQyam9Ib3RWY3MtTVhUZmxDb1pFMUhoVElZUEdqS2JPcW5kT0dpaTN3emRUUUo5SU9ZRFFyVnlyZW9aYlF5dmVCQ1puYjRMRkd4OXFXb0s2Nlk4a1NtNVlfb3QydENNZDJ4bWlfSDVlZnZONHNSRk95dGxyeWZpV1dn; CONSENT=PENDING+281; __Secure-YEC=Cgt2ZlpYajN4dVdLZyjSmpuaBg%3D%3D; SIDCC=AIKkIs2pSVZXshn1zeCzrzL3mlIC6VAAgWfoULSkTBWcrht_9EMrkr8D9EQZYcCiKRDa8ejUTw; __Secure-1PSIDCC=AIKkIs0_imP3kQ3wfQWUyWhD_IKDL_QYExRxV4Ou7EpSO75uDq-4J6t3VhJOJGx1dM0zGdI3cpc; VISITOR_INFO1_LIVE=uN4trE-9SW8; wide=1; __Secure-3PSID=PwhomEhQTZ77kJmEhSDm0D3ui-d5WWiRyRhTGsP7BAyxF_dlxCTncdVXtBbp04fUJlDtPw.; __Secure-3PAPISID=qMwfMfR_YyoT3NGb/AzFZpb4NqFXud3Nwr; YSC=8LddSzq-F84; SID=PwhomEhQTZ77kJmEhSDm0D3ui-d5WWiRyRhTGsP7BAyxF_dlBdrU3vl6GPsCr1ylPTr4KQ.; __Secure-1PSID=PwhomEhQTZ77kJmEhSDm0D3ui-d5WWiRyRhTGsP7BAyxF_dlKcjSgx1HUrI2I9zInQMtxw.; HSID=Aco1DxTh4I1ySKm8Q; SSID=A1vzE52cm5ko7nyff; APISID=W6YIE8FP4wiEER0O/AbLbtnGAFqeU0gqza; SAPISID=qMwfMfR_YyoT3NGb/AzFZpb4NqFXud3Nwr; __Secure-1PAPISID=qMwfMfR_YyoT3NGb/AzFZpb4NqFXud3Nwr")
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
                "client":{
                    "hl": "${MainActivity.ui_language}",
                    "platform": "DESKTOP",
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20221031.00.00-canary_control",
                    "userAgent": "$USER_AGENT",
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
            """
        )
    }


    val final_body = if (body != null) base_context + klaxon.parseJsonObject(body) else base_context
    return klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
}

internal fun convertBrowseId(browse_id: String): Result<String> {
    if (!browse_id.startsWith("MPREb_")) {
        return Result.success(browse_id)
    }

    val request = Request.Builder()
        .url("https://music.youtube.com/browse/$browse_id")
        .header("Cookie", "CONSENT=YES+1")
        .header("User-Agent", USER_AGENT)
        .build()

    val result = client.newCall(request).execute()
    if (result.code != 200) {
        return Result.failure(result)
    }

    val text = result.body!!.string()

    val target = "urlCanonical\\x22:\\x22https:\\/\\/music.youtube.com\\/playlist?list\\x3d"
    val start = text.indexOf(target) + target.length
    val end = text.indexOf("\\", start + 1)

    return text.substring(start, end)
}