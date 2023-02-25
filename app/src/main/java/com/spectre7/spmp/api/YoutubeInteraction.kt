package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Artist
import com.spectre7.utils.getString
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.IOException
import java.util.*

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

    val stream = result.getOrThrowHere().body!!.charStream()
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

private data class WatchtimeRepsonse(
    val playbackTracking: PlaybackTracking
) {
    val playback_url: String get() = playbackTracking.videostatsPlaybackUrl.baseUrl
    val watchtime_url: String get() = playbackTracking.videostatsWatchtimeUrl.baseUrl

    data class PlaybackTracking(
        val videostatsWatchtimeUrl: TrackingUrl,
        val videostatsPlaybackUrl: TrackingUrl
    )
    data class TrackingUrl(val baseUrl: String)
}

//fun markSongAsWatched(_id: String): Result<Any> {
//    // https://music.youtube.com/watch?v=k0g04t7ZeSw
//    val id = "k0g04t7ZeSw"
//
//    fun buildRequest(alt: Boolean): Request {
//        return Request.Builder()
//            .url("https://music.youtube.com/youtubei/v1/player?key=${getString(R.string.yt_i_api_key)}")
//            .post(DataApi.getYoutubeiRequestBody("""{
//                "videoId": "$id",
//                "playlistId": null
//            }""", alt))
//            .build()
//    }
//
//    val result = DataApi.request(buildRequest(false))
//
//    val stream = result.getOrThrow().body!!.charStream()
//    val data: WatchtimeRepsonse = DataApi.klaxon.parse(stream)!!
//    stream.close()
//
//    val cpn_alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
//    val cpn = (1..16).map { cpn_alphabet[Random().nextInt(256) and 63] }.joinToString("")
//
//    val playback_data = data.watchtime_url.toHttpUrl()
//    val video_length = ((playback_data.queryParameter("len")?.toFloat() ?: 1.5f) - 1).toString()
//
//    val playback_url = playback_data.newBuilder()
//        .host("www.youtube.com")
//        .setQueryParameter("ver", "2")
//        .setQueryParameter("cpn", cpn)
//        .setQueryParameter("cmt", video_length)
//        .setQueryParameter("el", "detailpage")
//        .setQueryParameter("st", "0")
//        .setQueryParameter("et", video_length)
//        .build()
//
//    val request = Request.Builder()
//        .url(playback_url)
//        .headers(DataApi.getYTMHeaders())
//        .build()
//
//    println(DataApi.request(request).getOrThrowHere().body?.string())
//
//    TODO()
//}
