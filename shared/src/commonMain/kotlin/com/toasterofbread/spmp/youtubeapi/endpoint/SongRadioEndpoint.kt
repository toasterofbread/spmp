package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SongRadioEndpoint: YoutubeApi.Endpoint() {
    data class RadioData(val items: List<SongData>, var continuation: String?, val filters: List<List<RadioBuilderModifier>>?)

    abstract suspend fun getSongRadio(
        video_id: String,
        continuation: String?,
        filters: List<RadioBuilderModifier> = emptyList()
    ): Result<RadioData>
}
