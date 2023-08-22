package com.toasterofbread.spmp.youtubeapi.formats

import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat

abstract class VideoFormatsEndpoint: YoutubeApi.Endpoint() {
    abstract fun getVideoFormats(id: String, filter: ((YoutubeVideoFormat) -> Boolean)? = null): Result<List<YoutubeVideoFormat>>
}
