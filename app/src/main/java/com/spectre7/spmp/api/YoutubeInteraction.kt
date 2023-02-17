package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.model.Artist
import okhttp3.Request

fun isSubscribedToArtist(artist: Artist): Result<Boolean?> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/browse")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""
                    {
                        "browseId": "${artist.id}"
                    }
                """))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val reader = JsonReader(stream)

    var ret: Result<Boolean?>? = null

    try {
        reader.beginObject()
        reader.next("header", false) {
            reader.next(null, false) {
                reader.next("subscriptionButton", false, true) {
                    reader.next("subscribeButtonRenderer", false) {
                        reader.next("subscribed", null) {
                            ret = Result.success(reader.nextBoolean())
                        }
                    }
                }
            }
        }
        reader.endObject()
    }
    catch (e: Throwable) {
        throw RuntimeException(artist.toString(), e)
    }

    stream.close()
    reader.close()

    return ret ?: Result.success(null)
}

fun subscribeOrUnsubscribeArtist(artist: Artist, subscribe: Boolean): Result<Any> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/subscription/${if (subscribe) "subscribe" else "unsubscribe"}")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""
            {
                "channelIds": ["${artist.subscribe_channel_id}"]
            }
        """))
        .build()

    return DataApi.request(request)
}
