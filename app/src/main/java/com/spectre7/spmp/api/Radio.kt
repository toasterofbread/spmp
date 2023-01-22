package com.spectre7.spmp.api

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.spectre7.spmp.model.MediaItem
import okhttp3.Request
import java.util.zip.GZIPInputStream

data class RadioItem(val id: String, val browse_endpoints: List<MediaItem.BrowseEndpoint>)

fun getSongRadio(id: String): Result<List<RadioItem>> {
    val request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/next")
        .header("accept", "*/*")
        .header("accept-encoding", "gzip, deflate")
        .header("content-encoding", "gzip")
        .header("origin", "https://music.youtube.com")
        .header("X-Goog-Visitor-Id", "CgtUYXUtLWtyZ3ZvTSj3pNWaBg%3D%3D")
        .header("Content-type", "application/json")
        .header("Cookie", "CONSENT=YES+1")
        .post(getYoutubeiRequestBody(
        """
        {
            "enablePersistentPlaylistPanel": true,
            "isAudioOnly": true,
            "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
            "videoId": "$id",
            "playlistId": "RDAMVM$id",
            "watchEndpointMusicSupportedConfigs": {
                "watchEndpointMusicConfig": {
                    "hasPersistentPlaylistPanel": true,
                    "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                }
            }
        }
        """
        ))
        .build()
    
    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result.failure(response)
    }

    class Run(val navigationEndpoint: NavigationEndpoint? = null)
    class LongBylineText(val runs: List<Run>)
    class PlaylistPanelVideoRenderer(val videoId: String, val longBylineText: LongBylineText)
    class RadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer)

    fun <T> JsonObject.first(): T {
        return values.first() as T
    }

    val radio = klaxon.parseJsonObject(GZIPInputStream(response.body!!.byteStream()).reader())
        .obj("contents")!!
        .first<JsonObject>()  // singleColumnMusicWatchNextResultsRenderer
        .first<JsonObject>() // tabbedRenderer
        .first<JsonObject>() // watchNextTabbedResultsRenderer
        .first<JsonArray<JsonObject>>() // tabs
        .first() // 0
        .first<JsonObject>() // tabRenderer
        .obj("content")!!
        .first<JsonObject>() // musicQueueRenderer
        .first<JsonObject>() // content
        .first<JsonObject>() // playlistPanelRenderer
        .array<JsonObject>("contents")!!
    
    return Result.success(List(radio.size - 1) { i ->
        val item = klaxon.parseFromJsonObject<RadioItem>(radio[i + 1])!!

        val browse_endpoints = mutableListOf<MediaItem.BrowseEndpoint>()
        for (run in item.playlistPanelVideoRenderer.longBylineText.runs) {
            if (run.navigationEndpoint != null) {
                browse_endpoints.add(MediaItem.BrowseEndpoint(
                    run.navigationEndpoint.browseEndpoint!!.browseId,
                    run.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType
                ))
            }
        }

        RadioItem(item.playlistPanelVideoRenderer.videoId, browse_endpoints)
    })
}