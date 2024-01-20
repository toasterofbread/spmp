package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class ArtistShuffleEndpoint: YoutubeApi.Endpoint() {
    data class RadioData(val items: List<SongData>, var continuation: String?)

    abstract suspend fun getArtistShuffle(
        artist: Artist,
        continuation: String?
    ): Result<RadioData>
}
