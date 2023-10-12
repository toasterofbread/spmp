package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class ArtistRadioEndpoint: YoutubeApi.Endpoint() {
    data class RadioData(val items: List<SongData>, var continuation: String?)

    abstract suspend fun getArtistRadio(
        artist: Artist,
        continuation: String?
    ): Result<RadioData>
}
