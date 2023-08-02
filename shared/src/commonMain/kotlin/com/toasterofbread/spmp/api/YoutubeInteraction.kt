package com.toasterofbread.spmp.api

import SpMp
import com.toasterofbread.Database
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.SongLikedStatus
import com.toasterofbread.spmp.model.mediaitem.toLong
import com.toasterofbread.utils.lazyAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.Random

class ArtistBrowseResponse(val header: Header) {
    class Header(val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer? = null)
    class MusicImmersiveHeaderRenderer(val subscriptionButton: SubscriptionButton)
    class SubscriptionButton(val subscribeButtonRenderer: SubscribeButtonRenderer)
    class SubscribeButtonRenderer(val subscribed: Boolean)

    fun getSubscribed(): Boolean? = header.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscribed
}

suspend fun isSubscribedToArtist(artist: Artist): Result<Boolean> = withContext(Dispatchers.IO) {
    lazyAssert {
        !artist.IsForItem.get(SpMp.context.database)
    }

    val request: Request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("browseId" to artist.id)))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrow().getStream()
    val parsed: ArtistBrowseResponse = try {
        Api.klaxon.parse(stream)!!
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
    }

    return@withContext Result.success(parsed.getSubscribed() == true)
}

suspend fun subscribeOrUnsubscribeArtist(
    artist: Artist,
    subscribe: Boolean,
    db: Database = SpMp.context.database
): Result<Unit> = withContext(Dispatchers.IO) {
    check(Api.ytm_authenticated)

    val subscribe_channel_id =
        db.artistQueries.subscribeChannelIdById(artist.id).executeAsOneOrNull()?.subscribe_channel_id
            ?: artist.id

    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/subscription/${if (subscribe) "subscribe" else "unsubscribe"}")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(
            mapOf("channelIds" to listOf(subscribe_channel_id))
        ))
        .build()

    return@withContext Api.request(request).cast()
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

suspend fun loadSongLiked(id: String): Result<SongLikedStatus> = withContext(Dispatchers.IO) {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/next")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("videoId" to id)))
        .build()

    val result = Api.request(request)
    val stream = result.getOrNull()?.getStream() ?: return@withContext result.cast()

    val parsed: PlayerLikeResponse = try {
        Api.klaxon.parse(stream)!!
    }
    catch (e: Throwable) {
        return@withContext Result.failure(e)
    }
    finally {
        stream.close()
    }

    return@withContext Result.success(when (parsed.status.likeStatus) {
        "LIKE" -> SongLikedStatus.LIKED
        "DISLIKE" -> SongLikedStatus.DISLIKED
        "INDIFFERENT" -> SongLikedStatus.NEUTRAL
        else -> throw NotImplementedError(parsed.status.likeStatus)
    })
}

suspend fun setSongLiked(song_id: String, liked: SongLikedStatus, db: Database? = null): Result<Unit> = withContext(Dispatchers.IO) {
    val request: Request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/" + when (liked) {
            SongLikedStatus.NEUTRAL -> "like/removelike"
            SongLikedStatus.LIKED -> "like/like"
            SongLikedStatus.DISLIKED -> "like/dislike"
        })
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(
            mapOf("target" to mapOf("videoId" to song_id))
        ))
        .build()

    return@withContext Api.request(request).fold(
        {
            runCatching {
                db?.songQueries?.updatelikedById(liked.toLong(), song_id)
                it.close()
            }
        },
        { Result.failure(it) }
    )
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

private fun buildPlayerRequest(id: String, alt: Boolean): Request {
    return Request.Builder()
        .ytUrl("/youtubei/v1/player")
        .post(Api.getYoutubeiRequestBody(
            mapOf("videoId" to id),
            context = if (alt) Api.Companion.YoutubeiContextType.ALT else Api.Companion.YoutubeiContextType.BASE
        ))
        .addYtHeaders()
        .build()
}

suspend fun markSongAsWatched(id: String): Result<Any> = withContext(Dispatchers.IO) {
    var result = Api.request(buildPlayerRequest(id, false))
    if (result.isFailure) {
        result = Api.request(buildPlayerRequest(id, true))
    }

    val response = result.getOrNull() ?: return@withContext result.cast()

    val stream = response.getStream()
    val data: PlaybackTrackingRepsonse =
        try {
            Api.klaxon.parse(stream)!!
        }
        catch (e: Throwable) {
            return@withContext Result.failure(e)
        }
        finally {
            stream.close()
        }

    check(data.playback_url.contains("s.youtube.com")) { data.playback_url }

    val playback_url = data.playback_url.replace("s.youtube.com", "music.youtube.com")
        .toHttpUrl().newBuilder()
        .setQueryParameter("ver", "2")
        .setQueryParameter("c", "WEB_REMIX")
        .setQueryParameter("cpn", generateCpn())
        .build()

    val request = Request.Builder()
        .url(playback_url)
        .addYtHeaders(include = listOf("cookie", "user-agent"))
        .build()

    return@withContext Api.request(request).unit()
}
