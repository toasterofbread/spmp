package com.spectre7.spmp.api

import com.beust.klaxon.JsonObject
import okhttp3.Request

data class RadioItem(val id: String, val browse_id: String, val browse_type: String)

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

    class Run(val navigationEndpoint: NavigationEndpoint)
    class LongBylineText(val runs: List<Run>)
    class PlaylistPanelVideoRenderer(val videoId: String, val longBylineText: LongBylineText)
    class RadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer)

    fun JsonObject.first(): JsonObject {
        return values.first() as JsonObject
    }
    
    val radio = klaxon.parseJsonObject(response.body!!.charStream())!!
        .obj("contents")!!
        .first()  // singleColumnMusicWatchNextResultsRenderer
        .first() // tabbedRenderer
        .first() // watchNextTabbedResultsRenderer
        .first() // tabs
        .first() // 0
        .first() // tabRenderer
        .obj("content")!!
        .first() // musicQueueRenderer
        .first() // content
        .first() // playlistPanelRenderer
        .array<RadioItem>("contents")!!
    
    return Result.success(List(radio.size) { i ->
        val item = radio[i]
        val browse_endpoint = item.playlistPanelVideoRenderer.longBylineText.runs[0].navigationEndpoint.browseEndpoint
        RadioItem(item.playlistPanelVideoRenderer.videoId, browse_endpoint.browseId, browse_endpoint.browseEndpointContextSupportedConfigs.browseEndpointContextMusicConfig.pageType)
    })
}