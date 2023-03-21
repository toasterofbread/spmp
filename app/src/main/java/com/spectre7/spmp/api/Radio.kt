package com.spectre7.spmp.api

import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import okhttp3.Request
import java.util.zip.GZIPInputStream

class RadioInstance {
    private var item: MediaItem? = null
    private var continuation: MediaItemLayout.Continuation? = null

    val active: Boolean get() = song != null
    val has_continuation: Boolean get() = continuation != null

    fun playMediaItem(item: MediaItem): Result<List<Song>> {
        this.item = item
        continuation = null
        return getContinuation()
    }

    fun cancelRadio() {
        song = null
        continuation = null
    }

    fun getContinuation(): Result<List<Song>> {
        if (continuation == null) {
            return getInitialSongs()
        }

        val result = continuation.loadContinuation()
        if (result.isFailure) {
            return result.cast()
        }

        val (items, cont) = result.getOrThrow()
        continuation.update(cont)

        return Result.success(items)
    }

    private fun getInitialSongs(): Result<List<Song>> {
        when (item.type) {
            MediaItem.Type.SONG -> {
                val result = getSongRadio(item.id, continuation)
                return result.fold(
                    {
                        continuation = MediaItemLayout.Continuation(it.continuation, MediaItemLayout.Continuation.Type.SONG, item.id)
                        Result.success(it.items)
                    }
                    { Result.failure(It) }
                )
            }
            MediaItem.Type.PLAYLIST -> {
                if (item.feed_layouts == null) {
                    val result = item.loadData()
                    if (result.isFailure) {
                        return result.cast()
                    }
                }

                val layout = item.feed_layouts?.firstOrNull()
                if (layout == null) {
                    return Result.success(emptyList())
                }

                continuation = layout.continuation
                return Result.success(layout.items)
            }
            MediaItem.Type.ARTIST -> TODO()
        }
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

    val stream = GZIPInputStream(result.getOrThrowHere().body!!.byteStream())

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