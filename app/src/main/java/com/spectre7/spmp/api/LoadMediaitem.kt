package com.spectre7.spmp.api

import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.utils.getString
import okhttp3.Request
import java.time.Duration

private val CACHE_LIFETIME = Duration.ofDays(1)

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

        val cache_key = "d${item.id}${item.javaClass.simpleName}"
        val cached = Cache.get(cache_key)
        if (cached != null) {
            item.initWithData(klaxon.parse<ApiResponse>(cached)!!.items.first())
            item.loading_lock.notifyAll()
            return Result.success(item)
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

        val response = client.newCall(request).execute()
        if (response.code != 200) {
            return Result.failure(response)
        }

        val response_body = response.body!!.string()
        Cache.set(cache_key, response_body, CACHE_LIFETIME)

        val data = klaxon.parse<ApiResponse>(response_body)!!
        item.initWithData(data.items.first())
        item.loading_lock.notifyAll()

        return Result.success(item)
    }
}
