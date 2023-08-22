package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

data class HomeFeedLoadResult(
    val layouts: List<MediaItemLayout>,
    val ctoken: String?,
    val filter_chips: List<FilterChip>?
)

abstract class HomeFeedEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getHomeFeed(
        min_rows: Int = -1,
        allow_cached: Boolean = true,
        params: String? = null,
        continuation: String? = null
    ): Result<HomeFeedLoadResult>
}
