package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Artist
import com.spectre7.utils.getString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
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

private data class PlaybackTrackingRepsonse(
    val playbackTracking: PlaybackTracking
) {
    val playback_url: String get() = playbackTracking.videostatsPlaybackUrl.baseUrl

    data class PlaybackTracking(
        val videostatsPlaybackUrl: TrackingUrl
    )
    data class TrackingUrl(val baseUrl: String)
}

fun markSongAsWatched(id: String): Result<Any> {
    fun buildRequest(alt: Boolean): Request {
        return Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player?key=${getString(R.string.yt_i_api_key)}")
            .post(DataApi.getYoutubeiRequestBody(body=
                """{
                    "videoId": "$id"
                }""", alt))
            .headers(DataApi.getYTMHeaders())
            .build()
    }

    var result = DataApi.request(buildRequest(true))
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val data: PlaybackTrackingRepsonse = DataApi.klaxon.parse(stream)!!
    stream.close()

    val playback_data = data.playback_url.toHttpUrl()

    val cpn_alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
    val cpn = (0 until 16).map { cpn_alphabet[Random().nextInt(256) and 63] }.joinToString("")

    val playback_url = playback_data.newBuilder()
        .setQueryParameter("ver", "2")
        .setQueryParameter("c", "WEB_REMIX")
        .setQueryParameter("cpn", cpn)
        .build()

    val request = Request.Builder()
        .url(playback_url)
        .headers(DataApi.getYTMHeaders())
        .build()

    result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    return Result.success(Unit)
}
