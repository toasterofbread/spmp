package com.spectre7.spmp.api

import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

fun getSongRadio(id: String): Result<List<String>> {
    val request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/next")
        .header("accept", "*/*")
        .header("accept-encoding", "gzip, deflate")
        .header("content-encoding", "gzip")
        .header("origin", "https://music.youtube.com")
        .header("X-Goog-Visitor-Id", "CgtUYXUtLWtyZ3ZvTSj3pNWaBg%3D%3D")
        .header("Content-type", "application/json")
        .header("Cookie", "CONSENT=YES+1")
        .post("""
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

            },
            "context" : {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20221023.01.00",
                    "hl": "ja"
                },
                "user": {}
            }
        """.toRequestBody("application/json".toMediaType()))
        .build()
    
    val response = client.newCall(request).execute()
    if (response.code != 200) {
        return Result(null, response)
    }

    class RadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer)
    class PlaylistPanelVideoRenderer(val videoId: String)

    val radio = klaxon.parseJsonObject(response.body!!.charStream())!!
        .obj("contents")!!
        .values.first()!! // singleColumnMusicWatchNextResultsRenderer
        .values.first()!! // tabbedRenderer
        .values.first()!! // watchNextTabbedResultsRenderer
        .values.first()!! // tabs
        .values.first()!! // 0
        .values.first()!! // tabRenderer
        .obj("content")!!
        .values.first()!! // musicQueueRenderer
        .values.first()!! // content
        .values.first()!! // playlistPanelRenderer
        .array<RadioItem>("contents")!!
    
    return Result(List(radio.size) { i ->
        radio[i].playlistPanelVideoRenderer.videoId
    })
}