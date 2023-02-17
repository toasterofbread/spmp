package com.spectre7.spmp.api

import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import okhttp3.Request
import java.util.zip.GZIPInputStream

class RadioInstance {
    private var song: Song? = null
    private var continuation: String? = null

    val has_continuation: Boolean get() = continuation != null

    fun startNewRadio(song: Song): Result<List<Song>> {
        this.song = song
        continuation = null
        return updateRadio(song.id)
    }

    fun getRadioContinuation(): Result<List<Song>> {
        return updateRadio(song!!.id)
    }

    private fun updateRadio(video_id: String): Result<List<Song>> {
        val result = getSongRadio(video_id, continuation)
        if (result.isFailure) {
            return result.cast()
        }
        continuation = result.getOrThrow().continuation
        return Result.success(result.getOrThrow().items)
    }
}

private data class RadioData(val items: List<Song>, var continuation: String?)

data class YoutubeiNextResponse(
    val contents: Contents
) {
    data class Contents(val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer)
    data class SingleColumnMusicWatchNextResultsRenderer(val tabbedRenderer: TabbedRenderer)
    data class TabbedRenderer(val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer)
    data class WatchNextTabbedResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content? = null)
    data class Content(val musicQueueRenderer: MusicQueueRenderer)
    data class MusicQueueRenderer(val content: MusicQueueRendererContent)
    data class MusicQueueRendererContent(val playlistPanelRenderer: PlaylistPanelRenderer)
    data class PlaylistPanelRenderer(val contents: List<ResponseRadioItem>, val continuations: List<Continuation>? = null)
    data class ResponseRadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null)
    data class PlaylistPanelVideoRenderer(
        val videoId: String,
        val title: TextRuns,
        val longBylineText: TextRuns,
        val menu: Menu
    )
    data class Menu(val menuRenderer: MenuRenderer)
    data class MenuRenderer(val items: List<MenuItem>) {
        fun getArtist(): MenuItem? {
            return items.firstOrNull {
                it.menuNavigationItemRenderer?.icon?.iconType == "ARTIST"
            }
        }
    }
    data class MenuItem(val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null)
    data class MenuNavigationItemRenderer(val icon: MenuIcon, val navigationEndpoint: NavigationEndpoint)
    data class MenuIcon(val iconType: String)
    data class Continuation(val nextContinuationData: ContinuationData? = null, val nextRadioContinuationData: ContinuationData? = null) {
        val data: ContinuationData? get() = nextContinuationData ?: nextRadioContinuationData
    }
    data class ContinuationData(val continuation: String)
}

data class YoutubeiNextContinuationResponse(
    val continuationContents: Contents
) {
    data class Contents(val playlistPanelContinuation: YoutubeiNextResponse.PlaylistPanelRenderer)
}

private fun getSongRadio(video_id: String, continuation: String?): Result<RadioData> {
    val request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/next")
        .header("accept", "*/*")
        .header("accept-encoding", "gzip, deflate")
        .header("content-encoding", "gzip")
        .header("origin", "https://music.youtube.com")
        .header("X-Goog-Visitor-Id", "CgtUYXUtLWtyZ3ZvTSj3pNWaBg%3D%3D")
        .header("Content-type", "application/json")
        .header("Cookie", "CONSENT=YES+1")
        .post(DataApi.getYoutubeiRequestBody(
        """
        {
            "enablePersistentPlaylistPanel": true,
            "isAudioOnly": true,
            "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
            "videoId": "$video_id",
            "playlistId": "RDAMVM$video_id",
            "watchEndpointMusicSupportedConfigs": {
                "watchEndpointMusicConfig": {
                    "hasPersistentPlaylistPanel": true,
                    "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                }
            }
            ${if (continuation != null) """, "continuation": "$continuation" """ else ""}
        }
        """
        ))
        .build()
    
    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = GZIPInputStream(result.getOrThrow().body!!.byteStream())

    val radio: YoutubeiNextResponse.PlaylistPanelRenderer

    if (continuation == null) {
        radio = DataApi.klaxon.parse<YoutubeiNextResponse>(stream)!!
            .contents
            .singleColumnMusicWatchNextResultsRenderer
            .tabbedRenderer
            .watchNextTabbedResultsRenderer
            .tabs
            .first()
            .tabRenderer
            .content!!
            .musicQueueRenderer
            .content
            .playlistPanelRenderer
    }
    else {
        radio = DataApi.klaxon.parse<YoutubeiNextContinuationResponse>(stream)!!
            .continuationContents
            .playlistPanelContinuation
    }

    stream.close()

    return Result.success(
        RadioData(
            radio.contents.map { item ->
                val song = Song.fromId(item.playlistPanelVideoRenderer!!.videoId)
                    .supplyTitle(item.playlistPanelVideoRenderer.title.first_text) as Song

                for (run in item.playlistPanelVideoRenderer.longBylineText.runs!!) {
                    if (run.navigationEndpoint == null) {
                        continue
                    }

                    val browse_endpoint = run.navigationEndpoint.browseEndpoint!!
                    if (browse_endpoint.page_type == "MUSIC_PAGE_TYPE_ARTIST") {
                        song.supplyArtist(Artist.fromId(browse_endpoint.browseId).supplyTitle(run.text) as Artist)
                        continue
                    }

                    song.addBrowseEndpoint(
                        browse_endpoint.browseId,
                        browse_endpoint.browseEndpointContextSupportedConfigs!!.browseEndpointContextMusicConfig.pageType
                    )
                }

                return@map song
            },
            radio.continuations?.firstOrNull()?.data?.continuation
        )
    )
}