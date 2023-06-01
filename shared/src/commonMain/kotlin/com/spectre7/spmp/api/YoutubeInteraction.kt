package com.spectre7.spmp.api

import com.spectre7.spmp.api.Api.Companion.addYtHeaders
import com.spectre7.spmp.api.Api.Companion.getStream
import com.spectre7.spmp.api.Api.Companion.ytUrl
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.resources.getString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.*

class ArtistBrowseResponse(val header: Header) {
    class Header(val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer? = null)
    class MusicImmersiveHeaderRenderer(val subscriptionButton: SubscriptionButton)
    class SubscriptionButton(val subscribeButtonRenderer: SubscribeButtonRenderer)
    class SubscribeButtonRenderer(val subscribed: Boolean)

    fun getSubscribed(): Boolean? = header.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscribed
}

fun isSubscribedToArtist(artist: Artist): Result<Boolean?> {
    check(!artist.is_for_item)

    val request: Request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody("""{ "browseId": "${artist.id}" }"""))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val parsed: ArtistBrowseResponse = Api.klaxon.parse(stream)!!
    stream.close()

    return Result.success(parsed.getSubscribed())
}

fun subscribeOrUnsubscribeArtist(artist: Artist, subscribe: Boolean): Result<Any> {
    check(Api.ytm_authenticated)

    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/subscription/${if (subscribe) "subscribe" else "unsubscribe"}")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody("""
            {
                "channelIds": ["${artist.subscribe_channel_id}"]
            }
        """))
        .build()

    return Api.request(request)
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
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody("""
            {
                "videoId": "$id"
            }
        """))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val parsed: PlayerLikeResponse = Api.klaxon.parse(stream)!!
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
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody("""
            {
                "target": { "videoId": "$id" }
            }
        """))
        .build()

    val result = Api.request(request)
    result.getOrNull()?.close()
    return result
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
            .url("https://music.youtube.com/youtubei/v1/player?key=${getString("yt_i_api_key")}")
            .post(Api.getYoutubeiRequestBody(
                """{ "videoId": "$id" }""",
                context = if (alt) Api.Companion.YoutubeiContextType.ALT else Api.Companion.YoutubeiContextType.BASE
            ))
            .addYtHeaders()
            .build()
    }

    var result = Api.request(buildRequest(true))
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrow().body!!.charStream()
    val data: PlaybackTrackingRepsonse = Api.klaxon.parse(stream)!!
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
        .addYtHeaders()
        .build()

    result = Api.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    return Result.success(Unit)
}
