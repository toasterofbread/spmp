package com.spectre7.spmp.api

import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.utils.getString
import okhttp3.Request
import java.io.BufferedReader
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

open class HomeFeedRow {

    lateinit var items: List<MediaItem>
    var title: String = ""
        get() {
            if (field.isNotEmpty()) {
                return field
            }
            // TODO
            return "Title"
        }
    var subtitle: String? = null
    var thumbnail_provider: MediaItem.ThumbnailProvider? = null

    open fun viewMore(
        listPage: (url: String) -> Unit,
        viewArtist: (artist: Artist) -> Unit,
        viewPlaylist: (playlist: Playlist) -> Unit
    ) {}

    class UrlPage(
        private val url: String
    ): HomeFeedRow() {
        override fun viewMore(
            listPage: (url: String) -> Unit,
            viewArtist: (artist: Artist) -> Unit,
            viewPlaylist: (playlist: Playlist) -> Unit
        ) {
            listPage(url)
        }
    }

    class ArtistContent(
        val artist: Artist
    ): HomeFeedRow() {
        init {
            title = artist.title!!
        }

        override fun viewMore(
            listPage: (url: String) -> Unit,
            viewArtist: (artist: Artist) -> Unit,
            viewPlaylist: (playlist: Playlist) -> Unit
        ) {
            viewArtist(artist)
        }
    }

    class PlaylistContent(
        val playlist: Playlist
    ): HomeFeedRow() {
        init {
            title = playlist.title!!
        }

        override fun viewMore(
            listPage: (url: String) -> Unit,
            viewArtist: (artist: Artist) -> Unit,
            viewPlaylist: (playlist: Playlist) -> Unit
        ) {
            viewPlaylist(playlist)
        }
    }
}

fun getHomeFeed(min_rows: Int = -1, allow_cached: Boolean = true, continuation: String? = null): Result<Pair<List<HomeFeedRow>, String?>> {

    fun postRequest(ctoken: String?): Result<BufferedReader> {
        val url = "https://music.youtube.com/youtubei/v1/browse"
        val request = Request.Builder()
            .url(if (ctoken == null) url else "$url?ctoken=$ctoken&continuation=$ctoken&type=next")
            .headers(DataApi.getYTMHeaders())
            .post(DataApi.getYoutubeiRequestBody())
            .build()

        val result = DataApi.request(request)
        if (!result.isSuccess) {
            return result.cast()
        }

        return Result.success(BufferedReader(result.getOrThrowHere().body!!.charStream()))
    }

    fun processRows(rows: List<YoutubeiShelf>): List<HomeFeedRow> {
        val ret = mutableListOf<HomeFeedRow>()
        for (row in rows) {
            when (val renderer = row.getRenderer()) {
                is MusicDescriptionShelfRenderer -> continue
                is MusicCarouselShelfRenderer -> {
                    val header = renderer.header.musicCarouselShelfBasicHeaderRenderer!!

                    fun add(feed_row: HomeFeedRow, title: String? = null) {
                        feed_row.items = row.getMediaItems()
                        if (title != null) {
                            feed_row.title = title
                        }

                        val thumbnails = header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                        if (thumbnails != null) {
                            feed_row.thumbnail_provider = MediaItem.ThumbnailProvider.fromThumbnails(thumbnails)
                        }
                        ret.add(feed_row)
                    }

                    val browse_endpoint = header.title.runs?.first()?.navigationEndpoint?.browseEndpoint
                    if (browse_endpoint == null) {
                        add(HomeFeedRow())
                        continue
                    }

                    when (browse_endpoint.browseId) {
                        "FEmusic_listen_again" -> {
                            add(HomeFeedRow.UrlPage("https://music.youtube.com/listen_again"), getString(R.string.home_feed_listen_again))
                            continue
                        }
                        "FEmusic_mixed_for_you" -> {
                            add(HomeFeedRow.UrlPage("https://music.youtube.com/mixed_for_you"), getString(R.string.home_feed_mixed_for_you))
                            continue
                        }
                        "FEmusic_new_releases_albums" -> {
                            add(HomeFeedRow.UrlPage("https://music.youtube.com/new_releases/albums"), getString(R.string.home_feed_new_releases))
                            continue
                        }
                        "FEmusic_moods_and_genres" -> {
                            add(HomeFeedRow.UrlPage("https://music.youtube.com/moods_and_genres"), getString(R.string.home_feed_moods_and_genres))
                            continue
                        }
                        // https://music.youtube.com/charts
                    }

                    val page_type = browse_endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    when (page_type) {
                        "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> add(
                            HomeFeedRow.ArtistContent(
                                Artist.fromId(browse_endpoint.browseId).supplyTitle(header.title.first_text) as Artist
                            )
                        )
                        "MUSIC_PAGE_TYPE_PLAYLIST" -> add(
                            HomeFeedRow.PlaylistContent(
                                Playlist.fromId(browse_endpoint.browseId).supplyTitle(header.title.first_text) as Playlist
                            )
                        )
                        else -> throw NotImplementedError(browse_endpoint.toString())
                    }
                }
                else -> throw NotImplementedError(row.getRenderer().toString())
            }
        }

        return ret
    }

    val rows: MutableList<HomeFeedRow>
    var response_reader: BufferedReader? = null

    val cache_key = "feed"
    if (allow_cached && continuation == null) {
        response_reader = Cache.get(cache_key)
    }

    if (response_reader == null) {
        val result = postRequest(continuation)
        if (!result.isSuccess) {
            return result.cast()
        }

        response_reader = result.getOrThrowHere()

        if (continuation == null) {
            Cache.set(cache_key, response_reader, CACHE_LIFETIME)
            response_reader.close()
            response_reader = Cache.get(cache_key)!!
        }
    }

    var data: YoutubeiBrowseResponse = DataApi.klaxon.parse(response_reader)!!
    response_reader.close()
    rows = processRows(data.getShelves(continuation != null)).toMutableList()

    var ctoken: String? = data.ctoken
    while (min_rows >= 1 && rows.size < min_rows) {
        if (ctoken == null) {
            break
        }

        val result = postRequest(ctoken)
        if (!result.isSuccess) {
            return result.cast()
        }
        data = DataApi.klaxon.parse(result.data)!!
        result.data.close()
        rows.addAll(processRows(data.getShelves(true)))

        ctoken = data.ctoken
    }

    return Result.success(Pair(rows, ctoken))
}

data class YoutubeiBrowseResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents? = null,
    val header: Header? = null
) {
    val ctoken: String?
        get() = continuationContents?.sectionListContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation
                ?: contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content?.sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

    fun getShelves(has_continuation: Boolean): List<YoutubeiShelf> {
        return if (has_continuation) continuationContents!!.sectionListContinuation.contents!! else contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content!!.sectionListRenderer.contents!!
    }

    data class Contents(val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer)
    data class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content? = null)
    data class Content(val sectionListRenderer: SectionListRenderer)
    open class SectionListRenderer(val contents: List<YoutubeiShelf>? = null, val continuations: List<YoutubeiNextResponse.Continuation>? = null)

    data class ContinuationContents(val sectionListContinuation: SectionListRenderer)
}

data class YoutubeiShelf(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
    val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer? = null,
    val musicPlaylistShelfRenderer: MusicShelfRenderer? = null
) {
    init {
        assert(musicShelfRenderer != null || musicCarouselShelfRenderer != null || musicDescriptionShelfRenderer != null || musicPlaylistShelfRenderer != null)
    }

    val title: TextRun? get() =
        if (musicShelfRenderer != null) musicShelfRenderer.title?.runs?.firstOrNull()
        else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.getRenderer().title.runs?.firstOrNull()
        else if (musicDescriptionShelfRenderer != null) musicDescriptionShelfRenderer.header.runs?.firstOrNull()
        else null

    fun getDescription(): String? {
        return musicDescriptionShelfRenderer?.description?.first_text
    }

    fun getMediaItems(): List<MediaItem> {
        return (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer!!.contents).mapNotNull {
            val item = it.toMediaItem()
            item?.saveToCache()
            return@mapNotNull item
        }
    }

    fun getRenderer(): Any {
        return musicShelfRenderer ?: musicCarouselShelfRenderer ?: musicDescriptionShelfRenderer!!
    }
}

data class WatchEndpoint(val videoId: String? = null, val playlistId: String? = null)
data class BrowseEndpointContextMusicConfig(val pageType: String)
data class BrowseEndpointContextSupportedConfigs(val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig)
data class BrowseEndpoint(val browseId: String, val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null) {
    val page_type: String? get() = browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
}
data class SearchEndpoint(val query: String, val params: String)
data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null,
    val searchEndpoint: SearchEndpoint? = null
)
data class Header(
    val musicCarouselShelfBasicHeaderRenderer: HeaderRenderer? = null,
    val musicImmersiveHeaderRenderer: HeaderRenderer? = null,
    val musicVisualHeaderRenderer: HeaderRenderer? = null,
    val musicDetailHeaderRenderer: HeaderRenderer? = null,
    val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer? = null
) {
    fun getRenderer(): HeaderRenderer {
        return musicCarouselShelfBasicHeaderRenderer ?: musicImmersiveHeaderRenderer ?: musicVisualHeaderRenderer ?: musicDetailHeaderRenderer ?: musicEditablePlaylistDetailHeaderRenderer!!.header.getRenderer()
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
    val foregroundThumbnail: Thumbnails? = null
) {
    fun getThumbnails(): List<MediaItem.ThumbnailProvider.Thumbnail> {
        return (thumbnail ?: foregroundThumbnail)?.thumbnails ?: emptyList()
    }
}
data class SubscriptionButton(val subscribeButtonRenderer: SubscribeButtonRenderer)
data class SubscribeButtonRenderer(val subscribed: Boolean, val subscriberCountText: TextRuns, val channelId: String)
data class Thumbnails(val musicThumbnailRenderer: MusicThumbnailRenderer? = null, val croppedSquareThumbnailRenderer: MusicThumbnailRenderer? = null) {
    init {
        assert(musicThumbnailRenderer != null || croppedSquareThumbnailRenderer != null)
    }

    val thumbnails: List<MediaItem.ThumbnailProvider.Thumbnail> get() = (musicThumbnailRenderer ?: croppedSquareThumbnailRenderer!!).thumbnail.thumbnails
}
data class MusicThumbnailRenderer(val thumbnail: Thumbnail) {
    data class Thumbnail(val thumbnails: List<MediaItem.ThumbnailProvider.Thumbnail>)
}
data class TextRuns(val runs: List<TextRun>? = null) {
    val first_text: String get() = runs!![0].text
}
data class TextRun(val text: String, val strapline: TextRuns? = null, val navigationEndpoint: NavigationEndpoint? = null)

data class MusicShelfRenderer(val title: TextRuns? = null, val contents: List<ContentsItem>)
data class MusicCarouselShelfRenderer(val header: Header, val contents: List<ContentsItem>)
data class MusicDescriptionShelfRenderer(val header: TextRuns, val description: TextRuns)

data class MusicTwoRowItemRenderer(val navigationEndpoint: NavigationEndpoint, val title: TextRuns, val subtitle: TextRuns, val thumbnailRenderer: ThumbnailRenderer) {
    fun getArtist(): Artist? {
        return subtitle.runs!!.firstOrNull {
            it.navigationEndpoint?.browseEndpoint?.page_type == "MUSIC_PAGE_TYPE_ARTIST"
        }?.let { run ->
            Artist.fromId(run.navigationEndpoint!!.browseEndpoint!!.browseId).supplyTitle(run.text)
        } as Artist?
    }
}
data class ThumbnailRenderer(val musicThumbnailRenderer: MusicThumbnailRenderer) {
    fun toThumbnailProvider(): MediaItem.ThumbnailProvider {
        return MediaItem.ThumbnailProvider.fromThumbnails(musicThumbnailRenderer.thumbnail.thumbnails)!!
    }
}
data class MusicResponsiveListItemRenderer(
    val playlistItemData: PlaylistItemData? = null,
    val flexColumns: List<FlexColumn>? = null,
    val thumbnail: ThumbnailRenderer? = null,
    val navigationEndpoint: NavigationEndpoint? = null
)
data class PlaylistItemData(val videoId: String)
data class FlexColumn(val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer) {
    val runs: TextRuns get() = musicResponsiveListItemFlexColumnRenderer.text
}
data class MusicResponsiveListItemFlexColumnRenderer(val text: TextRuns)

data class ContentsItem(val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null, val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null) {
    fun toMediaItem(): MediaItem? {
        if (musicTwoRowItemRenderer != null) {
            val renderer = musicTwoRowItemRenderer

            // Video
            if (renderer.navigationEndpoint.watchEndpoint?.videoId != null) {
                return Song.fromId(renderer.navigationEndpoint.watchEndpoint.videoId)
                    .supplyTitle(renderer.title.first_text)
                    .supplyArtist(renderer.getArtist())
                    .supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
            }

            // Playlist or artist
            val browse_id = renderer.navigationEndpoint.browseEndpoint!!.browseId
            val page_type = renderer.navigationEndpoint.browseEndpoint.page_type!!

            return when (page_type) {
                "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST", "MUSIC_PAGE_TYPE_AUDIOBOOK" -> Playlist.fromId(browse_id).supplyArtist(renderer.getArtist())
                "MUSIC_PAGE_TYPE_ARTIST" -> Artist.fromId(browse_id)
                else -> throw NotImplementedError("$page_type ($browse_id)")
            }.supplyTitle(renderer.title.first_text).supplyThumbnailProvider(renderer.thumbnailRenderer.toThumbnailProvider())
        }
        else if (musicResponsiveListItemRenderer != null) {
            val renderer = musicResponsiveListItemRenderer

            var video_id: String? = renderer.playlistItemData?.videoId ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
            var video_is_main: Boolean = true

            var title: String? = null
            var artist: Artist? = null
            var playlist: Playlist? = null

            if (video_id == null) {
                val page_type = renderer.navigationEndpoint?.browseEndpoint?.page_type
                when (page_type) {
                    "MUSIC_PAGE_TYPE_ALBUM", "MUSIC_PAGE_TYPE_PLAYLIST" -> {
                        video_is_main = false
                        playlist = Playlist
                            .fromId(renderer.navigationEndpoint.browseEndpoint.browseId)
                            .supplyIsAlbum(page_type == "MUSIC_PAGE_TYPE_ALBUM", true) as Playlist
                    }
                    "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                        video_is_main = false
                        artist = Artist
                            .fromId(renderer.navigationEndpoint.browseEndpoint.browseId)
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

                    for (run in text.runs) {
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
                        when (browse_endpoint?.page_type) {
                            "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> {
                                if (artist == null) {
                                    artist = Artist.fromId(browse_endpoint.browseId).supplyTitle(run.text) as Artist
                                }
                            }
                        }
                    }
                }
            }

            val ret: MediaItem

            if (video_id != null) {
                ret = Song.fromId(video_id)
            }
            else if (video_is_main) {
                return null
            }
            else {
                ret = playlist ?: artist ?: return null
            }

            return ret.supplyTitle(title).supplyArtist(artist ?: Artist.UNKNOWN).supplyThumbnailProvider(renderer.thumbnail?.toThumbnailProvider())
        }

        throw NotImplementedError()
    }
}
