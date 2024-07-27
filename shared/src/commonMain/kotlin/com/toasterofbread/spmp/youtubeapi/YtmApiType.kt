package com.toasterofbread.spmp.youtubeapi

import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.resources.Language
import dev.toastbits.ytmkt.impl.unimplemented.UnimplementedYtmApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.YtmApi

enum class YtmApiType {
    YOUTUBE_MUSIC,
    UNIMPLEMENTED_FOR_TESTING;

    fun isSelectable(): Boolean = this != UNIMPLEMENTED_FOR_TESTING

    fun getDefaultUrl(): String =
        when (this) {
            YOUTUBE_MUSIC -> YoutubeiApi.DEFAULT_API_URL
            UNIMPLEMENTED_FOR_TESTING -> ""
        }

    fun instantiate(context: AppContext, api_url: String, data_language: Language): YtmApi =
        when (this) {
            YOUTUBE_MUSIC -> SpMpYoutubeiApi(context, api_url, data_language)
            UNIMPLEMENTED_FOR_TESTING -> UnimplementedYtmApi()
        }

    companion object {
        val DEFAULT: YtmApiType = YOUTUBE_MUSIC
    }
}
