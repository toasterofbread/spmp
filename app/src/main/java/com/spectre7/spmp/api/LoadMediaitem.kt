package com.spectre7.spmp.api

import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.utils.getString
import okhttp3.Request

private class ApiResponse(val items: List<MediaItem.YTApiDataResponse>)

fun loadMediaItemData(item: MediaItem): Result<MediaItem> {
    synchronized(item.loading_lock) {
        if (item.load_status == MediaItem.LoadStatus.LOADED) {
            return Result.success(item)
        }

        if (item.load_status == MediaItem.LoadStatus.LOADING) {
            item.loading_lock.wait()
            return if (item.load_status == MediaItem.LoadStatus.LOADED) Result.success(item) else Result.failure(RuntimeException())
        }

        val type: String
        val part: String
        when (item) {
            is Song -> {
                type = "videos"
                part = "contentDetails,snippet"
            }
            is Artist -> {
                type = "channels"
                part = "contentDetails,snippet,statistics"
            }
            is Playlist -> {
                type = "playlists"
                part = "snippet"
            }
            else -> throw NotImplementedError()
        }

        val request = Request.Builder()
            .url("https://www.googleapis.com/youtube/v3/$type?part=$part&id=${item.id}&hl=${MainActivity.data_language}&key=${getString(R.string.yt_api_key)}")
            .build()

        println("https://www.googleapis.com/youtube/v3/$type?part=contentDetails,snippet,statistics&id=${item.id}&hl=${MainActivity.data_language}&key=${getString(R.string.yt_api_key)}")
        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return Result.failure(response)
        }

        val str = response.body!!.string()

        val data = klaxon.parse<ApiResponse>(str)!!
        item.initWithData(data.items.first())
        item.loading_lock.notifyAll()

        return Result.success(item)
    }
}
