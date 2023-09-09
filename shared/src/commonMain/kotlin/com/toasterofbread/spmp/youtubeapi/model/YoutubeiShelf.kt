package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMGetHomeFeedEndpoint

data class YoutubeiShelf(
    val musicShelfRenderer: YTMGetHomeFeedEndpoint.MusicShelfRenderer?,
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
    val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
    val musicPlaylistShelfRenderer: YTMGetHomeFeedEndpoint.MusicShelfRenderer?,
    val musicCardShelfRenderer: MusicCardShelfRenderer?,
    val gridRenderer: GridRenderer?,
    val itemSectionRenderer: ItemSectionRenderer?
) {
    val title: TextRun? get() =
        if (musicShelfRenderer != null) musicShelfRenderer.title?.runs?.firstOrNull()
        else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.getRenderer()?.title?.runs?.firstOrNull()
        else if (musicDescriptionShelfRenderer != null) musicDescriptionShelfRenderer.header?.runs?.firstOrNull()
        else if (musicCardShelfRenderer != null) musicCardShelfRenderer.title.runs?.firstOrNull()
        else if (gridRenderer != null) gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()
        else null

    val description: String? get() = musicDescriptionShelfRenderer?.description?.first_text

    fun getNavigationEndpoint(): NavigationEndpoint? =
        musicShelfRenderer?.bottomEndpoint ?: musicCarouselShelfRenderer?.header?.getRenderer()?.moreContentButton?.buttonRenderer?.navigationEndpoint

    fun getMediaItems(hl: String): List<MediaItemData> =
        (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer!!.items).mapNotNull {
            it.toMediaItemData(hl)?.first
        }

    fun getMediaItemsOrNull(hl: String): List<MediaItemData>? =
        (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer?.items)?.mapNotNull {
            it.toMediaItemData(hl)?.first
        }

    fun getMediaItemsAndSetIds(hl: String): List<Pair<MediaItemData, String?>> =
        (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer?.items ?: emptyList()).mapNotNull {
            it.toMediaItemData(hl)
        }

    fun getRenderer(): Any? =
        musicShelfRenderer ?:
        musicCarouselShelfRenderer ?:
        musicDescriptionShelfRenderer ?:
        musicPlaylistShelfRenderer ?:
        musicCardShelfRenderer ?:
        gridRenderer ?:
        itemSectionRenderer
}
