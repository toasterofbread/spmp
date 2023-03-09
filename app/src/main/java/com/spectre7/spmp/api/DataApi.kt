package com.spectre7.spmp.api

import com.beust.klaxon.*
import com.spectre7.spmp.BuildConfig
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.utils.getString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.lang3.text.StrSubstitutor
import org.apache.commons.text.StringSubstitutor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse

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
        val user_agent: String get() = getString(R.string.ytm_user_agent)

        private val enum_converter = object : Converter {
            override fun canConvert(cls: Class<*>): Boolean {
                return cls.isEnum
            }

            override fun fromJson(jv: JsonValue): Enum<*>? {
                val cls = jv.propertyClass
                if (cls !is Class<*> || !cls.isEnum) {
                    throw IllegalArgumentException("Could not convert $jv to enum")
                }

                val name = jv.inside as String? ?: return null
                val field = cls.declaredFields
                    .firstOrNull { it.name == name || it.getAnnotation(Json::class.java)?.name == name }
                    ?: throw IllegalArgumentException("Could not find enum value for $name")

                return field.get(null) as Enum<*>
            }

            override fun toJson(value: Any): String {
                return "\"${(value as Enum<*>).name}\""
            }
        }
        val klaxon: Klaxon get() = Klaxon().converter(enum_converter)

        private lateinit var youtubei_base_context: JsonObject
        private lateinit var youtubei_alt_context: JsonObject
        private lateinit var youtubei_headers: Headers

        fun request(request: Request, allow_fail_response: Boolean = false): Result<Response> {
//            val new_request = request.newBuilder().url(request.url.newBuilder().addQueryParameter("prettyPrint", "false").build()).build()
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
            val context_substitutor = StringSubstitutor(
                mapOf(
                    "user_agent" to user_agent,
                    "hl" to MainActivity.ui_language
                ),
                "\${",
                "}"
            )

            youtubei_base_context = klaxon.parseJsonObject(
                context_substitutor.replace(getString(R.string.ytm_context)).reader()
            )
            youtubei_alt_context = klaxon.parseJsonObject(
                context_substitutor.replace(getString(R.string.ytm_context_alt)).reader()
            )

            val headers_builder = Headers.Builder().apply {
                add("cookie", BuildConfig.TESTING_COOKIE)
                add("user-agent", user_agent)
            }

            val headers = MainActivity.resources.getStringArray(R.array.ytm_headers)
            var i = 0
            while (i < headers.size) {
                val key = headers[i++]
                val value = headers[i++]
                headers_builder.add(key, value)
            }

            youtubei_headers = headers_builder.build()

            NewPipe.init(object : Downloader() {
                override fun execute(request: NewPipeRequest): NewPipeResponse {
                    val url = request.url()
                    val request_body: RequestBody? = request.dataToSend()?.let {
                        it.toRequestBody(null, 0, it.size)
                    }

                    val request_builder = Request.Builder()
                        .method(request.httpMethod(), request_body).url(url)
                        .addHeader("User-Agent", user_agent)

                    for ((headerName, headerValueList) in request.headers()) {
                        if (headerValueList.size > 1) {
                            request_builder.removeHeader(headerName)
                            for (headerValue in headerValueList) {
                                request_builder.addHeader(headerName, headerValue)
                            }
                        } else if (headerValueList.size == 1) {
                            request_builder.header(headerName, headerValueList[0])
                        }
                    }

                    val response = request(request_builder.build(), true).getOrThrowHere()
                    if (response.code == 429) {
                        response.close()
                        throw ReCaptchaException("reCaptcha Challenge requested", url)
                    }

                    return NewPipeResponse(response.code,
                        response.message,
                        response.headers.toMultimap(),
                        response.body?.string(),
                        response.request.url.toString()
                    )
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
