package com.spectre7.spmp.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.printJson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Request
import okio.ByteString.Companion.readByteString
import java.util.zip.GZIPInputStream

@OptIn(DelicateCoroutinesApi::class)
class RadioInstance {
    private var continuation: MediaItemLayout.Continuation? = null
    private var current_job: Job? by mutableStateOf(null)
    private val lock = Object()

    var item: MediaItem? by mutableStateOf(null)
        private set
    val active: Boolean get() = item != null
    val loading: Boolean get() = current_job != null

    fun playMediaItem(item: MediaItem) {
        synchronized(lock) {
            this.item = item
            continuation = null
            cancelJob()
        }
    }

    fun cancelRadio() {
        synchronized(lock) {
            item = null
            continuation = null
            cancelJob()
        }
    }

    private fun cancelJob() {
        current_job?.cancel()
        current_job = null
    }

    fun loadContinuation(callback: (Result<List<Song>>) -> Unit) {
        synchronized(lock) {
            check(current_job == null)

            current_job = GlobalScope.launch {
                if (continuation == null) {
                    callback(getInitialSongs())
                    synchronized(lock) { current_job = null }
                    return@launch
                }

                val result = continuation!!.loadContinuation()
                if (result.isFailure) {
                    callback(result.cast())
                    synchronized(lock) { current_job = null }
                    return@launch
                }

                val (items, cont) = result.getOrThrow()

                if (cont != null) {
                    continuation!!.update(cont)
                }
                else {
                    continuation = null
                }

                callback(Result.success(items.filterIsInstance<Song>()))
                synchronized(lock) { current_job = null }
            }
        }
    }

    private fun getInitialSongs(): Result<List<Song>> {
        when (val item = item!!) {
            is Song -> {
                val result = getSongRadio(item.id, null)
                return result.fold(
                    {
                        continuation = it.continuation?.let { MediaItemLayout.Continuation(it, MediaItemLayout.Continuation.Type.SONG, item!!.id) }
                        Result.success(it.items)
                    },
                    { Result.failure(it) }
                )
            }
            is Playlist -> {
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
                return Result.success(layout.items.filterIsInstance<Song>())
            }
            is Artist -> TODO()
            else -> throw NotImplementedError(item.javaClass.name)
        }
    }
}

data class RadioData(val items: List<Song>, var continuation: String?, val filters: List<RadioBuilderModifier>?)

data class YoutubeiNextResponse(
    val contents: Contents
) {
    class Contents(val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer)
    class SingleColumnMusicWatchNextResultsRenderer(val tabbedRenderer: TabbedRenderer)
    class TabbedRenderer(val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer)
    class WatchNextTabbedResultsRenderer(val tabs: List<Tab>)
    class Tab(val tabRenderer: TabRenderer)
    class TabRenderer(val content: Content? = null)
    class Content(val musicQueueRenderer: MusicQueueRenderer)
    class MusicQueueRenderer(val content: MusicQueueRendererContent, val subHeaderChipCloud: SubHeaderChipCloud)

    class SubHeaderChipCloud(val chipCloudRenderer: ChipCloudRenderer)
    class ChipCloudRenderer(val chips: List<Chip>)
    class Chip(val chipCloudChipRenderer: ChipCloudChipRenderer)
    class ChipCloudChipRenderer(val navigationEndpoint: NavigationEndpoint)

    class MusicQueueRendererContent(val playlistPanelRenderer: PlaylistPanelRenderer)
    class PlaylistPanelRenderer(val contents: List<ResponseRadioItem>, val continuations: List<Continuation>? = null)
    class ResponseRadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null)
    class PlaylistPanelVideoRenderer(
        val videoId: String,
        val title: TextRuns,
        val longBylineText: TextRuns,
        val menu: Menu
    ) {
        // Artist, certain
        fun getArtist(host_item: Song): Result<Pair<Artist?, Boolean>> {
            // Get artist ID directly
            for (run in longBylineText.runs!! + title.runs!!) {
                if (run.browse_endpoint_type != "MUSIC_PAGE_TYPE_ARTIST" && run.browse_endpoint_type != "MUSIC_PAGE_TYPE_USER_CHANNEL") {
                    continue
                }

                return Result.success(Pair(
                    Artist.fromId(run.navigationEndpoint!!.browseEndpoint!!.browseId).supplyTitle(run.text) as Artist,
                    true
                ))
            }

            val menu_artist = menu.menuRenderer.getArtist()?.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
            if (menu_artist != null) {
                return Result.success(Pair(
                    Artist.fromId(menu_artist),
                    false
                ))
            }

            // Get artist from album
            for (run in longBylineText.runs!!) {
                if (run.navigationEndpoint?.browseEndpoint?.getPageType() != "MUSIC_PAGE_TYPE_ALBUM") {
                    continue
                }

                val playlist_result = Playlist.fromId(run.navigationEndpoint.browseEndpoint.browseId).loadData()
                if (playlist_result.isFailure) {
                    return playlist_result.cast()
                }

                val artist = playlist_result.getOrThrowHere()?.artist
                if (artist != null) {
                    return Result.success(Pair(artist, false))
                }
            }

            // Get title-only artist (Resolves to 'Various artists' when viewed on YouTube)
            val artist_title = longBylineText.runs?.firstOrNull { it.navigationEndpoint == null }
            if (artist_title != null) {
                return Result.success(Pair(
                    Artist.createForItem(host_item).supplyTitle(artist_title.text) as Artist,
                    false
                ))
            }

            return Result.success(Pair(null, false))
        }
    }
    class Menu(val menuRenderer: MenuRenderer)
    class MenuRenderer(val items: List<MenuItem>) {
        fun getArtist(): MenuItem? {
            return items.firstOrNull {
                it.menuNavigationItemRenderer?.icon?.iconType == "ARTIST"
            }
        }
    }
    class MenuItem(val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null)
    class MenuNavigationItemRenderer(val icon: MenuIcon, val navigationEndpoint: NavigationEndpoint)
    class MenuIcon(val iconType: String)
    class Continuation(val nextContinuationData: ContinuationData? = null, val nextRadioContinuationData: ContinuationData? = null) {
        val data: ContinuationData? get() = nextContinuationData ?: nextRadioContinuationData
    }
    class ContinuationData(val continuation: String)
}

data class YoutubeiNextContinuationResponse(
    val continuationContents: Contents
) {
    data class Contents(val playlistPanelContinuation: YoutubeiNextResponse.PlaylistPanelRenderer)
}

fun getSongRadio(video_id: String, continuation: String?): Result<RadioData> {
    val RADIO_PLAYLIST_ID_PREFIX = "RDAMVM"
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
            "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
            "isAudioOnly": true,
            "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
            "playlistId": "$RADIO_PLAYLIST_ID_PREFIX$video_id",
            "videoId", "$video_id",
            "params": "wAEB",
            "activePlayers": [{"playerContextParams": "Q0FFU0FnZ0I="}],
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
    val filters: List<RadioBuilderModifier>?

    if (continuation == null) {
        val renderer = DataApi.klaxon.parse<YoutubeiNextResponse>(stream)!!
            .contents
            .singleColumnMusicWatchNextResultsRenderer
            .tabbedRenderer
            .watchNextTabbedResultsRenderer
            .tabs
            .first()
            .tabRenderer
            .content!!
            .musicQueueRenderer
        radio = renderer.content.playlistPanelRenderer

        filters = renderer.subHeaderChipCloud.chipCloudRenderer.chips.mapNotNull { chip ->
            val id = chip.chipCloudChipRenderer.navigationEndpoint.watchEndpoint?.playlistId
            if (id == null) {
                return@mapNotNull null
            }

            val modifier_id = id.substring(RADIO_PLAYLIST_ID_PREFIX.length, id.length - video_id.length)
            TODO()

            return@mapNotNull RadioBuilderModifier.fromString(id)
        }
    }
    else {
        radio = DataApi.klaxon.parse<YoutubeiNextContinuationResponse>(stream)!!
            .continuationContents
            .playlistPanelContinuation
        filters = null
    }

    stream.close()

    return Result.success(
        RadioData(
            radio.contents.map { item ->
                val song = Song.fromId(item.playlistPanelVideoRenderer!!.videoId)
                    .supplyTitle(item.playlistPanelVideoRenderer.title.first_text) as Song

                val artist_result = item.playlistPanelVideoRenderer.getArtist(song)
                if (artist_result.isFailure) {
                    return artist_result.cast()
                }

                val (artist, certain) = artist_result.getOrThrow()
                if (artist != null) {
                    song.supplyArtist(artist, certain)
                }

                return@map song
            },
            radio.continuations?.firstOrNull()?.data?.continuation,
            filters
        )
    )
}