package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.BuildConfig
import com.spectre7.spmp.MainActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

const val DATA_API_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

fun <T> Result.Companion.failure(response: Response): Result<T> {
    return failure(RuntimeException("${response.message}: ${response.body?.string()} (${response.code})"))
}
fun <I, O> Result<I>.cast(): Result<O> {
    return Result.failure(exceptionOrNull()!!)
}
val <T> Result<T>.data get() = getOrThrowHere()

fun <T> Result<T>.getOrThrowHere(): T {
    if (isFailure) {
        throw Exception(exceptionOrNull()!!)
    }
    return getOrThrow()
}

class DataApi {

    companion object {
        private val client: OkHttpClient = OkHttpClient()

        val klaxon: Klaxon = Klaxon()

        private lateinit var youtubei_base_context: JsonObject
        private lateinit var youtubei_alt_context: JsonObject
        private lateinit var youtubei_headers: Headers

        fun request(request: Request, allow_fail_response: Boolean = false): Result<Response> {
            try {
                val response = client.newCall(request).execute()
                if (!allow_fail_response && !response.isSuccessful) {
                    return Result.failure(response)
                }
                return Result.success(response)
            }
            catch (e: Throwable) {
                return Result.failure(e)
            }
        }

        fun initialise() {
            youtubei_base_context = klaxon.parseJsonObject("""
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
            """.reader())

            youtubei_alt_context = klaxon.parseJsonObject("""
                {
                    "context": {
                        "client":{
                            "hl": "${MainActivity.ui_language}",
                            "platform": "DESKTOP",
                            "clientName": "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                            "clientVersion": "2.0",
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
            """.reader())

            youtubei_headers = Headers.Builder()
                .add("user-agent", DATA_API_USER_AGENT)
                .add("accept", "*/*")
                .add("accept-language", "en")
                .add("content-type", "application/json")
                .add("x-goog-visitor-id", "CgtTeVhTTl94clQ2YyimgumeBg%3D%3D")
                .add("x-youtube-client-name", "67")
                .add("x-youtube-client-version", "1.20221019.01.00")
                .add("x-goog-authuser", "1")
                .add("x-origin", "https://music.youtube.com")
                .add("origin", "https://music.youtube.com")
                .add("alt-used", "music.youtube.com")
                .add("connection", "keep-alive")
                .add("authorization", "SAPISIDHASH 1677407834_47421e2bb133ac7d02e4ca4a5af50403715c3244")
                .add("cookie", BuildConfig.TESTING_COOKIE) // TODO Config
                .add("sec-fetch-dest", "empty")
                .add("sec-fetch-mode", "same-origin")
                .add("sec-fetch-site", "same-origin")
                .add("pragma", "no-cache")
                .add("cache-control", "no-cache")
                .add("te", "trailers")
                .build()

            NewPipe.init(object : Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                    val httpMethod: String = request.httpMethod()
                    val url: String = request.url()
                    val headers: Map<String, List<String>> = request.headers()
                    val dataToSend: ByteArray? = request.dataToSend()
                    var requestBody: RequestBody? = null
                    if (dataToSend != null) {
                        requestBody = RequestBody.create(null, dataToSend)
                    }
                    val requestBuilder = Request.Builder()
                        .method(httpMethod, requestBody).url(url)
                        .addHeader("User-Agent", DATA_API_USER_AGENT)

                    for ((headerName, headerValueList) in headers) {
                        if (headerValueList.size > 1) {
                            requestBuilder.removeHeader(headerName)
                            for (headerValue in headerValueList) {
                                requestBuilder.addHeader(headerName, headerValue)
                            }
                        } else if (headerValueList.size == 1) {
                            requestBuilder.header(headerName, headerValueList[0])
                        }
                    }

                    val response = DataApi.request(requestBuilder.build(), true).getOrThrowHere()
                    if (response.code == 429) {
                        response.close()
                        throw ReCaptchaException("reCaptcha Challenge requested", url)
                    }
                    val body = response.body
                    var responseBodyToReturn: String? = null
                    if (body != null) {
                        responseBodyToReturn = body.string()
                    }
                    val latestUrl = response.request.url.toString()
                    return org.schabi.newpipe.extractor.downloader.Response(response.code,
                        response.message,
                        response.headers.toMultimap(),
                        responseBodyToReturn,
                        latestUrl)
                }
            })
        }

        internal fun getYTMHeaders(): Headers {
            return youtubei_headers
        }

        internal fun getYoutubeiRequestBody(body: String? = null, alt: Boolean = false): RequestBody {
            val context = if (alt) youtubei_alt_context else youtubei_base_context
            val final_body = if (body != null) context + klaxon.parseJsonObject(body.reader()) else context
            return klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
        }
    }
}

//fun getApiAuthHeaders(callback: (header: Headers) -> Unit) {
//    getAuthToken { token ->
//        callback(Headers.Builder().add("Authorization", "Bearer $token").build())
//    }
//}
//
//fun getAuthToken(callback: (String) -> Unit) {
//    val auth_state = MainActivity.auth_state
//    val auth_service = MainActivity.auth_service
//
//    fun onFinished() {
//        auth_state.performActionWithFreshTokens(auth_service) { token: String?, id: String?, exception: AuthorizationException? ->
//            if (exception != null) {
//                throw exception
//            }
//            callback(token!!)
//        }
//    }
//
//    fun requestToken() {
//        auth_service.performTokenRequest(
//            auth_state.lastAuthorizationResponse!!.createTokenExchangeRequest()
//        ) { response, exception ->
//            if (exception != null) {
//                throw exception
//            }
//
//            auth_state.update(response, null)
//            MainActivity.saveAuthState()
//
//            onFinished()
//        }
//    }
//
//    if (auth_state.refreshToken == null) {
//        MainActivity.startAuthLogin { exception ->
//            if (exception != null) {
//                throw exception
//            }
//            requestToken()
//        }
//        return
//    }
//
//    if (auth_state.needsTokenRefresh) {
//        requestToken()
//        return
//    }
//
//    onFinished()
//}
