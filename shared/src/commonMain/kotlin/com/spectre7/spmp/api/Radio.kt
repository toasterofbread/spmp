package com.spectre7.spmp.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spectre7.spmp.api.Api.Companion.addYtHeaders
import com.spectre7.spmp.api.Api.Companion.getStream
import com.spectre7.spmp.api.Api.Companion.ytUrl
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.utils.ValueListeners
import kotlinx.coroutines.*
import okhttp3.Request

private const val RADIO_ID_PREFIX = "RDAMVM"
private const val MODIFIED_RADIO_ID_PREFIX = "RDAT"

@OptIn(DelicateCoroutinesApi::class)
class RadioInstance {
    var state: RadioState by mutableStateOf(RadioState())
        private set
    val active: Boolean get() = state.item != null
    var loading: Boolean by mutableStateOf(false)
        private set

    private val coroutine_scope = CoroutineScope(Dispatchers.IO)
    private val lock = coroutine_scope

    val filter_changed_listeners = ValueListeners<List<RadioModifier>?>()

    fun playMediaItem(item: MediaItem, index: Int? = null, shuffle: Boolean = false) {
        synchronized(lock) {
            cancelJob()
            state = RadioState()
            state.item = Pair(item, index)
            state.shuffle = shuffle
        }
    }

    fun setFilter(filter_index: Int?) {
        if (filter_index == state.current_filter) {
            return
        }
        state.current_filter = filter_index
        state.continuation = null

        val filter = state.current_filter?.let { state.filters!![it] }
        filter_changed_listeners.call(filter)
    }

    fun onSongMoved(from: Int, to: Int) {
        if (from == to) {
            return
        }

        val current_index = state.item?.second ?: return

        if (from == current_index) {
            state.item = state.item?.copy(second = to)
        }
        else if (current_index in from..to) {
            state.item = state.item?.copy(second = current_index - 1)
        }
        else if (current_index in to .. from) {
            state.item = state.item?.copy(second = current_index + 1)
        }
    }

    class RadioState {
        var item: Pair<MediaItem, Int?>? by mutableStateOf(null)
        var continuation: MediaItemLayout.Continuation? by mutableStateOf(null)
        var filters: List<List<RadioModifier>>? by mutableStateOf(null)
        var current_filter: Int? by mutableStateOf(null)
        var shuffle: Boolean = false
    }

    fun setRadioState(new_state: RadioState) {
        if (state == new_state) {
            return
        }
        synchronized(lock) {
            cancelRadio()
            state = new_state
        }
    }

    fun cancelRadio(): RadioState {
        synchronized(lock) {
            val old_state = state
            state = RadioState()
            cancelJob()
            return old_state
        }
    }

    fun cancelJob() {
        coroutine_scope.coroutineContext.cancelChildren()
        loading = false
    }

    private fun formatContinuationResult(result: Result<List<Song>>): Result<List<Song>> =
        result.fold(
            { songs ->
                if (state.shuffle) Result.success(songs.shuffled())
                else result
            },
            { result }
        )

    fun loadContinuation(onStart: (() -> Unit)? = null, callback: (Result<List<Song>>) -> Unit) {
        synchronized(lock) {
            check(!loading)

            coroutine_scope.launch {
                coroutineContext.job.invokeOnCompletion {
                    synchronized(lock) {
                        loading = false
                    }
                }
                synchronized(lock) {
                    loading = true
                }

                onStart?.invoke()

                if (state.continuation == null) {
                    val initial_songs = getInitialSongs()
                    val formatted = formatContinuationResult(initial_songs)
                    callback(formatted)
                    return@launch
                }

                val result = state.continuation!!.loadContinuation(state.current_filter?.let { state.filters?.get(it) } ?: emptyList())
                if (result.isFailure) {
                    callback(result.cast())
                    return@launch
                }

                val (items, cont) = result.getOrThrow()

                if (cont != null) {
                    state.continuation!!.update(cont)
                }
                else {
                    state.continuation = null
                }

                callback(formatContinuationResult(Result.success(items.filterIsInstance<Song>())))
            }
        }
    }

    private suspend fun getInitialSongs(): Result<List<Song>> {
        when (val item = state.item!!.first) {
            is Song -> {
                val result = getSongRadio(item.id, null, state.current_filter?.let { state.filters?.get(it) } ?: emptyList())
                return result.fold(
                    { data ->
                        state.continuation = data.continuation?.let { continuation ->
                            MediaItemLayout.Continuation(continuation, MediaItemLayout.Continuation.Type.SONG, item.id)
                        }

                        if (state.filters == null) {
                            state.filters = data.filters
                        }

                        Result.success(data.items)
                    },
                    { Result.failure(it) }
                )
            }
            is MediaItemWithLayouts -> {
                val feed_layouts = item.getFeedLayouts().fold(
                    { it },
                    { return Result.failure(it) }
                )

                val layout = feed_layouts.firstOrNull()
                if (layout == null) {
                    return Result.success(emptyList())
                }

                val layout_item = layout.view_more?.media_item
                if (layout_item is Playlist) {
                    state.continuation = MediaItemLayout.Continuation(layout_item.id, MediaItemLayout.Continuation.Type.PLAYLIST_INITIAL, layout.items.size)
                }
                else {
                    state.continuation = layout.continuation
                }

                return Result.success(layout.items.filterIsInstance<Song>())
            }
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
        suspend fun getArtist(host_item: Song): Result<Pair<Artist?, Boolean>> {
            // Get artist ID directly
            for (run in longBylineText.runs!! + title.runs!!) {
                if (run.browse_endpoint_type != "MUSIC_PAGE_TYPE_ARTIST" && run.browse_endpoint_type != "MUSIC_PAGE_TYPE_USER_CHANNEL") {
                    continue
                }

                return Result.success(Pair(
                    Artist.fromId(run.navigationEndpoint!!.browseEndpoint!!.browseId).editArtistData { supplyTitle(run.text) },
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

                val playlist = AccountPlaylist.fromId(run.navigationEndpoint.browseEndpoint.browseId)
                return playlist.getArtistOrNull().fold(
                    { artist ->
                        Result.success(Pair(artist, false))
                    },
                    { Result.failure(it) }
                )
            }

            // Get title-only artist (Resolves to 'Various artists' when viewed on YouTube)
            val artist_title = longBylineText.runs?.firstOrNull { it.navigationEndpoint == null }
            if (artist_title != null) {
                return Result.success(Pair(
                    Artist.createForItem(host_item).editArtistData { supplyTitle(artist_title.text) },
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

suspend fun getSongRadio(video_id: String, continuation: String?, filters: List<RadioModifier> = emptyList()): Result<RadioData> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/next")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf(
            "enablePersistentPlaylistPanel" to true,
            "tunerSettingValue" to "AUTOMIX_SETTING_NORMAL",
            "playlistId" to videoIdToRadio(video_id, filters),
            "watchEndpointMusicSupportedConfigs" to mapOf(
                "watchEndpointMusicConfig" to mapOf(
                    "hasPersistentPlaylistPanel" to true,
                    "musicVideoType" to "MUSIC_VIDEO_TYPE_ATV"
                )
            ),
            "isAudioOnly" to true
        ).let {
            if (continuation == null) it
            else it + mapOf("continuation" to continuation)
        }))
        .build()

    val result = Api.request(request)
    if (result.isFailure) {
        return@withContext result.cast()
    }

    val stream = result.getOrThrowHere().getStream()

    val radio: YoutubeiNextResponse.PlaylistPanelRenderer
    val out_filters: List<List<RadioModifier>>?

    if (continuation == null) {
        val renderer = Api.klaxon.parse<YoutubeiNextResponse>(stream)!!
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
        radio = Api.klaxon.parse<YoutubeiNextContinuationResponse>(stream)!!
            .continuationContents
            .playlistPanelContinuation
        out_filters = null
    }

    stream.close()

    return@withContext Result.success(
        RadioData(
            radio.contents.map { item ->
                val song = Song.fromId(item.playlistPanelVideoRenderer!!.videoId)
                val error = song.editSongDataSuspend<Result<RadioData>?> {
                    supplyTitle(item.playlistPanelVideoRenderer.title.first_text)

                    val artist_result = item.playlistPanelVideoRenderer.getArtist(song)
                    if (artist_result.isFailure) {
                        return@editSongDataSuspend artist_result.cast()
                    }

                    val (artist, certain) = artist_result.getOrThrow()
                    if (artist != null) {
                        supplyArtist(artist, certain)
                    }

                    null
                }

                if (error != null) {
                    return@withContext error
                }

                return@map song
            },
            radio.continuations?.firstOrNull()?.data?.continuation,
            out_filters
        )
    )
}