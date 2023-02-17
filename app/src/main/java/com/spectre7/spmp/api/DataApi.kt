package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.spectre7.spmp.BuildConfig
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.model.MediaItem
import net.openid.appauth.AuthorizationException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val DATA_API_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:105.0) Gecko/20100101 Firefox/105.0"

fun <T> Result.Companion.failure(response: Response): Result<T> {
    return failure(RuntimeException("${response.message}: ${response.body!!.string()}"))
}
fun <I, O> Result<I>.cast(): Result<O> {
    require(!isSuccess)
    return Result.failure(exceptionOrNull()!!)
}
val <T> Result<T>.data get() = getOrThrow()

class DataApi {

    companion object {
        private val client: OkHttpClient = OkHttpClient()

        val klaxon: Klaxon = Klaxon()

        private lateinit var youtubei_base_context: JsonObject
        private lateinit var youtubei_alt_context: JsonObject
        private lateinit var youtubei_headers: Headers

        private val failed = mutableListOf<String>()

        fun request(request: Request, allow_fail_response: Boolean = false, fail: Boolean = true): Result<Response> {
            if (fail) {
                val key = Throwable().stackTrace[2].let { trace -> "${trace.fileName}:${trace.lineNumber}" }
                if (!failed.contains(key)) {
                    failed.add(key)
                    return Result.failure(RuntimeException("Failed (testing)"))
                }
            }

            try {
                val response = client.newCall(request).execute()
                if (!allow_fail_response && response.code != 200) {
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
                .add("authorization", "SAPISIDHASH 1675248141_d44fbb5137d2b0b742935593e96ccb67b3b5c37b")
                .add("cookie", BuildConfig.TESTING_COOKIE) // TODO Config
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
