package com.toasterofbread.spmp.api.model

import com.toasterofbread.spmp.api.ChipCloudRendererHeader
import com.toasterofbread.spmp.api.MusicShelfRenderer
import com.toasterofbread.spmp.api.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString

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
    data class SectionListRenderer(val contents: List<YoutubeiShelf>? = null, val header: ChipCloudRendererHeader? = null, val continuations: List<YoutubeiNextResponse.Continuation>? = null)

    class TwoColumnBrowseResultsRenderer(val tabs: List<Tab>, val secondaryContents: SecondaryContents) {
        class SecondaryContents(val sectionListRenderer: SectionListRenderer)
    }

    data class ContinuationContents(val sectionListContinuation: SectionListRenderer? = null, val musicPlaylistShelfContinuation: MusicShelfRenderer? = null)
}
