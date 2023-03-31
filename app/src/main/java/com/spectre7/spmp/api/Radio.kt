package com.spectre7.spmp.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.api.DataApi.Companion.addYtHeaders
import com.spectre7.spmp.api.DataApi.Companion.getStream
import com.spectre7.spmp.api.DataApi.Companion.ytUrl
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.Listeners
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Request

private const val RADIO_ID_PREFIX = "RDAMVM"
private const val MODIFIED_RADIO_ID_PREFIX = "RDAT"

@OptIn(DelicateCoroutinesApi::class)
class RadioInstance {
    private val lock = Object()
    private var current_job: Job? by mutableStateOf(null)

    var filters: List<List<RadioModifier>>? by mutableStateOf(null)
        private set
    var current_filter: Int? by mutableStateOf(null)
        private set

    private val _filter_changed_listeners: MutableList<(List<RadioModifier>?) -> Unit> = mutableListOf()
    val filter_changed_listeners = Listeners(_filter_changed_listeners)

    private var continuation: MediaItemLayout.Continuation? = null

    var item: MediaItem? by mutableStateOf(null)
        private set
    val active: Boolean get() = item != null
    val loading: Boolean get() = current_job != null

    fun playMediaItem(item: MediaItem) {
        synchronized(lock) {
            cancelJob()
            reset()
            this.item = item
        }
    }

    fun setFilter(filter_index: Int?) {
        if (filter_index == current_filter) {
            return
        }
        current_filter = filter_index
        continuation = null

        val filter = current_filter?.let { filters!![it] }
        for (listener in _filter_changed_listeners) {
            listener.invoke(filter)
        }
    }

    fun cancelRadio() {
        synchronized(lock) {
            reset()
            cancelJob()
        }
    }

    private fun reset() {
        item = null
        continuation = null
        filters = null
        current_filter = null
    }

    fun cancelJob() {
        current_job?.cancel()
        current_job = null
    }

    fun loadContinuation(onStart: (() -> Unit)? = null, callback: (Result<List<Song>>) -> Unit) {
        synchronized(lock) {
            check(current_job == null)

            current_job = GlobalScope.launch {
                onStart?.invoke()

                if (continuation == null) {
                    callback(getInitialSongs())
                    return@launch
                }

                val result = continuation!!.loadContinuation(current_filter?.let { filters?.get(it) } ?: emptyList())
                if (result.isFailure) {
                    callback(result.cast())
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
            }

            current_job!!.invokeOnCompletion {
                synchronized(lock) { current_job = null }
            }
        }
    }

    private fun getInitialSongs(): Result<List<Song>> {
        when (val item = item!!) {
            is Song -> {
                val result = getSongRadio(item.id, null, current_filter?.let { filters?.get(it) } ?: emptyList())
                return result.fold(
                    { data ->
                        continuation = data.continuation?.let { continuation ->
                            MediaItemLayout.Continuation(continuation, MediaItemLayout.Continuation.Type.SONG, item.id)
                        }

                        if (data.filters != null) {
                            filters = data.filters
                        }

                        Result.success(data.items)
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

data class RadioData(val items: List<Song>, var continuation: String?, val filters: List<List<RadioModifier>>?)

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
    class MusicQueueRenderer(val content: MusicQueueRendererContent, val subHeaderChipCloud: SubHeaderChipCloud? = null)

    class SubHeaderChipCloud(val chipCloudRenderer: ChipCloudRenderer)
    class ChipCloudRenderer(val chips: List<Chip>)
    class Chip(val chipCloudChipRenderer: ChipCloudChipRenderer) {
        fun getPlaylistId(): String = chipCloudChipRenderer.navigationEndpoint.queueUpdateCommand.fetchContentsCommand.watchEndpoint.playlistId!!
    }
    class ChipCloudChipRenderer(val navigationEndpoint: ChipNavigationEndpoint)
    class ChipNavigationEndpoint(val queueUpdateCommand: QueueUpdateCommand)
    class QueueUpdateCommand(val fetchContentsCommand: FetchContentsCommand)
    class FetchContentsCommand(val watchEndpoint: WatchEndpoint)

    class MusicQueueRendererContent(val playlistPanelRenderer: PlaylistPanelRenderer)
    class PlaylistPanelRenderer(val contents: List<ResponseRadioItem>, val continuations: List<Continuation>? = null)
    class ResponseRadioItem(val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null)
    class PlaylistPanelVideoRenderer(
        val videoId: String,
        val title: TextRuns,
        val longBylineText: TextRuns,
        val menu: Menu,
        val thumbnail: MusicThumbnailRenderer.Thumbnail
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

fun radioToFilters(radio: String, video_id: String): List<RadioModifier>? {
    if (!radio.startsWith(MODIFIED_RADIO_ID_PREFIX)) {
        return null
    }

    val ret: MutableList<RadioModifier> = mutableListOf()
    val modifier_string = radio.substring(MODIFIED_RADIO_ID_PREFIX.length, radio.length - video_id.length)

    var c = 0
    while (c + 1 < modifier_string.length) {
        val modifier = RadioModifier.fromString(modifier_string.substring(c++, ++c))
        if (modifier != null) {
            ret.add(modifier)
        }
    }

    if (ret.isEmpty()) {
        return null
    }

    return ret
}

fun videoIdToRadio(video_id: String, filters: List<RadioModifier>): String {
    if (filters.isEmpty()) {
        return RADIO_ID_PREFIX + video_id
    }

    val ret = StringBuilder(MODIFIED_RADIO_ID_PREFIX)
    for (filter in filters) {
        filter.string?.also { ret.append(it) }
    }
    ret.append('v')
    ret.append(video_id)
    return ret.toString()
}

fun getSongRadio(video_id: String, continuation: String?, filters: List<RadioModifier> = emptyList()): Result<RadioData> {

    val request = Request.Builder()
        .ytUrl("/youtubei/v1/next")
        .addYtHeaders()
        .post(DataApi.getYoutubeiRequestBody(
        """
        {
            "enablePersistentPlaylistPanel": true,
            "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
            "playlistId": "${videoIdToRadio(video_id, filters)}",
            "watchEndpointMusicSupportedConfigs": {
                "watchEndpointMusicConfig": {
                    "hasPersistentPlaylistPanel": true,
                    "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                }
            },
            "isAudioOnly": true
            ${if (continuation != null) ", \"continuation\": \"$continuation\" " else ""}
        }
        """
        ))
        .build()
    
    val result = DataApi.request(request)
    if (result.isFailure) {
        return result.cast()
    }

    val stream = result.getOrThrowHere().getStream()

    val radio: YoutubeiNextResponse.PlaylistPanelRenderer
    val out_filters: List<List<RadioModifier>>?

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

        out_filters = renderer.subHeaderChipCloud?.chipCloudRenderer?.chips?.mapNotNull { chip ->
            radioToFilters(chip.getPlaylistId(), video_id)
        }
    }
    else {
        radio = DataApi.klaxon.parse<YoutubeiNextContinuationResponse>(stream)!!
            .continuationContents
            .playlistPanelContinuation
        out_filters = null
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
            out_filters
        )
    )
}