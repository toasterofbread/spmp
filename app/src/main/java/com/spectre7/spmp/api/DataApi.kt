package com.spectre7.spmp.api

import android.util.Log
import com.beust.klaxon.Klaxon
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.util.zip.GZIPInputStream
import com.spectre7.spmp.MainActivity
import com.spectre7.utils.getString
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.ArtistData
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.model.SongData
import com.spectre7.spmp.ui.layout.ResourceType
import com.spectre7.utils.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread
import com.chaquo.python.Python
import com.chaquo.python.PyException
import com.spectre7.ptl.Ptl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking

class DataApi {

    class HTTPGetRequest(private var request_url: String) {
        private var params: String = ""

        fun addParam(name: String, value: String) {
            if (params.isNotEmpty()) {
                params += "&"
            }
            params += URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }

        fun getRequestURL(): String {
            var ret: String = request_url
            if (params.isNotEmpty()) {
                ret += "?$params"
            }
            return ret
        }

        fun getResult(): String {

            val resp: StringBuffer

            with(URL(getRequestURL()).openConnection() as HttpURLConnection) {
                requestMethod = "GET"

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var input_lint = it.readLine()
                    while (input_lint != null) {
                        response.append(input_lint)
                        input_lint = it.readLine()
                    }
                    it.close()
                    resp = response
                }
            }

            return resp.toString()
        }

        fun reset(new_request_url: String) {
            request_url = new_request_url
            params = ""
        }
    }

    private class GetPlaylistItemsResponse(val items: List<VideoItem>) {
        class VideoItem(val snippet: Snippet, val contentDetails: ContentDetails)
        class Snippet(val publishedAt: String, val channelId: String, val title: String, val description: String)
        class ContentDetails(val videoId: String)

        fun getSongList(): List<Song> {
            var ret: MutableList<Song> = mutableListOf()
            for (video: VideoItem in items) {
                ret.add(Song(video.contentDetails.videoId, SongData(null, video.snippet.title, video.snippet.description), getArtist(video.snippet.channelId)!!, Date.from(Instant.parse(video.snippet.publishedAt))))
            }
            return ret
        }
    }

    private class VideoInfoResponse(val items: List<VideoItem>) {
        class VideoItem(
            val contentDetails: ContentDetails,
            val localizations: Map<String, Localisation> = emptyMap(),
            val snippet: Snippet
        ) {
            class ContentDetails(val duration: String)
            class Localisation(val title: String, val description: String)
            class Snippet(val title: String, val description: String, val publishedAt: String, val channelId: String)
        }

        fun getVideo(): VideoItem {
            return items[0]
        }
    }

    private class ChannelInfoResponse(val items: List<ChannelItem>) {
        class ChannelItem(
            val snippet: Snippet,
            val statistics: Statistics,
            val localizations: Map<String, Localisation> = emptyMap()
        ) {
            class Snippet(val title: String, val description: String = "", val publishedAt: String, val defaultLanguage: String? = null, val country: String? = null, val thumbnails: Thumbnails)
            class Statistics(val viewCount: String, val subscriberCount: String, val hiddenSubscriberCount: Boolean, val videoCount: String)
            class Localisation(val title: String = "", val description: String = "")
        }

        fun getChannel(): ChannelItem {
            return items[0]
        }
    }

    class Thumbnails(val default: Thumbnail, val medium: Thumbnail, val high: Thumbnail)
    class Thumbnail(val url: String)

    companion object {

        private val klaxon: Klaxon = Klaxon()
        private val ptl = Ptl()
        private val api = Python.getInstance().getModule("ytmusicapi").callAttr("YTMusic").apply { callAttr("setup", null, getString(R.string.yt_music_creds)) }

        private val song_request_queue = mutableListOf<Pair<String, MutableList<(Song?) -> Unit>>>()
        private var song_request_count = 0
        private val max_song_requests = 5
        private val song_request_mutex = Mutex()

        // TODO | Song and artist cache
        fun getSong(song_id: String, callback: (Song?) -> Unit) {
            for (request in song_request_queue) {
                if (request.first == song_id) {
                    request.second.add(callback)
                    return
                }
            }

            song_request_queue.add(Pair(song_id, mutableListOf(callback)))

            updateSongRequests()
        }

        fun batchGetSongs(song_ids: List<String>, callback: (Int, Song?) -> Unit) {
            var added = false
            runBlocking {
                song_request_mutex.withLock {

                    val buffer = mutableMapOf<Int, Song?>()
                    var head = 0

                    fun getCallback(index: Int): (Song?) -> Unit {
                        return {
                            buffer.put(index, it)
                            while (buffer.containsKey(head)) {
                                callback(head, buffer[head++])
                            }
                        }
                    }

                    for (i in 0 until song_ids.size) {
                        val song_id = song_ids[i]
                        var skip = false
                        for (request in song_request_queue) {
                            if (request.first == song_id) {
                                request.second.add(getCallback(i))
                                skip = true
                                break
                            }
                        }

                        if (!skip) {
                            song_request_queue.add(Pair(song_id, mutableListOf(getCallback(i))))
                            added = true
                        }
                    }
                }
            }

            if (added) {
                updateSongRequests()
            }
        }

        private fun updateSongRequests() {
            runBlocking {
                song_request_mutex.withLock {
                    while (song_request_count < max_song_requests && song_request_queue.isNotEmpty()) {
                        song_request_count++

                        val song_request = song_request_queue.removeFirst()
                        thread {
                            var ret: Song? = null

                            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/videos")
                            request.addParam("key", getString(R.string.data_api_key))
                            request.addParam("part", "contentDetails,snippet,localizations")
                            request.addParam("id", song_request.first)

                            val video = klaxon.parse<VideoInfoResponse>(request.getResult())?.getVideo()
                            if (video != null) {
                                ret = Song(song_request.first, SongData("", video.snippet.title, video.snippet.description), getArtist(video.snippet.channelId)!!, Date.from(Instant.parse(video.snippet.publishedAt)), java.time.Duration.parse(video.contentDetails.duration))
                            }

                            for (callback in song_request.second) {
                                callback(ret)
                            }

                            MainActivity.runInMainThread {
                                song_request_count--
                                updateSongRequests()
                            }
                        }
                    }
                }
            }
        }

        private val artistCache: MutableMap<String, Artist> = mutableMapOf()

        fun getArtist(channelId: String): Artist? {

            if (artistCache.containsKey(channelId)) {
                return artistCache.getValue(channelId)
            }

            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/channels")
            request.addParam("key", getString(R.string.data_api_key))
            request.addParam("part", "contentDetails,snippet,localizations,statistics")
            request.addParam("id", channelId)

            val res = request.getResult()

            val channel = klaxon.parse<ChannelInfoResponse>(res)?.getChannel()
            if (channel == null) {
                return null
            }

            val ret = Artist(channelId, ArtistData(channel.snippet.defaultLanguage, channel.snippet.title, channel.snippet.description),
                Date.from(Instant.parse(channel.snippet.publishedAt)),
                channel.snippet.thumbnails.default.url,
                channel.snippet.thumbnails.high.url,
                channel.statistics.viewCount,
                channel.statistics.subscriberCount,
                channel.statistics.hiddenSubscriberCount,
                channel.statistics.videoCount
            )

            artistCache[channelId] = ret

            return ret
        }

        data class SearchResults(val items: List<Result>) {
            data class Result(val id: ResultId, val snippet: Snippet)
            data class ResultId(val kind: String, val videoId: String = "", val channelId: String = "", val playlistId: String = "")
            data class Snippet(val publishedAt: String, val channelId: String, val title: String, val description: String, val thumbnails: Thumbnails)
        }

        fun search(query: String, type: ResourceType, max_results: Int = 10, channel_id: String? = null): List<SearchResults.Result> {

            // // For testing purposes, return static result to reduce API usage
            // val data = when(type) {
            //     ResourceType.SONG -> """{"kind":"youtube#searchListResponse","etag":"nRPnD-pR1sRRYkjoxR_YG5KS8oU","nextPageToken":"CAoQAA","regionCode":"NL","pageInfo":{"totalResults":217141,"resultsPerPage":10},"items":[{"kind":"youtube#searchResult","etag":"0EB7F-Xe42jn2z29Jk7WiwFX9Jw","id":{"kind":"youtube#video","videoId":"cp8UEv8i0lc"},"snippet":{"publishedAt":"2022-05-25T11:00:12Z","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw","title":"ツユ - いつかオトナになれるといいね。 MV","description":"Apple Music・Spotify・LINE MUSIC等のサブスク配信中です。 https://tuyu.lnk.to/itsuoto OffVocal音源 https://piapro.jp/t/UPLn …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/cp8UEv8i0lc/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/cp8UEv8i0lc/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/cp8UEv8i0lc/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2022-05-25T11:00:12Z"}},{"kind":"youtube#searchResult","etag":"0oUsn0L4miLOvgG13FJM8nPVxuw","id":{"kind":"youtube#video","videoId":"TBoBfT-_sfM"},"snippet":{"publishedAt":"2022-07-27T11:00:11Z","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw","title":"ツユ - アンダーキッズ MV","description":"Apple Music・Spotify・LINE MUSIC等のサブスク配信中です。 https://tuyu.lnk.to/underkids OffVocal音源 https://tuyu-official.jp/dl …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2022-07-27T11:00:11Z"}},{"kind":"youtube#searchResult","etag":"5rauEhD5J9aLreBZN5OBa4seOC0","id":{"kind":"youtube#video","videoId":"Wx08V5jPEwg"},"snippet":{"publishedAt":"2021-04-26T11:00:13Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - デモーニッシュ MV","description":"iTunes・Apple Music・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/PFzEFNUx □公式ツイッター …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/Wx08V5jPEwg/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/Wx08V5jPEwg/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/Wx08V5jPEwg/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2021-04-26T11:00:13Z"}},{"kind":"youtube#searchResult","etag":"9QA65dSwRDd8ZgaGOp3yehMNk44","id":{"kind":"youtube#video","videoId":"M7FH1dL51oU"},"snippet":{"publishedAt":"2020-08-22T11:11:05Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - 泥の分際で私だけの大切を奪おうだなんて MV","description":"iTunes・Apple Music・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/RE1ex62C □公式ツイッター …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/M7FH1dL51oU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/M7FH1dL51oU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/M7FH1dL51oU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2020-08-22T11:11:05Z"}},{"kind":"youtube#searchResult","etag":"dp17Y-0qA5fsOe83Fh5Crinh0iE","id":{"kind":"youtube#video","videoId":"olWvy0PiLfA"},"snippet":{"publishedAt":"2019-10-16T11:00:09Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - くらべられっ子 MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 ﻿https://linkco.re/7g3S6Y0A OffVocal音源 https://piapro.jp/t/ZiFe …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/olWvy0PiLfA/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/olWvy0PiLfA/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/olWvy0PiLfA/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-10-16T11:00:09Z"}},{"kind":"youtube#searchResult","etag":"lFYLkvCE9W7MNLdEPjCrQlGKSqI","id":{"kind":"youtube#video","videoId":"4QXCPuwBz2E"},"snippet":{"publishedAt":"2019-11-13T11:00:01Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - あの世行きのバスに乗ってさらば。 MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/7g3S6Y0A 《ツユ1stフルアルバム好評発売中》 …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/4QXCPuwBz2E/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/4QXCPuwBz2E/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/4QXCPuwBz2E/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-11-13T11:00:01Z"}},{"kind":"youtube#searchResult","etag":"-vbuzqRg3ZRHm6rzLiWurAXSo54","id":{"kind":"youtube#video","videoId":"xU7KuHLnExA"},"snippet":{"publishedAt":"2022-08-28T12:00:34Z","channelId":"UCRMpIxnySp7Fy5SbZ8dBv2w","title":"いつかオトナになれるといいね。 - ツユ (Cover) / KMNZ LIZ","description":"ずちゃ@  推し ------------------------ Original：https://www.youtube.com/watch?v=cp8UEv8i0lc Main Art：けい Vocal &amp; MIX &amp; Movie …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/xU7KuHLnExA/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/xU7KuHLnExA/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/xU7KuHLnExA/hqdefault.jpg","width":480,"height":360}},"channelTitle":"KMNZ LIZ","liveBroadcastContent":"none","publishTime":"2022-08-28T12:00:34Z"}},{"kind":"youtube#searchResult","etag":"WN_INUUdndsVvQz_Z41ttBnszCg","id":{"kind":"youtube#video","videoId":"D0ehC_8sQuU"},"snippet":{"publishedAt":"2019-06-12T11:00:03Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - やっぱり雨は降るんだね MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/7g3S6Y0A 《ツユ1stフルアルバム好評発売中》 ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-06-12T11:00:03Z"}},{"kind":"youtube#searchResult","etag":"CqSdlT7P-B4ScW4FgZpnxcl7yDM","id":{"kind":"youtube#video","videoId":"1cGQotpn8r4"},"snippet":{"publishedAt":"2019-12-19T11:00:00Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - ロックな君とはお別れだ MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/7g3S6Y0A 《ツユ1stフルアルバム好評発売中》 ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/1cGQotpn8r4/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/1cGQotpn8r4/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/1cGQotpn8r4/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-12-19T11:00:00Z"}},{"kind":"youtube#searchResult","etag":"Onh9P3ZCBhOWx-Dy12e9xG098O4","id":{"kind":"youtube#video","videoId":"vcw5THyM7Jo"},"snippet":{"publishedAt":"2021-10-09T11:30:13Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - 終点の先が在るとするならば。 MV","description":"iTunes・Apple Music・Spotify・LINE MUSIC等のサブスク配信中です。 https://linkco.re/ArP2Q03M □公式ツイッター ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/vcw5THyM7Jo/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/vcw5THyM7Jo/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/vcw5THyM7Jo/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2021-10-09T11:30:13Z"}}]}"""
            //     ResourceType.ARTIST -> """{"kind":"youtube#searchListResponse","etag":"UQIKOrbUNgtyfXkrxgHjTgir7GU","nextPageToken":"CAoQAA","regionCode":"NL","pageInfo":{"totalResults":860,"resultsPerPage":10},"items":[{"kind":"youtube#searchResult","etag":"-h_I14JnHfU0H4jjClwEoOJrJiI","id":{"kind":"youtube#channel","channelId":"UCB2tP2QfRG7hTra0KTOtTBg"},"snippet":{"publishedAt":"2009-04-08T07:04:56Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ","description":"ツユ Composer：ぷす 1994年5月23日生 (28歳) 歌／作詞作曲／編曲／ギター／Mix&amp;Mastering／プロデュース等 ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/Kyb1VwVChx8DXOgqOhrq9Lvca5e_SZskAs6f7l5wVoL_ZOLFoJv51Ar0Hye0VxPpntP_rUTFiw=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/Kyb1VwVChx8DXOgqOhrq9Lvca5e_SZskAs6f7l5wVoL_ZOLFoJv51Ar0Hye0VxPpntP_rUTFiw=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/Kyb1VwVChx8DXOgqOhrq9Lvca5e_SZskAs6f7l5wVoL_ZOLFoJv51Ar0Hye0VxPpntP_rUTFiw=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2009-04-08T07:04:56Z"}},{"kind":"youtube#searchResult","etag":"aed4rL0QLOYKZeCtPDkaK77JPbk","id":{"kind":"youtube#channel","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw"},"snippet":{"publishedAt":"2022-03-23T06:37:21Z","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/gHRX90T8rDM-IEW_W-OT9Pw3IEB-_wMJY_vDUMfIpuy_K64u8C7UvPsGPenRDCmaMIcK7sRZrA=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/gHRX90T8rDM-IEW_W-OT9Pw3IEB-_wMJY_vDUMfIpuy_K64u8C7UvPsGPenRDCmaMIcK7sRZrA=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/gHRX90T8rDM-IEW_W-OT9Pw3IEB-_wMJY_vDUMfIpuy_K64u8C7UvPsGPenRDCmaMIcK7sRZrA=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2022-03-23T06:37:21Z"}},{"kind":"youtube#searchResult","etag":"-qVOSPnePR8PhV8RNFJg4BiKKgg","id":{"kind":"youtube#channel","channelId":"UCZkClY03Vnv-X9qMnEHzxHQ"},"snippet":{"publishedAt":"2020-03-11T18:45:54Z","channelId":"UCZkClY03Vnv-X9qMnEHzxHQ","title":"TUYU - Topic","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/bpkTKhpedrjqXx3vT3LffQOSOuv8AxlG5s7nrK9630c2l9VEePVfwkIbAQOoHQquiuCDgloyYqQ=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/bpkTKhpedrjqXx3vT3LffQOSOuv8AxlG5s7nrK9630c2l9VEePVfwkIbAQOoHQquiuCDgloyYqQ=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/bpkTKhpedrjqXx3vT3LffQOSOuv8AxlG5s7nrK9630c2l9VEePVfwkIbAQOoHQquiuCDgloyYqQ=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"TUYU - Topic","liveBroadcastContent":"none","publishTime":"2020-03-11T18:45:54Z"}},{"kind":"youtube#searchResult","etag":"C9yw6LdfVJ2h5IBw334MuzsRwnM","id":{"kind":"youtube#channel","channelId":"UCos_5GfVM3AYMWCMw_mdfbg"},"snippet":{"publishedAt":"2019-05-10T20:13:02Z","channelId":"UCos_5GfVM3AYMWCMw_mdfbg","title":"つゆ","description":"ご依頼は下記リンクTwitterのDMまでお願いします 基本インスタのストーリーで息してます.","thumbnails":{"default":{"url":"https://yt3.ggpht.com/ytc/AMLnZu_EAidQFP9jhpjJcb1ZgOg-l70GzWiz7cTvR3D6=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/ytc/AMLnZu_EAidQFP9jhpjJcb1ZgOg-l70GzWiz7cTvR3D6=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/ytc/AMLnZu_EAidQFP9jhpjJcb1ZgOg-l70GzWiz7cTvR3D6=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"つゆ","liveBroadcastContent":"upcoming","publishTime":"2019-05-10T20:13:02Z"}},{"kind":"youtube#searchResult","etag":"zfd9skflf5v4jeJy_ikdzwnh0iM","id":{"kind":"youtube#channel","channelId":"UC1x8Dc0Pwq3rdShBm0GWjig"},"snippet":{"publishedAt":"2021-08-11T16:03:23Z","channelId":"UC1x8Dc0Pwq3rdShBm0GWjig","title":"ぷす(fromTUYU)","description":"ツユ Composer：ぷす 歌／作詞作曲／編曲／ギター／Mix&amp;Mastering／プロデュース等 (事務所・レーベル無所属) お仕事の依頼 ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/8C22bhMj6QbmNe5VFywWNPZGYPKNVn9SInUmxg_w5OsLTFsVvXELYWIIqwPjGbXNaKBe9YtNbw=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/8C22bhMj6QbmNe5VFywWNPZGYPKNVn9SInUmxg_w5OsLTFsVvXELYWIIqwPjGbXNaKBe9YtNbw=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/8C22bhMj6QbmNe5VFywWNPZGYPKNVn9SInUmxg_w5OsLTFsVvXELYWIIqwPjGbXNaKBe9YtNbw=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ぷす(fromTUYU)","liveBroadcastContent":"none","publishTime":"2021-08-11T16:03:23Z"}},{"kind":"youtube#searchResult","etag":"_n-81QNXu3s0Jux1YSEQqGjroXQ","id":{"kind":"youtube#channel","channelId":"UCzWCYM0vr67bjzZ9ubvk6-A"},"snippet":{"publishedAt":"2020-07-29T21:32:26Z","channelId":"UCzWCYM0vr67bjzZ9ubvk6-A","title":"ツユ","description":"σɦαყσ!^^ ɓเεɳѵεɳเ∂σ α εรƭε ɦµɱเℓ∂ε-? cαɳαℓ      ‍","thumbnails":{"default":{"url":"https://yt3.ggpht.com/0W5YlSVH-ZSX0kpNwebcwOeM-QIKKhhwcrakl6xxR1mgBUF0dDc2hdqcWSFAk_Mbc9KpmyCo9XU=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/0W5YlSVH-ZSX0kpNwebcwOeM-QIKKhhwcrakl6xxR1mgBUF0dDc2hdqcWSFAk_Mbc9KpmyCo9XU=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/0W5YlSVH-ZSX0kpNwebcwOeM-QIKKhhwcrakl6xxR1mgBUF0dDc2hdqcWSFAk_Mbc9KpmyCo9XU=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2020-07-29T21:32:26Z"}},{"kind":"youtube#searchResult","etag":"KMMa0KyxhvubHv4zOxkRBwD-4IA","id":{"kind":"youtube#channel","channelId":"UCxNwOL0yz5oOur7KfejNE5w"},"snippet":{"publishedAt":"2016-10-16T12:38:23Z","channelId":"UCxNwOL0yz5oOur7KfejNE5w","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/ytc/AMLnZu9776KSJsDalHiTw3QrKRuNerZoV-sDF8KKVJM6=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/ytc/AMLnZu9776KSJsDalHiTw3QrKRuNerZoV-sDF8KKVJM6=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/ytc/AMLnZu9776KSJsDalHiTw3QrKRuNerZoV-sDF8KKVJM6=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2016-10-16T12:38:23Z"}},{"kind":"youtube#searchResult","etag":"lcLDjjZZ1pus9IVQR_ZfjtoTN94","id":{"kind":"youtube#channel","channelId":"UCqWIuSoQAUKmkOpW4GIhYBA"},"snippet":{"publishedAt":"2021-08-29T07:54:46Z","channelId":"UCqWIuSoQAUKmkOpW4GIhYBA","title":"☔ドＳつゆ-ASMR-☔","description":"女性向けASMRを配信してます！ 雨の日の推しにならない？ コラボお仕事依頼は↓からお願いします！ Twitter: ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/E1yOQlWWax4hlBtck6ePlEXidGYmwEmf5APk9RhZzoKi3m5bh1A30pBiFfklGXytwhdxq5uSEaY=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/E1yOQlWWax4hlBtck6ePlEXidGYmwEmf5APk9RhZzoKi3m5bh1A30pBiFfklGXytwhdxq5uSEaY=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/E1yOQlWWax4hlBtck6ePlEXidGYmwEmf5APk9RhZzoKi3m5bh1A30pBiFfklGXytwhdxq5uSEaY=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"☔ドＳつゆ-ASMR-☔","liveBroadcastContent":"upcoming","publishTime":"2021-08-29T07:54:46Z"}},{"kind":"youtube#searchResult","etag":"vAvRW_pC3aYVCWOYijicSwSgehM","id":{"kind":"youtube#channel","channelId":"UC5BjneXmkFt1WXDRUkQGocw"},"snippet":{"publishedAt":"2017-04-29T09:52:45Z","channelId":"UC5BjneXmkFt1WXDRUkQGocw","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/u1O9CjCcflKOqeFr4g5GTp02qXl7_m7AVwFRQ0GV46sz-Q8SOEFaTZF0e4NA8YAOSrGo4zRP6yg=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/u1O9CjCcflKOqeFr4g5GTp02qXl7_m7AVwFRQ0GV46sz-Q8SOEFaTZF0e4NA8YAOSrGo4zRP6yg=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/u1O9CjCcflKOqeFr4g5GTp02qXl7_m7AVwFRQ0GV46sz-Q8SOEFaTZF0e4NA8YAOSrGo4zRP6yg=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2017-04-29T09:52:45Z"}},{"kind":"youtube#searchResult","etag":"FjDrnOA-RncYBK6p8DlyKvgzpiY","id":{"kind":"youtube#channel","channelId":"UCEnd7OmJItQbJzYuTsxgWmQ"},"snippet":{"publishedAt":"2021-06-09T03:47:50Z","channelId":"UCEnd7OmJItQbJzYuTsxgWmQ","title":"☔️つゆ-Tsuyu-☔️ASMR","description":"女性向けASMRを配信してます！ 貴女に癒しを届けられたら嬉しいな・・・ 貴女の心にひっそりと暮らしたいそんな思いで配信し ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/DGtVgQLIggogw6M475ErUn_LsGPjkz-tgDbjktaKtWiTQV4Y5_tMJgfFibCJEqWmnEtslwG3lrc=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/DGtVgQLIggogw6M475ErUn_LsGPjkz-tgDbjktaKtWiTQV4Y5_tMJgfFibCJEqWmnEtslwG3lrc=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/DGtVgQLIggogw6M475ErUn_LsGPjkz-tgDbjktaKtWiTQV4Y5_tMJgfFibCJEqWmnEtslwG3lrc=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"☔️つゆ-Tsuyu-☔️ASMR","liveBroadcastContent":"none","publishTime":"2021-06-09T03:47:50Z"}}]}"""
            //     ResourceType.PLAYLIST -> """{"kind":"youtube#searchListResponse","etag":"BM2y_hloSS5ytbL6TFDEXw2Ckz0","nextPageToken":"CAoQAA","regionCode":"NL","pageInfo":{"totalResults":15989,"resultsPerPage":10},"items":[{"kind":"youtube#searchResult","etag":"37w6wWybAAHNnIcoo6YVTYs0xEM","id":{"kind":"youtube#playlist","playlistId":"PLwBnYkSZTLgIGr1_6l5pesUY0TZZFIy_b"},"snippet":{"publishedAt":"2019-06-12T14:11:30Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ MV","description":"TUYU Music Video.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-06-12T14:11:30Z"}},{"kind":"youtube#searchResult","etag":"oGY-fePJLEy9YBqRAhnc-JYWcg0","id":{"kind":"youtube#playlist","playlistId":"PLYYLyJyK7RmFp0qHWXwyKyI6_-e9AilhS"},"snippet":{"publishedAt":"2020-05-06T00:47:18Z","channelId":"UCQpVZLfSlH_g9Swywys0eHA","title":"ツユメドレー","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/5xfNTyy-Xhk/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/5xfNTyy-Xhk/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/5xfNTyy-Xhk/hqdefault.jpg","width":480,"height":360}},"channelTitle":"すがさん。","liveBroadcastContent":"none","publishTime":"2020-05-06T00:47:18Z"}},{"kind":"youtube#searchResult","etag":"2tKuiNaxaMrkdETOA22GoPtLadY","id":{"kind":"youtube#playlist","playlistId":"PLAsBHK1reTZaPB6Dk47jGDJhgsQfqbEIJ"},"snippet":{"publishedAt":"2020-06-10T20:44:11Z","channelId":"UCLoecsegV_i2c3183UlbEHQ","title":"ツユ 全曲","description":"ツユのオフィシャルアカウントから投稿されたもののみです。","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"瀬端霞","liveBroadcastContent":"none","publishTime":"2020-06-10T20:44:11Z"}},{"kind":"youtube#searchResult","etag":"WPOn5CpDGUKZJs5iWgVfeb__4p8","id":{"kind":"youtube#playlist","playlistId":"PLDNjvlOWpHgpAVtH14iv1yll-4HwA6PA1"},"snippet":{"publishedAt":"2022-08-27T09:00:26Z","channelId":"UCnX4hfw41Zqc0_mE8AEFHiw","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/UArb6-27kwM/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/UArb6-27kwM/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/UArb6-27kwM/hqdefault.jpg","width":480,"height":360}},"channelTitle":"𝐆𝐧𝐞𝐢𝐒𝐬","liveBroadcastContent":"none","publishTime":"2022-08-27T09:00:26Z"}},{"kind":"youtube#searchResult","etag":"xjYPLYVZtUK9KJ0N6Y99nHc3S58","id":{"kind":"youtube#playlist","playlistId":"PLNXxEUczfoD-XwsElupA6McvfRu-gEOBa"},"snippet":{"publishedAt":"2021-07-16T09:15:57Z","channelId":"UC-mxWa9e8CewHAvsUGsHYxA","title":"TUYU (ツユ) Piano Arrangments","description":"oldfrenchguy's TUYU Piano Arrangments.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/oCGiKvJUvYw/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/oCGiKvJUvYw/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/oCGiKvJUvYw/hqdefault.jpg","width":480,"height":360}},"channelTitle":"oldfrenchguy","liveBroadcastContent":"none","publishTime":"2021-07-16T09:15:57Z"}},{"kind":"youtube#searchResult","etag":"yRsoDjBwUSFZgShwzw_b0Kw9Rk8","id":{"kind":"youtube#playlist","playlistId":"PLFAYlFKp2kqadBcTRzRkvz8wkVLoRnaBs"},"snippet":{"publishedAt":"2020-08-30T14:36:21Z","channelId":"UCDukeCFjXSb9FQnkpJjIzJQ","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/RJJQ2emN478/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/RJJQ2emN478/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/RJJQ2emN478/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ねむみある","liveBroadcastContent":"none","publishTime":"2020-08-30T14:36:21Z"}},{"kind":"youtube#searchResult","etag":"vl4dh3EPIzuHoFwVXLlG2SrxsKY","id":{"kind":"youtube#playlist","playlistId":"PLi3eTn_pS3k13BkxSZMAFohKmhtq4vLXj"},"snippet":{"publishedAt":"2020-10-22T05:09:35Z","channelId":"UCyz3im994KnGDo6ID9T901g","title":"[Haoto] ツユ - Piano Arrangements","description":"Playlist is sorted by UPLOAD order, not RELEASE order. On October 22nd, 2020, I finally made this playlist lol. I kept forgetting to.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/2a1Ra3nkBJI/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/2a1Ra3nkBJI/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/2a1Ra3nkBJI/hqdefault.jpg","width":480,"height":360}},"channelTitle":"Haoto 葉音 - Anime on Piano","liveBroadcastContent":"none","publishTime":"2020-10-22T05:09:35Z"}},{"kind":"youtube#searchResult","etag":"F6k3uFx7FHF-nwd4aWzMb-sdW_Q","id":{"kind":"youtube#playlist","playlistId":"PLKqUGdYEtm-XY92Vkyqo2Wn6ykVsfyHRu"},"snippet":{"publishedAt":"2020-12-31T04:13:10Z","channelId":"UCz_wt22sn_ZE-xhTTIH1WKg","title":"ツユ　トピック","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/hqdefault.jpg","width":480,"height":360}},"channelTitle":"夜世","liveBroadcastContent":"none","publishTime":"2020-12-31T04:13:10Z"}},{"kind":"youtube#searchResult","etag":"YeuWwBjC7ZZP6tTVtQ7_Ixa14Ag","id":{"kind":"youtube#playlist","playlistId":"PLzOOAYPdJwZVrbYWY9HJuF3b3zIJv9SF0"},"snippet":{"publishedAt":"2022-02-03T10:33:40Z","channelId":"UCJJs_vvba7wE9yXLfQbj9AA","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/hqdefault.jpg","width":480,"height":360}},"channelTitle":"レックスたいちょう","liveBroadcastContent":"none","publishTime":"2022-02-03T10:33:40Z"}},{"kind":"youtube#searchResult","etag":"MGTunrtlBtJwSSOV4gyIweq7wLg","id":{"kind":"youtube#playlist","playlistId":"PLcgu28mP0Hc5Oa-32hD0fCwxE0yBWg02T"},"snippet":{"publishedAt":"2021-05-24T15:36:16Z","channelId":"UCmA90KgDT2HfzKW3kvrJEuw","title":"ツユ☔","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"槙-sin-","liveBroadcastContent":"none","publishTime":"2021-05-24T15:36:16Z"}}]}"""
            // }

            val request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/search")
            request.addParam("key", getString(R.string.data_api_key))
            request.addParam("part", "snippet")
            request.addParam("type", when (type) {
                ResourceType.SONG -> "video"
                ResourceType.ARTIST -> "channel"
                ResourceType.PLAYLIST -> "playlist"
            })
            request.addParam("q", query)
            request.addParam("maxResults", max_results.toString())
            request.addParam("safeSearch", "none")

            if (channel_id != null) {
                request.addParam("channelId", channel_id)
            }

            return klaxon.parse<SearchResults>(request.getResult())!!.items
            // return klaxon.parse<SearchResults>(data)!!.items
        }

        class GetDownloadUrlResult(val playabilityStatus: PlayabilityStatus, val streamingData: StreamingData? = null) {
            class StreamingData(val adaptiveFormats: List<Format>)
            class Format(val itag: Int?, val url: String? = null)
            class PlayabilityStatus(val status: String, val reason: String? = null)

            fun isPlayable(): Boolean {
                return playabilityStatus.status == "OK"
            }

            fun getError(): String {
                return playabilityStatus.reason.toString()
            }
        }

        val ytd = Python.getInstance().getModule("yt_dlp").callAttr("YoutubeDL")

        fun getDownloadUrl(id: String, callback: (url: String?) -> Unit) {
            thread {

                val DEFAULT_CLIENT = """{"clientName":"ANDROID","clientVersion":"16.50","visitorData":null,"hl":"en"}"""
                val NO_CONTENT_WARNING_CLIENT = """{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","visitorData":null,"hl":"en"}"""

                val http_client = OkHttpClient()

                // TODO | Use own API key
                fun attemptGetDownloadUrl(client: String): String? {
                    val body = """{"context":{"client":$client},"videoId":"$id","playlistId":null}""".toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8&prettyPrint=false")
                        .post(body)
                        .build()
                    val response = http_client.newCall(request).execute()
                    if (response.code != 200) {
                        throw RuntimeException(response.body!!.string())
                    }

                    val parsed = klaxon.parse<GetDownloadUrlResult>(response.peekBody(1000000).string())!!

                    if (!parsed.isPlayable()) {
                        return null
                    }

                    for (format in parsed.streamingData!!.adaptiveFormats) {
                        if (format.itag == 140) {
                            return format.url
                        }
                    }

                    return null
                }

                var url = attemptGetDownloadUrl(DEFAULT_CLIENT)
                if (url == null) {
                    url = attemptGetDownloadUrl(NO_CONTENT_WARNING_CLIENT)
                    if (url == null) {
                        // Use yt-dlp as a catch-all backup (it's too slow to use all the time)
                        val formats = ytd.callAttr("extract_info", id, false).callAttr("get", "formats").asList()
                        for (format in formats) {
                            if (format.callAttr("get", "format_id").toString() == "140") {
                                url = format.callAttr("get", "url").toString()
                                break
                            }
                        }
                    }
                }

                MainActivity.runInMainThread {
                    callback(url)
                }
            }
        }

        fun getSongPTLyricsId(song: Song): Int? {
            return ptl.findLyricsId(song.getTitle(), song.artist.nativeData.name)
        }

        fun getSongYTLyricsId(song: Song): String? {
            fun getLyricsId(video_id: String?): String? {
                val lyrics = api.callAttr("get_watch_playlist", video_id, null, 1).callAttr("get", "lyrics")
                if (lyrics != null) {
                    return lyrics.toString()
                }
                return null
            }

            val ret = getLyricsId(song.getId())
            if (ret != null) {
                return ret
            }

            return getLyricsId(song.getCounterpartId())
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            thread {
                val pt_id = song.getPTLyricsId()
                if (pt_id != null) {
                    val lyrics = ptl.getLyrics(pt_id)
                    if (lyrics != null) {
                        callback(Song.PTLyrics(lyrics))
                        return@thread
                    }
                }

                try {
                    val lyrics = api.callAttr("get_lyrics", song.getYTLyricsId())
                    callback(Song.YTLyrics(lyrics.callAttr("get", "lyrics").toString(), lyrics.callAttr("get", "source").toString()))
                }
                catch (e: PyException) {
                    callback(null)
                }
            }
        }

        fun getSongCounterpartId(song: Song): String? {
            val counterpart = api.callAttr("get_watch_playlist", song.getId(), null, 1).callAttr("get", "tracks").asList().first().callAttr("get", "conuterpart")
            if (counterpart == null) {
                return null
            }
            return counterpart.callAttr("get", "videoId").toString()
        }

        fun getArtistCounterpartId(artist: Artist): String? {
            val results = api.callAttr("search", artist.nativeData.name, "artists", null, 1, true).asList()
            return results.first().callAttr("get", "browseId").toString()
        }

        fun getSongRadio(song_id: String, include_first: Boolean = true, limit: Int = 25): List<String> {
            val body = """
            {
                "enablePersistentPlaylistPanel": true,
                "isAudioOnly": true,
                "tunerSettingValue": "AUTOMIX_SETTING_NORMAL",
                "videoId": "${song_id}",
                "playlistId": "RDAMVM${song_id}",
                "watchEndpointMusicSupportedConfigs": {
                    "watchEndpointMusicConfig": {
                        "hasPersistentPlaylistPanel": true,
                        "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                    }

                },
                "context" : {
                    "client": {
                        "clientName": "WEB_REMIX",
                        "clientVersion": "1.20221023.01.00",
                        "hl": "ja"
                    },
                    "user": {}
                }
            }
            """

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next?alt=json&key=AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30")
                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0")
                .addHeader("accept", "*/*")
                .addHeader("accept-encoding", "gzip, deflate")
                .addHeader("content-encoding", "gzip")
                .addHeader("origin", "https://music.youtube.com")
                .addHeader("X-Goog-Visitor-Id", "CgtUYXUtLWtyZ3ZvTSj3pNWaBg%3D%3D")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val client = OkHttpClient().newBuilder()
                .addInterceptor(object : Interceptor {
                    override fun intercept(chain: Interceptor.Chain): Response {
                        val original = chain.request();
                        val authorized = original.newBuilder()
                            .addHeader("Cookie", "CONSENT=YES+1")
                            .build();
                        return chain.proceed(authorized);
                    }
                })
                .build()

            val response = client.newCall(request).execute()

            val songs = (Parser.default().parse(GZIPInputStream(response.body!!.byteStream())) as JsonObject)
                .obj("contents")!!
                .obj("singleColumnMusicWatchNextResultsRenderer")!!
                .obj("tabbedRenderer")!!
                .obj("watchNextTabbedResultsRenderer")!!
                .array<JsonObject>("tabs")!![0]
                .obj("tabRenderer")!!
                .obj("content")!!
                .obj("musicQueueRenderer")!!
                .obj("content")!!
                .obj("playlistPanelRenderer")!!
                .array<JsonObject>("contents")!!

            val offset = if (include_first) 0 else 1
            return List(songs.size - offset) {
                songs[it + offset].obj("playlistPanelVideoRenderer")!!.string("videoId")!!
            }
        }

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_watch_playlist
        }
    }
}

