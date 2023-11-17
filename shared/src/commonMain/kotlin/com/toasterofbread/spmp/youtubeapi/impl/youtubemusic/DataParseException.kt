package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.fromJson
import com.toasterofbread.spmp.youtubeapi.getReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.Reader

val REQUEST_HEADERS_TO_REMOVE = listOf("authorization", "cookie")
private val YOUTUBE_JSON_DATA_KEYS_TO_REMOVE = listOf("responseContext", "trackingParams", "clickTrackingParams", "serializedShareEntity", "serializedContextData", "loggingContext")

class DataParseException(cause: Throwable? = null, private val cause_request: Request?, message: String? = null, private val causeDataProvider: suspend () -> Result<String>): RuntimeException(message, cause) {
    private var cause_data: String? = null
    suspend fun getCauseResponseData(): Result<String> {
        val data = cause_data
        if (data != null) {
            return Result.success(data)
        }

        val result = causeDataProvider()
        cause_data = result.getOrNull()
        return result
    }

    fun getCauseRequestUrl(): String? {
        return cause_request?.url?.toString()
    }

    fun getCauseRequestData(): String? {
        if (cause_request?.body == null) {
            return null
        }

        val buffer = Buffer()
        cause_request.body!!.writeTo(buffer)
        return buffer.readUtf8()
    }

    fun getCauseRequestHeaders(): String? {
        if (cause_request == null) {
            return null
        }

        val headers = cause_request.headers.toMutableList()

        val i = headers.iterator()
        while (i.hasNext()) {
            val header = i.next()
            if (REQUEST_HEADERS_TO_REMOVE.any { it == header.first.lowercase() }) {
                i.remove()
            }
        }

        return headers.toString()
    }

    companion object {
        fun ofYoutubeJsonRequest(
            request: Request,
            api: YoutubeApi,
            message: String? = null,
            cause: Throwable? = null,
            getResponseStream: (Response) -> Reader = { it.getReader(api) },
            keys_to_remove: List<String> = YOUTUBE_JSON_DATA_KEYS_TO_REMOVE
        ) = DataParseException(
            cause,
            request,
            message
        ) {
            runCatching {
                val json_string: String = withContext(Dispatchers.IO) {
                    val stream = getResponseStream(api.performRequest(request).getOrThrow())
                    stream.use {
                        it.readText()
                    }
                }

                try {
                    val json_object = api.gson.toJsonTree(api.gson.fromJson<Map<String, Any?>>(json_string)).asJsonObject

                    // Remove unneeded keys from JSON object
                    val items: MutableList<JsonObject> = mutableListOf(json_object)

                    while (items.isNotEmpty()) {
                        val obj = items.removeLast()

                        for (key in keys_to_remove) {
                            obj.remove(key)
                        }

                        for (key in obj.keySet()) {
                            val value: JsonElement = obj.get(key)

                            if (value.isJsonObject) {
                                items.add(value.asJsonObject)
                            } else if (value.isJsonArray) {
                                items.addAll(
                                    value.asJsonArray.mapNotNull { item ->
                                        if (item.isJsonObject) item.asJsonObject
                                        else null
                                    }
                                )
                            }
                        }
                    }

                    return@runCatching api.gson.toJson(json_object)
                }
                catch (_: Throwable) {
                    return@runCatching json_string
                }
            }
        }
    }
}
