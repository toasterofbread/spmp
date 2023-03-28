package com.spectre7.spmp.api

import android.util.JsonReader
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Artist
import com.spectre7.utils.getString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.internal.http2.StreamResetException
import java.util.*
import javax.net.ssl.SSLHandshakeException

fun isSubscribedToArtist(artist: Artist): Result<Boolean?> {
    check(!artist.for_song)

    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/browse")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""{ "browseId": "${artist.id}" }"""))
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

private data class PlayerLikeResponse(
    val playerOverlays: PlayerOverlays
) {
    val status: LikeButtonRenderer get() = playerOverlays.playerOverlayRenderer.actions.single().likeButtonRenderer

    class PlayerOverlays(val playerOverlayRenderer: PlayerOverlayRenderer)
    data class PlayerOverlayRenderer(val actions: List<Action>)
    data class Action(val likeButtonRenderer: LikeButtonRenderer)
    data class LikeButtonRenderer(val likeStatus: String, val likesAllowed: Boolean)
}

fun getSongLiked(id: String): Result<Boolean?> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/next")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""
            {
                "videoId": "$id"
            }
        """))
        .build()

    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val parsed: PlayerLikeResponse = DataApi.klaxon.parse(stream)!!
    stream.close()

    return Result.success(when (parsed.status.likeStatus) {
        "LIKE" -> true
        "DISLIKE" -> false
        "INDIFFERENT" -> null
        else -> throw NotImplementedError(parsed.status.likeStatus)
    })
}

fun setSongLiked(id: String, liked: Boolean?): Result<Any> {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/" + when (liked) {
            true -> "like/like"
            false -> "like/dislike"
            null -> "like/removelike"
        })
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""
            {
                "target": { "videoId": "$id" }
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

private const val CPN_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
private fun generateCpn(): String {
    return (0 until 16).map { CPN_ALPHABET[Random().nextInt(256) and 63] }.joinToString("")
}

fun markSongAsWatched(id: String): Result<Any> {
    fun buildRequest(alt: Boolean): Request {
        return Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player?key=${getString(R.string.yt_i_api_key)}")
            .post(DataApi.getYoutubeiRequestBody(
                """{ "videoId": "$id" }""",
                context = if (alt) DataApi.Companion.YoutubeiContextType.ALT else DataApi.Companion.YoutubeiContextType.BASE
            ))
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

    check(data.playback_url.contains("s.youtube.com"))

    val playback_url = data.playback_url.replace("s.youtube.com", "music.youtube.com")
        .toHttpUrl().newBuilder()
        .setQueryParameter("ver", "2")
        .setQueryParameter("c", "WEB_REMIX")
        .setQueryParameter("cpn", generateCpn())
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
