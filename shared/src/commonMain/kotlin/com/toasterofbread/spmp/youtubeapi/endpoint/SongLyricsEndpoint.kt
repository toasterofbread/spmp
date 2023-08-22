package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SongLyricsEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getSongLyrics(lyrics_id: String): Result<String>
}
