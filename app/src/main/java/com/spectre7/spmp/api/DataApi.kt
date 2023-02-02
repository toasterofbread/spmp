package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.MainActivity
import net.openid.appauth.AuthorizationException
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

const val DATA_API_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

class DataApi {

    companion object {
        internal val client: OkHttpClient = OkHttpClient()
        internal val klaxon: Klaxon = Klaxon()

        private lateinit var youtubei_base_context: JsonObject
        private lateinit var youtubei_headers: Headers

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
                .add("authorization", "SAPISIDHASH 1675248141_d44fbb5137d2b0b742935593e96ccb67b3b5c37b")
                .add("cookie", "***REMOVED***")
                .add("sec-fetch-dest", "empty")
                .add("sec-fetch-mode", "same-origin")
                .add("sec-fetch-site", "same-origin")
                .add("pragma", "no-cache")
                .add("cache-control", "no-cache")
                .add("te", "trailers")
                .build()
        }

        internal fun getYTMHeaders(): Headers {
            return youtubei_headers
        }

        internal fun getYoutubeiRequestBody(body: Any? = null): RequestBody {
            val final_body = if (body != null) youtubei_base_context + klaxon.toJsonObject(body) else youtubei_base_context
            return klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
        }

        internal fun getYoutubeiRequestBody(body: String): RequestBody {
            val final_body = youtubei_base_context + klaxon.parseJsonObject(body.reader())
            return klaxon.toJsonString(final_body).toRequestBody("application/json".toMediaType())
        }
    }

    class Result<T> private constructor() {

        private var _data: T? = null
        private var _exception: Throwable? = null

        val nullable_data: T? get() = _data
        val data: T get() = _data!!
        val exception: Throwable get() = _exception!!
        val success: Boolean get() = _exception == null

        fun getDataOrThrow(): T {
            return getNullableDataOrThrow()!!
        }

        fun getNullableDataOrThrow(): T? {
            if (!success) {
                throw exception
            }
            return nullable_data
        }

        companion object {
            fun <T> success(data: T): Result<T> {
                return Result<T>().also {
                    it._data = data
                }
            }

            fun <T> failure(exception: Throwable): Result<T> {
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
