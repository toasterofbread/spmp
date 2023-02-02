package com.spectre7.spmp.api

import com.spectre7.spmp.model.MediaItem
import okhttp3.Request
import java.util.zip.GZIPInputStream

data class RadioData(val items: MutableList<RadioItem>, private var continuation: String?) {
    data class Item(val id: String, val browse_endpoints: List<MediaItem.BrowseEndpoint>)

    val has_continuation: Boolean get() = continuation != null

    fun getContinuation() {
        TODO()
    }
}

private data class YtRadioResponse(
    val contents: Contents
) {
    class Contents(val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer)
    class SingleColumnMusicWatchNextResultsRenderer(val tabbedRenderer: TabbedRenderer)
    class TabbedRenderer(val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer)
    class WatchNextTabbedResultsRenderer(val tabs: List<Tab>)
    class Tab(val tabRenderer: TabRenderer)
    class TabRenderer(val content: Content)
    class Content(val musicQueueRenderer: MusicQueueRenderer)
    class MusicQueueRenderer(val content: MusicQueueRendererContent)
    class MusicQueueRendererContent(val playlistPanelRenderer: PlaylistPanelRenderer)
    class PlaylistPanelRenderer(val contents: List<ResponseRadioItem>, val continuations: List<Continuation>)
    class ResponseRadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer)
    class PlaylistPanelVideoRenderer(val videoId: String, val longBylineText: LongBylineText)
    class LongBylineText(val runs: List<TextRun>)
    class Continuation(val nextContinuationData: ContinuationData)
    class ContinuationData(val continuation: String)
}

fun getSongRadio(id: String): Result<RadioData> {
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

    val stream = response.body!!.charStream()

    val radio: YtRadioResponse = klaxon.parse(stream)
        .contents
        .singleColumnMusicWatchNextResultsRenderer
        .tabbedRenderer
        .watchNextTabbedResultsRenderer
        .tabs
        .first()
        .tabRenderer
        .content
        .musicQueueRenderer
        .content
        .playlistPanelRenderer
        .contents
    
    stream.close()

    return Result.success(
        RadioData(
            radio.map { item ->
                val browse_endpoints = mutableListOf<MediaItem.BrowseEndpoint>()
                for (run in item.playlistPanelVideoRenderer.longBylineText.runs) {
                    if (run.navigationEndpoint != null) {
                        browse_endpoints.add(MediaItem.BrowseEndpoint(
                            run.navigationEndpoint.browseEndpoint!!.browseId,
                            run.navigationEndpoint.browseEndpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType
                        ))
                    }
                }

                RadioData.Item(item.playlistPanelVideoRenderer.videoId, browse_endpoints)
            }.toMutableList(),
            TODO()
        )
    )
}