package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider

open class HeaderRenderer(
    val title: TextRuns?,
    val strapline: TextRuns?,
    val subscriptionButton: SubscriptionButton?,
    val description: TextRuns?,
    val thumbnail: Thumbnails?,
    val foregroundThumbnail: Thumbnails?,
    val subtitle: TextRuns?,
    val secondSubtitle: TextRuns?,
    val moreContentButton: MoreContentButton?
) {
    fun getThumbnails(): List<MediaItemThumbnailProvider.Thumbnail> {
        return (foregroundThumbnail ?: thumbnail)?.thumbnails ?: emptyList()
    }
}
