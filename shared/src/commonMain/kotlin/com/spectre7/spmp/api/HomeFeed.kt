package com.spectre7.spmp.api

import SpMp
import com.beust.klaxon.Json
import com.spectre7.spmp.api.Api.Companion.addYtHeaders
import com.spectre7.spmp.api.Api.Companion.getStream
import com.spectre7.spmp.api.Api.Companion.ytUrl
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.*
import com.spectre7.spmp.model.mediaitem.data.MediaItemData
import com.spectre7.spmp.model.mediaitem.enums.MediaItemType
import com.spectre7.spmp.model.mediaitem.enums.PlaylistType
import com.spectre7.spmp.model.mediaitem.enums.SongType
import com.spectre7.spmp.ui.component.MediaItemLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.InputStreamReader
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

suspend fun getHomeFeed(
    min_rows: Int = -1,
    allow_cached: Boolean = true,
    params: String? = null,
    continuation: String? = null
): Result<Triple<List<MediaItemLayout>, String?, List<Pair<Int, String>>?>> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val suffix = params ?: ""
    val rows_cache_key = "feed_rows$suffix"
    val ctoken_cache_key = "feed_ctoken$suffix"
    val chips_cache_key = "feed_chips$suffix"

    if (allow_cached && continuation == null) {
        val cached_rows = Cache.get(rows_cache_key)
        if (cached_rows != null) {
            val rows = Api.klaxon.parseArray<MediaItemLayout>(cached_rows)!!
            cached_rows.close()

            val ctoken = Cache.get(ctoken_cache_key)?.run {
                val ctoken = readText()
                close()
                ctoken
            }

            val chips = Cache.get(chips_cache_key)?.run {
                val chips: List<List<Any>> = Api.klaxon.parseArray(this)!!
                close()
                chips.map { Pair(it[0] as Int, it[1] as String) }
            }

            return@withContext Result.success(Triple(rows, ctoken, chips))
        }
    }

    var result: Result<InputStreamReader>? = null

    suspend fun performRequest(ctoken: String?) = withContext(Dispatchers.IO) {
        val endpoint = "/youtubei/v1/browse"
        val request = Request.Builder()
            .ytUrl(if (ctoken == null) endpoint else "$endpoint?ctoken=$ctoken&continuation=$ctoken&type=next")
            .addYtHeaders()
            .post(
                Api.getYoutubeiRequestBody(params?.let {
                    """{"params": "$it"}"""
                })
            )
            .build()

        result = Api.request(request).cast {
            it.getStream().reader()
        }
    }

    coroutineContext.job.invokeOnCompletion {
        result?.getOrNull()?.close()
    }

    performRequest(continuation)
    result!!.onFailure {
        return@withContext Result.failure(it)
    }

    val response_reader = result!!.getOrThrowHere()
    var data: YoutubeiBrowseResponse = Api.klaxon.parse(response_reader)!!
    response_reader.close()

    val rows: MutableList<MediaItemLayout> = processRows(data.getShelves(continuation != null), hl).toMutableList()
    check(rows.isNotEmpty())

    val chips = data.getHeaderChips()

    var ctoken: String? = data.ctoken
    while (min_rows >= 1 && rows.size < min_rows) {
        if (ctoken == null) {
            break
        }

        performRequest(ctoken)
        result!!.onFailure {
            return@withContext Result.failure(it)
        }

        data = Api.klaxon.parse(result!!.data)!!
        result!!.data.close()

        val shelves = data.getShelves(true)
        check(shelves.isNotEmpty())
        rows.addAll(processRows(shelves, hl))

        ctoken = data.ctoken
    }

    if (continuation == null) {
        Cache.set(rows_cache_key, Api.klaxon.toJsonString(rows).reader(), CACHE_LIFETIME)
        Cache.set(ctoken_cache_key, ctoken?.reader(), CACHE_LIFETIME)
        Cache.set(chips_cache_key, chips?.let { Api.klaxon.toJsonString(it.map { chip -> listOf(chip.first, chip.second) }).reader() }, CACHE_LIFETIME)
    }

    return@withContext Result.success(Triple(rows, ctoken, chips))
}

private fun processRows(rows: List<YoutubeiShelf>, hl: String): List<MediaItemLayout> {
    val ret = mutableListOf<MediaItemLayout>()
    for (row in rows) {
        if (!row.implemented) {
            continue
        }

        when (val renderer = row.getRenderer()) {
            is MusicDescriptionShelfRenderer -> continue
            is MusicCarouselShelfRenderer -> {
                val header = renderer.header.musicCarouselShelfBasicHeaderRenderer!!

                fun add(
                    title: LocalisedYoutubeString,
                    subtitle: LocalisedYoutubeString? = null,
                    thumbnail_source: MediaItemLayout.ThumbnailSource? =
                        header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.let {
                            MediaItemLayout.ThumbnailSource(null, url = it.url)
                        },
                    media_item_type: MediaItemType? = null,
                    view_more: MediaItemLayout.ViewMore? = null
                ) {
                    val items = row.getMediaItems(hl).toMutableList()

                    ret.add(MediaItemLayout(
                        title, subtitle,
                        items = items,
                        thumbnail_source = thumbnail_source,
                        view_more = view_more,
                        thumbnail_item_type = media_item_type
                    ))
                }

                val browse_endpoint = header.title.runs?.first()?.navigationEndpoint?.browseEndpoint
                if (browse_endpoint == null) {
                    add(
                        LocalisedYoutubeString.homeFeed(header.title.first_text),
                        header.subtitle?.first_text?.let { LocalisedYoutubeString.homeFeed(it) }
                    )
                    continue
                }

                val view_more_page_title_key = when (browse_endpoint.browseId) {
                    "FEmusic_listen_again" -> if (Settings.KEY_FEED_SHOW_LISTEN_ROW.get()) "home_feed_listen_again" else null
                    "FEmusic_mixed_for_you" -> if (Settings.KEY_FEED_SHOW_MIX_ROW.get()) "home_feed_mixed_for_you" else null
                    "FEmusic_new_releases_albums" -> if (Settings.KEY_FEED_SHOW_NEW_ROW.get()) "home_feed_new_releases" else null
                    "FEmusic_moods_and_genres" -> if (Settings.KEY_FEED_SHOW_MOODS_ROW.get()) "home_feed_moods_and_genres" else null
                    "FEmusic_charts" -> if (Settings.KEY_FEED_SHOW_CHARTS_ROW.get()) "home_feed_charts" else null
                    else -> null
                }

                if (view_more_page_title_key != null) {
                    add(
                        LocalisedYoutubeString.app(view_more_page_title_key),
                        null,
                        view_more = MediaItemLayout.ViewMore(list_page_browse_id = browse_endpoint.browseId)
                    )
                    continue
                }

                val page_type = browse_endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType

                val media_item: MediaItem = when (page_type) {
                    "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> Artist.fromId(browse_endpoint.browseId)
                    "MUSIC_PAGE_TYPE_PLAYLIST" -> AccountPlaylist.fromId(browse_endpoint.browseId).editPlaylistData { supplyTitle(header.title.first_text) }
                    else -> throw NotImplementedError(browse_endpoint.toString())
                }

                val thumbnail_source =
                    header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.let {
                        MediaItemLayout.ThumbnailSource(url = it.url)
                    }
                    ?: MediaItemLayout.ThumbnailSource(media_item = media_item)

                add(
                    LocalisedYoutubeString.raw(header.title.first_text),
                    header.subtitle?.first_text?.let { LocalisedYoutubeString.homeFeed(it) },
                    view_more = MediaItemLayout.ViewMore(media_item = media_item),
                    thumbnail_source = thumbnail_source,
                    media_item_type = media_item.type
                )
            }
            else -> throw NotImplementedError(row.getRenderer().toString())
        }
    }

    return ret
}

data class YoutubeiBrowseResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
    val header: Header? = null
) {
    val ctoken: String?
        get() = continuationContents?.sectionListContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation
                ?: contents!!.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content?.sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

    fun getShelves(has_continuation: Boolean): List<YoutubeiShelf> {
        return if (has_continuation) continuationContents?.sectionListContinuation?.contents ?: emptyList()
               else contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents ?: emptyList()
    }

    fun getHeaderChips(): List<Pair<Int, String>>? =
        contents?.singleColumnBrowseResultsRenderer?.tabs?.first()?.tabRenderer?.content?.sectionListRenderer?.header?.chipCloudRenderer?.chips?.map {
            Pair(
                LocalisedYoutubeString.filterChip(it.chipCloudChipRenderer.text!!.first_text) ?: throw NotImplementedError(it.chipCloudChipRenderer.text.first_text),
                it.chipCloudChipRenderer.navigationEndpoint.browseEndpoint!!.params!!
            )
        }

    data class Contents(val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer)
    data class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content? = null)
    data class Content(val sectionListRenderer: SectionListRenderer)
    open class SectionListRenderer(val contents: List<YoutubeiShelf>? = null, val header: ChipCloudRendererHeader? = null, val continuations: List<YoutubeiNextResponse.Continuation>? = null)

    data class ContinuationContents(val sectionListContinuation: SectionListRenderer? = null, val musicPlaylistShelfContinuation: MusicShelfRenderer? = null)
}

data class ItemSectionRenderer(val contents: List<ItemSectionRendererContent>)
data class ItemSectionRendererContent(val didYouMeanRenderer: DidYouMeanRenderer? = null)
data class DidYouMeanRenderer(val correctedQuery: TextRuns)

data class YoutubeiShelf(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
    val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer? = null,
    val musicPlaylistShelfRenderer: MusicShelfRenderer? = null,
    val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
    val gridRenderer: GridRenderer? = null,
    val itemSectionRenderer: ItemSectionRenderer? = null,
    val musicTastebuilderShelfRenderer: Any? = null
) {
    init {
        assert(
            musicShelfRenderer != null
            || musicCarouselShelfRenderer != null
            || musicDescriptionShelfRenderer != null
            || musicPlaylistShelfRenderer != null
            || musicCardShelfRenderer != null
            || gridRenderer != null
            || itemSectionRenderer != null
            || musicTastebuilderShelfRenderer != null
        ) { "No known shelf renderer" }
    }

    val implemented: Boolean get() = musicTastebuilderShelfRenderer == null

    val title: TextRun? get() =
        if (musicShelfRenderer != null) musicShelfRenderer.title?.runs?.firstOrNull()
        else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.getRenderer().title.runs?.firstOrNull()
        else if (musicDescriptionShelfRenderer != null) musicDescriptionShelfRenderer.header.runs?.firstOrNull()
        else if (musicCardShelfRenderer != null) musicCardShelfRenderer.title.runs?.firstOrNull()
        else if (gridRenderer != null) gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()
        else null

    val description: String? get() = musicDescriptionShelfRenderer?.description?.first_text

    fun getNavigationEndpoint(): NavigationEndpoint? {
        return musicShelfRenderer?.bottomEndpoint ?: musicCarouselShelfRenderer?.header?.getRenderer()?.moreContentButton?.buttonRenderer?.navigationEndpoint
    }

    fun getMediaItems(hl: String): List<MediaItem> {
        return (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer!!.items).mapNotNull {
            return@mapNotNull it.toMediaItem(hl)?.first
        }
    }

    fun getMediaItemsAndSetIds(hl: String): List<Pair<MediaItem, String?>> {
        return (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer?.items ?: emptyList()).mapNotNull {
            return@mapNotNull it.toMediaItem(hl)
        }
    }

    fun getRenderer(): Any {
        return musicShelfRenderer ?: musicCarouselShelfRenderer ?: musicDescriptionShelfRenderer!!
    }
}

data class GridRenderer(val items: List<ContentsItem>, val header: GridHeader? = null)
data class GridHeader(val gridHeaderRenderer: HeaderRenderer)

data class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
data class BrowseEndpointContextMusicConfig(val pageType: String)
data class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
data class BrowseEndpoint(
    val browseId: String,
    val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null,
    val params: String? = null
) {
    fun getPageType(): String? = browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
    fun getMediaItemType(): MediaItemType? = getPageType()?.let { MediaItemType.fromBrowseEndpointType(it) }

    fun getMediaItem(): MediaItem? {
        return getPageType()?.let { page_type ->
            MediaItem.fromBrowseEndpointType(page_type, browseId)
        }
    }

    fun getViewMore(): MediaItemLayout.ViewMore? {
        return getMediaItem()?.let { MediaItemLayout.ViewMore(media_item = it, browse_params = params) }
    }
}
data class SearchEndpoint(val query: String, val params: String? = null)
data class WatchPlaylistEndpoint(val playlistId: String, val params: String)
data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null,
    val searchEndpoint: SearchEndpoint? = null,
    val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null
) {
    fun getMediaItem(): MediaItem? {
        if (watchEndpoint != null) {
            if (watchEndpoint.videoId != null) {
                return Song.fromId(watchEndpoint.videoId)
            }
            else if (watchEndpoint.playlistId != null) {
                return AccountPlaylist.fromId(watchEndpoint.playlistId)
            }
        }
        if (browseEndpoint != null) {
            browseEndpoint.getMediaItem()?.also { return it }
        }
        if (watchPlaylistEndpoint != null) {
            return AccountPlaylist.fromId(watchPlaylistEndpoint.playlistId)
        }
        return null
    }

    fun getViewMore(): MediaItemLayout.ViewMore? {
        if (browseEndpoint != null) {
            browseEndpoint.getViewMore()?.also { return it }
        }
        return getMediaItem()?.let { MediaItemLayout.ViewMore(media_item = it) }
    }
}
data class Header(
    val musicCarouselShelfBasicHeaderRenderer: HeaderRenderer? = null,
    val musicImmersiveHeaderRenderer: HeaderRenderer? = null,
    val musicVisualHeaderRenderer: HeaderRenderer? = null,
    val musicDetailHeaderRenderer: HeaderRenderer? = null,
    val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer? = null,
    val musicCardShelfHeaderBasicRenderer: HeaderRenderer? = null
) {
    fun getRenderer(): HeaderRenderer {
        return musicCarouselShelfBasicHeaderRenderer ?: musicImmersiveHeaderRenderer ?: musicVisualHeaderRenderer ?: musicDetailHeaderRenderer ?: musicCardShelfHeaderBasicRenderer ?: musicEditablePlaylistDetailHeaderRenderer!!.header.getRenderer()
    }

    data class MusicEditablePlaylistDetailHeaderRenderer(val header: Header)
}

//val thumbnails = (header.obj("thumbnail") ?: header.obj("foregroundThumbnail")!!)
//    .obj("musicThumbnailRenderer")!!
//    .obj("thumbnail")!!
//    .array<JsonObject>("thumbnails")!!

data class HeaderRenderer(
    val title: TextRuns,
    val strapline: TextRuns? = null,
    val subscriptionButton: SubscriptionButton? = null,
    val description: TextRuns? = null,
    val thumbnail: Thumbnails? = null,
    val foregroundThumbnail: Thumbnails? = null,
    val subtitle: TextRuns? = null,
    val secondSubtitle: TextRuns? = null,
    val moreContentButton: MoreContentButton? = null
) {
    fun getThumbnails(): List<MediaItemThumbnailProvider.Thumbnail> {
        return (foregroundThumbnail ?: thumbnail)?.thumbnails ?: emptyList()
    }
}
data class SubscriptionButton(val subscribeButtonRenderer: SubscribeButtonRenderer)
data class SubscribeButtonRenderer(val subscribed: Boolean, val subscriberCountText: TextRuns, val channelId: String)
data class Thumbnails(val musicThumbnailRenderer: MusicThumbnailRenderer? = null, val croppedSquareThumbnailRenderer: MusicThumbnailRenderer? = null) {
    init {
        assert(musicThumbnailRenderer != null || croppedSquareThumbnailRenderer != null)
    }
    @Json(ignored = true)
    val thumbnails: List<MediaItemThumbnailProvider.Thumbnail> get() = (musicThumbnailRenderer ?: croppedSquareThumbnailRenderer!!).thumbnail.thumbnails
}
data class MusicThumbnailRenderer(val thumbnail: Thumbnail) {
    data class Thumbnail(val thumbnails: List<MediaItemThumbnailProvider.Thumbnail>)
}
data class TextRuns(
    @Json(name = "runs")
    val _runs: List<TextRun>? = null
) {
    @Json(ignored = true)
    val runs: List<TextRun>? get() = _runs?.filter { it.text != " \u2022 " }
    @Json(ignored = true)
    val first_text: String get() = runs!![0].text
}
data class TextRun(val text: String, val strapline: TextRuns? = null, val navigationEndpoint: NavigationEndpoint? = null) {
    @Json(ignored = true)
    val browse_endpoint_type: String? get() = navigationEndpoint?.browseEndpoint?.getPageType()
}

data class MusicShelfRenderer(
    val title: TextRuns? = null,
    val contents: List<ContentsItem>? = null,
    val continuations: List<YoutubeiNextResponse.Continuation>? = null,
    val bottomEndpoint: NavigationEndpoint? = null
)
data class MoreContentButton(val buttonRenderer: ButtonRenderer)
data class ButtonRenderer(val navigationEndpoint: NavigationEndpoint)
data class MusicCarouselShelfRenderer(
    val header: Header,
    val contents: List<ContentsItem>
)
data class MusicDescriptionShelfRenderer(val header: TextRuns, val description: TextRuns)
data class MusicCardShelfRenderer(
    val thumbnail: ThumbnailRenderer,
    val title: TextRuns,
    val subtitle: TextRuns,
    val menu: YoutubeiNextResponse.Menu,
    val header: Header
) {
    fun getMediaItem(): MediaItem {
        val item: MediaItem

        val endpoint = title.runs!!.first().navigationEndpoint!!
        if (endpoint.watchEndpoint != null) {
            item = Song.fromId(endpoint.watchEndpoint.videoId!!)
        }
        else {
            item = endpoint.browseEndpoint!!.getMediaItem()!!
        }

        item.editData {
            supplyTitle(title.first_text, true)
            supplyDataFromSubtitle(subtitle.runs!!)
            supplyThumbnailProvider(thumbnail.toThumbnailProvider(), true)
        }

        return item
    }
}

data class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint,
    val title: TextRuns,
    val subtitle: TextRuns? = null,
    val thumbnailRenderer: ThumbnailRenderer,
    val menu: YoutubeiNextResponse.Menu? = null
) {
    fun getArtist(host_item: MediaItem): Artist? {
        for (run in subtitle?.runs ?: emptyList()) {
            val browse_endpoint = run.navigationEndpoint?.browseEndpoint

            val endpoint_type = browse_endpoint?.getMediaItemType()
            if (endpoint_type == MediaItemType.ARTIST) {
                return Artist.fromId(browse_endpoint.browseId).editArtistData { supplyTitle(run.text) }
            }
        }

        if (host_item is Song) {
            val index = if (host_item.song_type == SongType.VIDEO) 0 else 1
            subtitle?.runs?.getOrNull(index)?.also {
                return Artist.createForItem(host_item).editArtistData { supplyTitle(it.text) }
            }
        }

        return null
    }
}
data class ThumbnailRenderer(val musicThumbnailRenderer: MusicThumbnailRenderer) {
    fun toThumbnailProvider(): MediaItemThumbnailProvider {
        return MediaItemThumbnailProvider.fromThumbnails(musicThumbnailRenderer.thumbnail.thumbnails)!!
    }
}
data class MusicResponsiveListItemRenderer(
    val playlistItemData: RendererPlaylistItemData? = null,
    val flexColumns: List<FlexColumn>? = null,
    val fixedColumns: List<FixedColumn>? = null,
    val thumbnail: ThumbnailRenderer? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val menu: YoutubeiNextResponse.Menu? = null
)
data class RendererPlaylistItemData(val videoId: String, val playlistSetVideoId: String? = null)

data class FlexColumn(val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemColumnRenderer)
data class FixedColumn(val musicResponsiveListItemFixedColumnRenderer: MusicResponsiveListItemColumnRenderer)
data class MusicResponsiveListItemColumnRenderer(val text: TextRuns)

data class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null) {
    // Pair(item, playlistSetVideoId)
    fun toMediaItem(hl: String): Pair<MediaItem, String?>? {
        if (musicTwoRowItemRenderer != null) {
            val renderer = musicTwoRowItemRenderer

            // Video
            if (renderer.navigationEndpoint.watchEndpoint?.videoId != null) {
                val first_thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer.thumbnail.thumbnails.first()
                return Pair(
                    Song.fromId(renderer.navigationEndpoint.watchEndpoint.videoId).editSongData {
                        // TODO | Is this the best way of checking?
                        supplySongType(if (first_thumbnail.height == first_thumbnail.width) SongType.SONG else SongType.VIDEO)
                        supplyTitle(renderer.title.first_text)
                        supplyArtist(renderer.getArtist(data_item))
                        supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
                    },
                    null
                )
            }

            val item: MediaItem

            if (renderer.navigationEndpoint.watchPlaylistEndpoint != null) {
                if (!Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                    return null
                }

                item = AccountPlaylist
                    .fromId(renderer.navigationEndpoint.watchPlaylistEndpoint.playlistId)
                    .editPlaylistData {
                        supplyPlaylistType(PlaylistType.RADIO, true)
                        supplyTitle(renderer.title.first_text)
                        supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
                    }
            }
            else {
                // Playlist or artist
                val browse_id = renderer.navigationEndpoint.browseEndpoint!!.browseId
                val page_type = renderer.navigationEndpoint.browseEndpoint.getPageType()!!

                item = when (page_type) {
                    "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> {
                        if (AccountPlaylist.formatId(browse_id).startsWith("RDAT") && !Settings.get<Boolean>(Settings.KEY_FEED_SHOW_RADIOS)) {
                            return null
                        }

                        AccountPlaylist
                            .fromId(browse_id)
                            .editPlaylistData {
                                supplyPlaylistType(when (page_type) {
                                    "MUSIC_PAGE_TYPE_ALBUM" -> PlaylistType.ALBUM
                                    "MUSIC_PAGE_TYPE_PLAYLIST" -> PlaylistType.PLAYLIST
                                    else -> PlaylistType.AUDIOBOOK
                                }, true)
                                supplyArtist(renderer.getArtist(data_item))
                                supplyTitle(renderer.title.first_text)
                                supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
                            }
                            .apply {
                                is_editable = renderer.menu?.menuRenderer?.items
                                    ?.any { it.menuNavigationItemRenderer?.icon?.iconType == "DELETE" } == true
                            }
                    }
                    "MUSIC_PAGE_TYPE_ARTIST" -> Artist.fromId(browse_id)
                    else -> throw NotImplementedError("$page_type ($browse_id)")
                }
            }

            return Pair(item, null)
        }
        else if (musicResponsiveListItemRenderer != null) {
            val renderer = musicResponsiveListItemRenderer

            var video_id: String? = renderer.playlistItemData?.videoId ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
            var video_is_main: Boolean = true

            var title: String? = null
            var artist: Artist? = null
            var playlist: AccountPlaylist? = null
            var duration: Long? = null

            if (video_id == null) {
                val page_type = renderer.navigationEndpoint?.browseEndpoint?.getPageType()
                when (page_type) {
                    "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> {
                        video_is_main = false
                        playlist = AccountPlaylist
                            .fromId(renderer.navigationEndpoint.browseEndpoint.browseId)
                            .editPlaylistData {
                                supplyPlaylistType(PlaylistType.fromTypeString(page_type), true)
                            }
                    }
                    "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                        video_is_main = false
                        artist = Artist.fromId(renderer.navigationEndpoint.browseEndpoint.browseId)
                    }
                }
            }

            if (renderer.flexColumns != null) {
                for (column in renderer.flexColumns.withIndex()) {
                    val text = column.value.musicResponsiveListItemFlexColumnRenderer.text
                    if (text.runs == null) {
                        continue
                    }

                    if (column.index == 0) {
                        title = text.first_text
                    }

                    for (run in text.runs!!) {
                        if (run.navigationEndpoint == null) {
                            continue
                        }

                        if (run.navigationEndpoint.watchEndpoint != null) {
                            if (video_id == null) {
                                video_id = run.navigationEndpoint.watchEndpoint.videoId!!
                            }
                            continue
                        }

                        val browse_endpoint = run.navigationEndpoint.browseEndpoint
                        when (browse_endpoint?.getPageType()) {
                            "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                                if (artist == null) {
                                    artist = Artist.fromId(browse_endpoint.browseId).editArtistData { supplyTitle(run.text) }
                                }
                            }
                        }
                    }
                }
            }

            if (renderer.fixedColumns != null) {
                for (column in renderer.fixedColumns) {
                    val text = column.musicResponsiveListItemFixedColumnRenderer.text.first_text
                    val parsed = parseYoutubeDurationString(text, hl)
                    if (parsed != null) {
                        duration = parsed
                        break
                    }
                }
            }

            val item_data: MediaItemData
            if (video_id != null) {
                item_data = Song.fromId(video_id).editSongDataManual {
                    supplyDuration(duration, true)
                    renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.also {
                        supplySongType(if (it.height == it.width) SongType.SONG else SongType.VIDEO)
                    }
                }
            }
            else if (video_is_main) {
                return null
            }
            else {
                item_data = (playlist?.data?.apply { supplyTotalDuration(duration, true) }) ?: artist?.data ?: return null
            }

            // Handle songs with no artist (or 'Various artists')
            if (artist == null) {
                if (renderer.flexColumns != null && renderer.flexColumns.size > 1) {
                    val text = renderer.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text
                    if (text.runs != null) {
                        artist = Artist.createForItem(item_data.data_item).editArtistData { supplyTitle(text.first_text) }
                    }
                }

                if (artist == null && renderer.menu != null) {
                    for (item in renderer.menu.menuRenderer.items) {
                        val browse_endpoint = (item.menuNavigationItemRenderer ?: continue).navigationEndpoint.browseEndpoint ?: continue
                        if (browse_endpoint.getMediaItemType() == MediaItemType.ARTIST) {
                            artist = Artist.fromId(browse_endpoint.browseId)
                            break
                        }
                    }
                }
            }

            with(item_data) {
                supplyTitle(title)
                supplyArtist(artist)
                supplyThumbnailProvider(renderer.thumbnail?.toThumbnailProvider())
                save()
            }

            return Pair(item_data.data_item, renderer.playlistItemData?.playlistSetVideoId)
        }

        throw NotImplementedError()
    }
}
