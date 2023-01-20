package com.spectre7.spmp.api

import okhttp3.Request

// https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_song_related
fun getSongRelated(id: String) {

    val browse_id: String

    val url = "https://music.youtube.com/youtubei/v1/browse"
    val request = Request.Builder()
        .url(if (ctoken == null) url else "$url?ctoken=$ctoken&continuation=$ctoken&type=next")
        .headers(getYTMHeaders())
        .post(getYoutubeiRequestBody(
        """
            {
                "browse": "$browse_id"
            }
        """
        ))
        .build()
        .build()
}