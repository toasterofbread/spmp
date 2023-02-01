package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.model.Artist
import okhttp3.Request

fun isSubscribedToArtist(artist: Artist): Result<Boolean?> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/browse")
        .headers(getYTMHeaders())
        .post(getYoutubeiRequestBody("""
                    {
                        "browseId": "${artist.id}"
                    }
                """))
        .build()

    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    val stream = response.body!!.charStream()
    val reader = JsonReader(stream)

    var ret: Result<Boolean?>? = null

    reader.beginObject()
    next(reader, "header", false) {
        next(reader, null, false) {
            next(reader, "subscriptionButton", false, true) {
                next(reader, "subscribeButtonRenderer", false) {
                    next(reader, "subscribed", null) {
                        ret = Result.success(reader.nextBoolean())
                    }
                }
            }
        }
    }
    reader.endObject()

    stream.close()
    reader.close()

    return ret ?: Result.success(null)
}

fun subscribeOrUnsubscribeArtist(artist: Artist, subscribe: Boolean): Result<Unit> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/subscription/${if (subscribe) "subscribe" else "unsubscribe"}")
        .headers(getYTMHeaders())
        .post(getYoutubeiRequestBody("""
            {
                "channelIds": ["${artist.subscribe_channel_id}"]
            }
        """))
        .build()

    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    return Result.success(Unit)
}
