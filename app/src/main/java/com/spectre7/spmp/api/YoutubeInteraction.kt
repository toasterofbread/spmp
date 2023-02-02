package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.model.Artist
import okhttp3.Request

fun isSubscribedToArtist(artist: Artist): DataApi.Result<Boolean?> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/browse")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""
                    {
                        "browseId": "${artist.id}"
                    }
                """))
        .build()

    val response = DataApi.client.newCall(request).execute()
    if (response.code != 200) {
        return DataApi.Result.failure(response)
    }

    val stream = response.body!!.charStream()
    val reader = JsonReader(stream)

    var ret: DataApi.Result<Boolean?>? = null

    reader.beginObject()
    reader.next("header", false) {
        reader.next(null, false) {
            reader.next("subscriptionButton", false, true) {
                reader.next("subscribeButtonRenderer", false) {
                    reader.next("subscribed", null) {
                        ret = DataApi.Result.success(reader.nextBoolean())
                    }
                }
            }
        }
    }
    reader.endObject()

    stream.close()
    reader.close()

    return ret ?: DataApi.Result.success(null)
}

fun subscribeOrUnsubscribeArtist(artist: Artist, subscribe: Boolean): DataApi.Result<Unit> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/subscription/${if (subscribe) "subscribe" else "unsubscribe"}")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""
            {
                "channelIds": ["${artist.subscribe_channel_id}"]
            }
        """))
        .build()

    val response = DataApi.client.newCall(request).execute()
    if (response.code != 200) {
        return DataApi.Result.failure(response)
    }

    return DataApi.Result.success(Unit)
}
