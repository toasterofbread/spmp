package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.GridRenderer
import com.toasterofbread.spmp.api.ItemSectionRenderer
import com.toasterofbread.spmp.api.MusicCardShelfRenderer
import com.toasterofbread.spmp.api.MusicCarouselShelfRenderer
import com.toasterofbread.spmp.api.MusicDescriptionShelfRenderer
import com.toasterofbread.spmp.api.MusicShelfRenderer
import com.toasterofbread.spmp.api.MusicThumbnailRenderer
import com.toasterofbread.spmp.api.NavigationEndpoint
import com.toasterofbread.spmp.api.TextRun
import com.toasterofbread.spmp.api.TextRuns
import com.toasterofbread.spmp.api.Thumbnails
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData

data class YoutubeiShelf(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
    val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer? = null,
    val musicPlaylistShelfRenderer: MusicShelfRenderer? = null,
    val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
    val gridRenderer: GridRenderer? = null,
    val itemSectionRenderer: ItemSectionRenderer? = null,
    val musicTastebuilderShelfRenderer: Any? = null,
    val musicMultiRowListItemRenderer: MusicMultiRowListItemRenderer? = null,
    val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer? = null
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
            || musicMultiRowListItemRenderer != null
            || musicResponsiveHeaderRenderer != null
        ) { "No known shelf renderer" }
    }

    val implemented: Boolean get() = musicTastebuilderShelfRenderer == null

    val title: TextRun? get() =
        if (musicShelfRenderer != null) musicShelfRenderer.title?.runs?.firstOrNull()
        else if (musicCarouselShelfRenderer != null) musicCarouselShelfRenderer.header.getRenderer()?.title?.runs?.firstOrNull()
        else if (musicDescriptionShelfRenderer != null) musicDescriptionShelfRenderer.header.runs?.firstOrNull()
        else if (musicCardShelfRenderer != null) musicCardShelfRenderer.title.runs?.firstOrNull()
        else if (gridRenderer != null) gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()
        else null

    val description: String? get() = musicDescriptionShelfRenderer?.description?.first_text

    fun getNavigationEndpoint(): NavigationEndpoint? =
        musicShelfRenderer?.bottomEndpoint ?: musicCarouselShelfRenderer?.header?.getRenderer()?.moreContentButton?.buttonRenderer?.navigationEndpoint

    fun getMediaItems(hl: String): List<MediaItemData> =
        (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer!!.items).mapNotNull {
            it.toMediaItem(hl)?.first
        }

    fun getMediaItemsOrNull(hl: String): List<MediaItemData>? =
        (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer?.items)?.mapNotNull {
            it.toMediaItem(hl)?.first
        }

    fun getMediaItemsAndSetIds(hl: String): List<Pair<MediaItemData, String?>> =
        (musicShelfRenderer?.contents ?: musicCarouselShelfRenderer?.contents ?: musicPlaylistShelfRenderer?.contents ?: gridRenderer?.items ?: emptyList()).mapNotNull {
            it.toMediaItem(hl)
        }

    fun getRenderer(): Any? =
        musicShelfRenderer ?:
        musicCarouselShelfRenderer ?:
        musicDescriptionShelfRenderer ?:
        musicPlaylistShelfRenderer ?:
        musicCardShelfRenderer ?:
        gridRenderer ?:
        itemSectionRenderer ?:
        musicTastebuilderShelfRenderer
}

class MusicResponsiveHeaderRenderer(
    val thumbnail: Thumbnails,
    val title: TextRuns,
    val straplineThumbnail: Thumbnails,
    val straplineTextOne: TextRuns,
    val description: Description? = null
) {
    class Description(val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?)
}
