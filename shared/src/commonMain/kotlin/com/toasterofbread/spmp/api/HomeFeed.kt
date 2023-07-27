package com.toasterofbread.spmp.api

import SpMp
import com.beust.klaxon.Json
import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.api.model.YoutubeiShelf
import com.toasterofbread.spmp.api.model.YoutubeiShelfContentsItem
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.Cache
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylist
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.model.FilterChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.use
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

data class HomeFeedLoadResult(
    val layouts: List<MediaItemLayout>,
    val ctoken: String?,
    val filter_chips: List<FilterChip>?
)

suspend fun getHomeFeed(
    min_rows: Int = -1,
    allow_cached: Boolean = true,
    params: String? = null,
    continuation: String? = null
): Result<HomeFeedLoadResult> = withContext(Dispatchers.IO) {
    val hl = SpMp.data_language
    val suffix = params ?: ""
    val rows_cache_key = "feed_rows$suffix"
    val ctoken_cache_key = "feed_ctoken$suffix"
    val chips_cache_key = "feed_chips$suffix"

    if (allow_cached && continuation == null) {
        Cache.get(rows_cache_key)?.use { cached_rows ->
            val rows = Api.klaxon.parseArray<MediaItemLayout>(cached_rows)!!

            val ctoken = Cache.get(ctoken_cache_key)?.use {
                it.readText()
            }

            val chips: List<FilterChip>? = Cache.get(chips_cache_key)?.use {
                Api.klaxon.parseArray(it)!!
            }

            return@withContext Result.success(HomeFeedLoadResult(rows, ctoken, chips))
        }
    }

    var last_request: Request? = null

    fun performRequest(ctoken: String?): Result<YoutubeiBrowseResponse> {
        last_request = null

        val endpoint = "/youtubei/v1/browse"
        val request = Request.Builder()
            .ytUrl(if (ctoken == null) endpoint else "$endpoint?ctoken=$ctoken&continuation=$ctoken&type=next")
            .addYtHeaders()
            .post(
                Api.getYoutubeiRequestBody(params?.let {
                    mapOf("params" to it)
                })
            )
            .build()

        val result = Api.request(request)
        val stream = result.getOrNull()?.getStream() ?: return result.cast()

        last_request = request

        try {
            return stream.use {
                Result.success(Api.klaxon.parse(it)!!)
            }
        }
        catch (e: Throwable) {
            return Result.failure(e)
        }
    }

    try {
        var data = performRequest(continuation).getOrThrow()

        val rows: MutableList<MediaItemLayout> = processRows(data.getShelves(continuation != null), hl).toMutableList()
        check(rows.isNotEmpty())

        val chips = data.getHeaderChips()

        var ctoken: String? = data.ctoken
        while (min_rows >= 1 && rows.size < min_rows) {
            if (ctoken == null) {
                break
            }

            data = performRequest(ctoken).getOrThrow()

            val shelves = data.getShelves(true)
            check(shelves.isNotEmpty())
            rows.addAll(processRows(shelves, hl))

            ctoken = data.ctoken
        }

        if (continuation == null) {
            Cache.set(rows_cache_key, Api.klaxon.toJsonString(rows).reader(), CACHE_LIFETIME)
            Cache.set(ctoken_cache_key, ctoken?.reader(), CACHE_LIFETIME)
            Cache.set(chips_cache_key, Api.klaxon.toJsonString(chips).reader(), CACHE_LIFETIME)
        }

        return@withContext Result.success(HomeFeedLoadResult(rows, ctoken, chips))
    }
    catch (error: Throwable) {
        val request = last_request ?: return@withContext Result.failure(error)
        return@withContext Result.failure(
            DataParseException.ofYoutubeJsonRequest(
                request,
                cause = error,
                klaxon = Api.klaxon
            )
        )
    }
}

private suspend fun processRows(rows: List<YoutubeiShelf>, hl: String): List<MediaItemLayout> {
    val ret = mutableListOf<MediaItemLayout>()
    for (row in rows) {
        if (!row.implemented) {
            continue
        }

        when (val renderer = row.getRenderer()) {
            is MusicDescriptionShelfRenderer -> continue
            is YoutubeiHeaderContainer -> {
                val header = renderer.header?.header_renderer ?: continue

                fun add(
                    title: LocalisedYoutubeString,
                    subtitle: LocalisedYoutubeString? = null,
                    thumbnail_source: MediaItemLayout.ThumbnailSource? =
                        header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.let {
                            MediaItemLayout.ThumbnailSource(null, url = it.url)
                        },
                    media_item_type: MediaItemType? = null,
                    view_more: MediaItemLayout.ViewMore? = null,
                    type: MediaItemLayout.Type? = null
                ) {
                    val items = row.getMediaItems(hl).toMutableList()

                    ret.add(
                        MediaItemLayout(
                        title, subtitle,
                        items = items,
                        thumbnail_source = thumbnail_source,
                        view_more = view_more,
                        thumbnail_item_type = media_item_type,
                        type = type
                    )
                    )
                }

                val browse_endpoint = header.title?.runs?.first()?.navigationEndpoint?.browseEndpoint
                if (browse_endpoint == null) {
                    add(
                        LocalisedYoutubeString.Type.HOME_FEED.create(header.title!!.first_text),
                        header.subtitle?.first_text?.let { LocalisedYoutubeString.Type.HOME_FEED.create(it) }
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
                        LocalisedYoutubeString.Type.APP.create(view_more_page_title_key),
                        null,
                        view_more = MediaItemLayout.ViewMore(list_page_browse_id = browse_endpoint.browseId),
                        type = when(browse_endpoint.browseId) {
                            "FEmusic_listen_again" -> MediaItemLayout.Type.GRID_ALT
                            else -> null
                        }
                    )
                    continue
                }

                val page_type = browse_endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType!!

                val media_item = MediaItemType.fromBrowseEndpointType(page_type)!!.fromId(browse_endpoint.browseId).apply {
                    editData {
                        header.title.runs?.getOrNull(0)?.also { title ->
                            supplyTitle(title.text)
                        }
                    }
                }

                val thumbnail_source =
                    header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()?.let {
                        MediaItemLayout.ThumbnailSource(url = it.url)
                    }
                    ?: MediaItemLayout.ThumbnailSource(media_item = media_item)

                add(
                    LocalisedYoutubeString.Type.RAW.create(header.title.first_text),
                    header.subtitle?.first_text?.let { LocalisedYoutubeString.Type.HOME_FEED.create(it) },
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
                ?: contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

    fun getShelves(has_continuation: Boolean): List<YoutubeiShelf> {
        return if (has_continuation) continuationContents?.sectionListContinuation?.contents ?: emptyList()
               else contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents ?: emptyList()
    }

    fun getHeaderChips(): List<FilterChip>? =
        contents?.singleColumnBrowseResultsRenderer?.tabs?.first()?.tabRenderer?.content?.sectionListRenderer?.header?.chipCloudRenderer?.chips?.map {
            FilterChip(
                LocalisedYoutubeString.Type.FILTER_CHIP.create(it.chipCloudChipRenderer.text!!.first_text),
                it.chipCloudChipRenderer.navigationEndpoint.browseEndpoint!!.params!!
            )
        }

    data class Contents(
        val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer? = null,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer? = null
    )
    data class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content? = null)
    data class Content(val sectionListRenderer: SectionListRenderer)
    open class SectionListRenderer(val contents: List<YoutubeiShelf>? = null, val header: ChipCloudRendererHeader? = null, val continuations: List<YoutubeiNextResponse.Continuation>? = null)

    class TwoColumnBrowseResultsRenderer(val tabs: List<Tab>, val secondaryContents: SecondaryContents) {
        class SecondaryContents(val sectionListRenderer: SectionListRenderer)
    }

    data class ContinuationContents(val sectionListContinuation: SectionListRenderer? = null, val musicPlaylistShelfContinuation: MusicShelfRenderer? = null)
}

data class ItemSectionRenderer(val contents: List<ItemSectionRendererContent>)
data class ItemSectionRendererContent(val didYouMeanRenderer: DidYouMeanRenderer? = null)
data class DidYouMeanRenderer(val correctedQuery: TextRuns)

data class GridRenderer(val items: List<YoutubeiShelfContentsItem>, override val header: GridHeader? = null): YoutubeiHeaderContainer
data class GridHeader(val gridHeaderRenderer: HeaderRenderer): YoutubeiHeader {
    override val header_renderer: HeaderRenderer?
        get() = gridHeaderRenderer
}

data class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
data class BrowseEndpointContextMusicConfig(val pageType: String)
data class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
data class BrowseEndpoint(
    val browseId: String,
    val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null,
    val params: String? = null
) {
    fun getPageType(): String? = 
        browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
    fun getMediaItemType(): MediaItemType? = 
        getPageType()?.let { MediaItemType.fromBrowseEndpointType(it) }

    fun getMediaItem(): MediaItem? =
        getPageType()?.let { page_type ->
            MediaItem.fromBrowseEndpointType(page_type, browseId)
        }

    fun getViewMore(): MediaItemLayout.ViewMore {
        val item = getMediaItem()
        if (item != null) {
            return MediaItemLayout.ViewMore(media_item = item)
        }
        else {
            return MediaItemLayout.ViewMore(
                list_page_browse_id = browseId,
                browse_params = params 
            ) 
        }
    }
}
data class SearchEndpoint(val query: String, val params: String? = null)
data class WatchPlaylistEndpoint(val playlistId: String, val params: String)
data class ChannelCreationFormEndpoint(val channelCreationToken: String)
data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null,
    val searchEndpoint: SearchEndpoint? = null,
    val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null,
    val channelCreationFormEndpoint: ChannelCreationFormEndpoint? = null,
    val urlEndpoint: String? = null
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
            browseEndpoint.getViewMore().also { return it }
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
): YoutubeiHeader {
    fun getRenderer(): HeaderRenderer? {
        return musicCarouselShelfBasicHeaderRenderer
            ?: musicImmersiveHeaderRenderer
            ?: musicVisualHeaderRenderer
            ?: musicDetailHeaderRenderer
            ?: musicCardShelfHeaderBasicRenderer
            ?: musicEditablePlaylistDetailHeaderRenderer?.header?.getRenderer()
    }

    data class MusicEditablePlaylistDetailHeaderRenderer(val header: Header)

    override val header_renderer: HeaderRenderer?
        get() = getRenderer()
}

//val thumbnails = (header.obj("thumbnail") ?: header.obj("foregroundThumbnail")!!)
//    .obj("musicThumbnailRenderer")!!
//    .obj("thumbnail")!!
//    .array<JsonObject>("thumbnails")!!

interface YoutubeiHeaderContainer {
    val header: YoutubeiHeader?
}
interface YoutubeiHeader {
    val header_renderer: HeaderRenderer?
}

data class HeaderRenderer(
    val title: TextRuns? = null,
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

    fun firstTextOrNull(): String? = runs?.getOrNull(0)?.text
}
data class TextRun(val text: String, val strapline: TextRuns? = null, val navigationEndpoint: NavigationEndpoint? = null) {
    @Json(ignored = true)
    val browse_endpoint_type: String? get() = navigationEndpoint?.browseEndpoint?.getPageType()
}

data class MusicShelfRenderer(
    val title: TextRuns? = null,
    val contents: List<YoutubeiShelfContentsItem>? = null,
    val continuations: List<YoutubeiNextResponse.Continuation>? = null,
    val bottomEndpoint: NavigationEndpoint? = null
)
data class MoreContentButton(val buttonRenderer: ButtonRenderer)
data class ButtonRenderer(val navigationEndpoint: NavigationEndpoint)
data class MusicCarouselShelfRenderer(
    override val header: Header,
    val contents: List<YoutubeiShelfContentsItem>
): YoutubeiHeaderContainer
data class MusicDescriptionShelfRenderer(val header: TextRuns, val description: TextRuns)
data class MusicCardShelfRenderer(
    val thumbnail: ThumbnailRenderer,
    val title: TextRuns,
    val subtitle: TextRuns,
    val menu: YoutubeiNextResponse.Menu,
    override val header: Header
): YoutubeiHeaderContainer {
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

data class ThumbnailRenderer(val musicThumbnailRenderer: MusicThumbnailRenderer) {
    fun toThumbnailProvider(): MediaItemThumbnailProvider {
        return MediaItemThumbnailProvider.fromThumbnails(musicThumbnailRenderer.thumbnail.thumbnails)!!
    }
}

data class FlexColumn(val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemColumnRenderer)
data class FixedColumn(val musicResponsiveListItemFixedColumnRenderer: MusicResponsiveListItemColumnRenderer)
data class MusicResponsiveListItemColumnRenderer(val text: TextRuns)
