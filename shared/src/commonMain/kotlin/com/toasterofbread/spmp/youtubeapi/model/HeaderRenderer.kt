package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider

open class HeaderRenderer(
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
