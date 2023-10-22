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
import java.io.PrintStream
import java.io.PrintWriter
import java.io.Reader

private val YOUTUBE_JSON_DATA_KEYS_TO_REMOVE = listOf("responseContext", "trackingParams", "clickTrackingParams", "serializedShareEntity", "serializedContextData", "loggingContext")

class DataParseException(cause: Throwable? = null, message: String? = null, private val causeDataProvider: suspend () -> Result<String>): RuntimeException(message, cause) {
    private var cause_data: String? = null
    suspend fun getCauseData(): Result<String> {
        val data = cause_data
        if (data != null) {
            return Result.success(data)
        }

        val result = causeDataProvider()
        cause_data = result.getOrNull()
        return result
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
            message
        ) {
            runCatching {
                val json_object: JsonObject = withContext(Dispatchers.IO) {
                    val stream = getResponseStream(api.performRequest(request).getOrThrow())
                    stream.use { reader ->
                        api.gson.toJsonTree(api.gson.fromJson<Map<String, Any?>>(reader)).asJsonObject
                    }
                }

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

                api.gson.toJson(json_object)
            }
        }
    }
}
