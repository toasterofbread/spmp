package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.resources.uilocalisation.YoutubeLocalisedString
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.ChipCloudRendererHeader
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint.YTMGetHomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse
import com.toasterofbread.spmp.platform.AppContext

data class YoutubeiBrowseResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
    val header: Header?
) {
    val ctoken: String?
        get() = continuationContents?.sectionListContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation
            ?: contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation

    fun getShelves(has_continuation: Boolean): List<YoutubeiShelf> {
        return if (has_continuation) continuationContents?.sectionListContinuation?.contents ?: emptyList()
        else contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents ?: emptyList()
    }

    fun getHeaderChips(context: AppContext): List<FilterChip>? =
        contents?.singleColumnBrowseResultsRenderer?.tabs?.first()?.tabRenderer?.content?.sectionListRenderer?.header?.chipCloudRenderer?.chips?.map {
            FilterChip(
                YoutubeLocalisedString.Type.FILTER_CHIP.createFromKey(it.chipCloudChipRenderer.text!!.first_text, context),
                it.chipCloudChipRenderer.navigationEndpoint.browseEndpoint!!.params!!
            )
        }

    data class Contents(
        val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?
    )
    data class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    data class Tab(val tabRenderer: TabRenderer)
    data class TabRenderer(val content: Content?)
    data class Content(val sectionListRenderer: SectionListRenderer?)

    data class SectionListRenderer(val contents: List<YoutubeiShelf>?, val header: ChipCloudRendererHeader?, val continuations: List<YoutubeiNextResponse.Continuation>?)
    class TwoColumnBrowseResultsRenderer(val tabs: List<Tab>, val secondaryContents: SecondaryContents) {
        class SecondaryContents(val sectionListRenderer: SectionListRenderer)
    }

    data class ContinuationContents(val sectionListContinuation: SectionListRenderer?, val musicPlaylistShelfContinuation: YTMGetHomeFeedEndpoint.MusicShelfRenderer?)
}
