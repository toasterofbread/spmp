package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.SongData

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
    fun getMediaItem(): MediaItemData {
        val item: MediaItemData

        val endpoint = title.runs!!.first().navigationEndpoint!!
        if (endpoint.watchEndpoint != null) {
            item = SongData(endpoint.watchEndpoint.videoId!!)
        }
        else {
            item = endpoint.browseEndpoint!!.getMediaItem()!!
        }

        item.title = title.first_text
        item.thumbnail_provider = thumbnail.toThumbnailProvider()

        subtitle.runs?.also { runs ->
            item.supplyDataFromSubtitle(runs)
        }

        return item
    }
}
