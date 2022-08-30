package com.spectre7.spmp.api

import android.annotation.SuppressLint
import android.util.Log
import android.util.SparseArray
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.beust.klaxon.*
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import com.spectre7.spmp.ui.layout.ResourceType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.*


class DataApi {
    
    class HTTPGetRequest(request_url: String) {
        private var requestURL: String = request_url
        private var params: String = ""

        fun addParam(name: String, value: String) {
            if (params.isNotEmpty()) {
                params += "&"
            }
            params += URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }

        fun getRequestURL(): String {
            var ret: String = requestURL
            if (params.isNotEmpty()) {
                ret += "?$params"
            }
            return ret
        }

        fun getResult(): String {

            val resp: StringBuffer

            with(URL(getRequestURL()).openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                println("Response Code : $responseCode")

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()
                    resp = response
                }
            }

            return resp.toString()
        }

        fun reset(request_url: String) {
            requestURL = request_url
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
//                if (MainActivity.youtube!!.isSongAvailable(video.contentDetails.videoId)) {
                    ret.add(Song(video.contentDetails.videoId, SongData(null, video.snippet.title, video.snippet.description), getArtist(video.snippet.channelId)!!, Date.from(Instant.parse(video.snippet.publishedAt))))
//                }
//                else {
//                    Log.d("", "Skip ${video.snippet.title}")
//                }
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

        // TODO | Song and artist cache
        fun getSong(videoId: String): Song? {
            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/videos")
            request.addParam("key", MainActivity.getString(R.string.data_api_key))
            request.addParam("part", "contentDetails,snippet,localizations")
            request.addParam("id", videoId)

            val video = klaxon.parse<VideoInfoResponse>(request.getResult())?.getVideo()
            if (video == null) {
                return null
            }

            return Song(videoId, SongData("", video.snippet.title, video.snippet.description), getArtist(video.snippet.channelId)!!, Date.from(Instant.parse(video.snippet.publishedAt)), java.time.Duration.parse(video.contentDetails.duration))
        }

        private val artistCache: MutableMap<String, Artist> = mutableMapOf()

        fun getArtist(channelId: String): Artist? {

            if (artistCache.containsKey(channelId)) {
                return artistCache.getValue(channelId)
            }

            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/channels")
            request.addParam("key", MainActivity.getString(R.string.data_api_key))
            request.addParam("part", "contentDetails,snippet,localizations,statistics")
            request.addParam("id", channelId)

            Log.d("", request.getRequestURL())

            val res = request.getResult()

            val channel = klaxon.parse<ChannelInfoResponse>(res)?.getChannel()
            if (channel == null) {
                return null
            }

            val ret = Artist(channelId, ArtistData(channel.snippet.defaultLanguage, channel.snippet.title, channel.snippet.description),
                Date.from(Instant.parse(channel.snippet.publishedAt)),
                channel.snippet.thumbnails.default.url,
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

            // For testing purposes, return static result to reduce API usage
            val data = when(type) {
                ResourceType.SONG -> """{"kind":"youtube#searchListResponse","etag":"nRPnD-pR1sRRYkjoxR_YG5KS8oU","nextPageToken":"CAoQAA","regionCode":"NL","pageInfo":{"totalResults":217141,"resultsPerPage":10},"items":[{"kind":"youtube#searchResult","etag":"0EB7F-Xe42jn2z29Jk7WiwFX9Jw","id":{"kind":"youtube#video","videoId":"cp8UEv8i0lc"},"snippet":{"publishedAt":"2022-05-25T11:00:12Z","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw","title":"ツユ - いつかオトナになれるといいね。 MV","description":"Apple Music・Spotify・LINE MUSIC等のサブスク配信中です。 https://tuyu.lnk.to/itsuoto OffVocal音源 https://piapro.jp/t/UPLn …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/cp8UEv8i0lc/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/cp8UEv8i0lc/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/cp8UEv8i0lc/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2022-05-25T11:00:12Z"}},{"kind":"youtube#searchResult","etag":"0oUsn0L4miLOvgG13FJM8nPVxuw","id":{"kind":"youtube#video","videoId":"TBoBfT-_sfM"},"snippet":{"publishedAt":"2022-07-27T11:00:11Z","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw","title":"ツユ - アンダーキッズ MV","description":"Apple Music・Spotify・LINE MUSIC等のサブスク配信中です。 https://tuyu.lnk.to/underkids OffVocal音源 https://tuyu-official.jp/dl …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2022-07-27T11:00:11Z"}},{"kind":"youtube#searchResult","etag":"5rauEhD5J9aLreBZN5OBa4seOC0","id":{"kind":"youtube#video","videoId":"Wx08V5jPEwg"},"snippet":{"publishedAt":"2021-04-26T11:00:13Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - デモーニッシュ MV","description":"iTunes・Apple Music・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/PFzEFNUx □公式ツイッター …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/Wx08V5jPEwg/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/Wx08V5jPEwg/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/Wx08V5jPEwg/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2021-04-26T11:00:13Z"}},{"kind":"youtube#searchResult","etag":"9QA65dSwRDd8ZgaGOp3yehMNk44","id":{"kind":"youtube#video","videoId":"M7FH1dL51oU"},"snippet":{"publishedAt":"2020-08-22T11:11:05Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - 泥の分際で私だけの大切を奪おうだなんて MV","description":"iTunes・Apple Music・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/RE1ex62C □公式ツイッター …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/M7FH1dL51oU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/M7FH1dL51oU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/M7FH1dL51oU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2020-08-22T11:11:05Z"}},{"kind":"youtube#searchResult","etag":"dp17Y-0qA5fsOe83Fh5Crinh0iE","id":{"kind":"youtube#video","videoId":"olWvy0PiLfA"},"snippet":{"publishedAt":"2019-10-16T11:00:09Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - くらべられっ子 MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 ﻿https://linkco.re/7g3S6Y0A OffVocal音源 https://piapro.jp/t/ZiFe …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/olWvy0PiLfA/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/olWvy0PiLfA/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/olWvy0PiLfA/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-10-16T11:00:09Z"}},{"kind":"youtube#searchResult","etag":"lFYLkvCE9W7MNLdEPjCrQlGKSqI","id":{"kind":"youtube#video","videoId":"4QXCPuwBz2E"},"snippet":{"publishedAt":"2019-11-13T11:00:01Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - あの世行きのバスに乗ってさらば。 MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/7g3S6Y0A 《ツユ1stフルアルバム好評発売中》 …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/4QXCPuwBz2E/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/4QXCPuwBz2E/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/4QXCPuwBz2E/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-11-13T11:00:01Z"}},{"kind":"youtube#searchResult","etag":"-vbuzqRg3ZRHm6rzLiWurAXSo54","id":{"kind":"youtube#video","videoId":"xU7KuHLnExA"},"snippet":{"publishedAt":"2022-08-28T12:00:34Z","channelId":"UCRMpIxnySp7Fy5SbZ8dBv2w","title":"いつかオトナになれるといいね。 - ツユ (Cover) / KMNZ LIZ","description":"ずちゃ@  推し ------------------------ Original：https://www.youtube.com/watch?v=cp8UEv8i0lc Main Art：けい Vocal &amp; MIX &amp; Movie …","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/xU7KuHLnExA/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/xU7KuHLnExA/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/xU7KuHLnExA/hqdefault.jpg","width":480,"height":360}},"channelTitle":"KMNZ LIZ","liveBroadcastContent":"none","publishTime":"2022-08-28T12:00:34Z"}},{"kind":"youtube#searchResult","etag":"WN_INUUdndsVvQz_Z41ttBnszCg","id":{"kind":"youtube#video","videoId":"D0ehC_8sQuU"},"snippet":{"publishedAt":"2019-06-12T11:00:03Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - やっぱり雨は降るんだね MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/7g3S6Y0A 《ツユ1stフルアルバム好評発売中》 ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-06-12T11:00:03Z"}},{"kind":"youtube#searchResult","etag":"CqSdlT7P-B4ScW4FgZpnxcl7yDM","id":{"kind":"youtube#video","videoId":"1cGQotpn8r4"},"snippet":{"publishedAt":"2019-12-19T11:00:00Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - ロックな君とはお別れだ MV","description":"iTunes・Spotify・LINE MUSIC等のサブスク解禁しました。 https://linkco.re/7g3S6Y0A 《ツユ1stフルアルバム好評発売中》 ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/1cGQotpn8r4/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/1cGQotpn8r4/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/1cGQotpn8r4/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-12-19T11:00:00Z"}},{"kind":"youtube#searchResult","etag":"Onh9P3ZCBhOWx-Dy12e9xG098O4","id":{"kind":"youtube#video","videoId":"vcw5THyM7Jo"},"snippet":{"publishedAt":"2021-10-09T11:30:13Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ - 終点の先が在るとするならば。 MV","description":"iTunes・Apple Music・Spotify・LINE MUSIC等のサブスク配信中です。 https://linkco.re/ArP2Q03M □公式ツイッター ...","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/vcw5THyM7Jo/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/vcw5THyM7Jo/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/vcw5THyM7Jo/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2021-10-09T11:30:13Z"}}]}"""
                ResourceType.ARTIST -> """{"kind":"youtube#searchListResponse","etag":"UQIKOrbUNgtyfXkrxgHjTgir7GU","nextPageToken":"CAoQAA","regionCode":"NL","pageInfo":{"totalResults":860,"resultsPerPage":10},"items":[{"kind":"youtube#searchResult","etag":"-h_I14JnHfU0H4jjClwEoOJrJiI","id":{"kind":"youtube#channel","channelId":"UCB2tP2QfRG7hTra0KTOtTBg"},"snippet":{"publishedAt":"2009-04-08T07:04:56Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ","description":"ツユ Composer：ぷす 1994年5月23日生 (28歳) 歌／作詞作曲／編曲／ギター／Mix&amp;Mastering／プロデュース等 ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/Kyb1VwVChx8DXOgqOhrq9Lvca5e_SZskAs6f7l5wVoL_ZOLFoJv51Ar0Hye0VxPpntP_rUTFiw=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/Kyb1VwVChx8DXOgqOhrq9Lvca5e_SZskAs6f7l5wVoL_ZOLFoJv51Ar0Hye0VxPpntP_rUTFiw=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/Kyb1VwVChx8DXOgqOhrq9Lvca5e_SZskAs6f7l5wVoL_ZOLFoJv51Ar0Hye0VxPpntP_rUTFiw=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2009-04-08T07:04:56Z"}},{"kind":"youtube#searchResult","etag":"aed4rL0QLOYKZeCtPDkaK77JPbk","id":{"kind":"youtube#channel","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw"},"snippet":{"publishedAt":"2022-03-23T06:37:21Z","channelId":"UCC7NAtMGCX-SJxvVLkrNCtw","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/gHRX90T8rDM-IEW_W-OT9Pw3IEB-_wMJY_vDUMfIpuy_K64u8C7UvPsGPenRDCmaMIcK7sRZrA=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/gHRX90T8rDM-IEW_W-OT9Pw3IEB-_wMJY_vDUMfIpuy_K64u8C7UvPsGPenRDCmaMIcK7sRZrA=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/gHRX90T8rDM-IEW_W-OT9Pw3IEB-_wMJY_vDUMfIpuy_K64u8C7UvPsGPenRDCmaMIcK7sRZrA=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2022-03-23T06:37:21Z"}},{"kind":"youtube#searchResult","etag":"-qVOSPnePR8PhV8RNFJg4BiKKgg","id":{"kind":"youtube#channel","channelId":"UCZkClY03Vnv-X9qMnEHzxHQ"},"snippet":{"publishedAt":"2020-03-11T18:45:54Z","channelId":"UCZkClY03Vnv-X9qMnEHzxHQ","title":"TUYU - Topic","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/bpkTKhpedrjqXx3vT3LffQOSOuv8AxlG5s7nrK9630c2l9VEePVfwkIbAQOoHQquiuCDgloyYqQ=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/bpkTKhpedrjqXx3vT3LffQOSOuv8AxlG5s7nrK9630c2l9VEePVfwkIbAQOoHQquiuCDgloyYqQ=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/bpkTKhpedrjqXx3vT3LffQOSOuv8AxlG5s7nrK9630c2l9VEePVfwkIbAQOoHQquiuCDgloyYqQ=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"TUYU - Topic","liveBroadcastContent":"none","publishTime":"2020-03-11T18:45:54Z"}},{"kind":"youtube#searchResult","etag":"C9yw6LdfVJ2h5IBw334MuzsRwnM","id":{"kind":"youtube#channel","channelId":"UCos_5GfVM3AYMWCMw_mdfbg"},"snippet":{"publishedAt":"2019-05-10T20:13:02Z","channelId":"UCos_5GfVM3AYMWCMw_mdfbg","title":"つゆ","description":"ご依頼は下記リンクTwitterのDMまでお願いします 基本インスタのストーリーで息してます.","thumbnails":{"default":{"url":"https://yt3.ggpht.com/ytc/AMLnZu_EAidQFP9jhpjJcb1ZgOg-l70GzWiz7cTvR3D6=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/ytc/AMLnZu_EAidQFP9jhpjJcb1ZgOg-l70GzWiz7cTvR3D6=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/ytc/AMLnZu_EAidQFP9jhpjJcb1ZgOg-l70GzWiz7cTvR3D6=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"つゆ","liveBroadcastContent":"upcoming","publishTime":"2019-05-10T20:13:02Z"}},{"kind":"youtube#searchResult","etag":"zfd9skflf5v4jeJy_ikdzwnh0iM","id":{"kind":"youtube#channel","channelId":"UC1x8Dc0Pwq3rdShBm0GWjig"},"snippet":{"publishedAt":"2021-08-11T16:03:23Z","channelId":"UC1x8Dc0Pwq3rdShBm0GWjig","title":"ぷす(fromTUYU)","description":"ツユ Composer：ぷす 歌／作詞作曲／編曲／ギター／Mix&amp;Mastering／プロデュース等 (事務所・レーベル無所属) お仕事の依頼 ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/8C22bhMj6QbmNe5VFywWNPZGYPKNVn9SInUmxg_w5OsLTFsVvXELYWIIqwPjGbXNaKBe9YtNbw=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/8C22bhMj6QbmNe5VFywWNPZGYPKNVn9SInUmxg_w5OsLTFsVvXELYWIIqwPjGbXNaKBe9YtNbw=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/8C22bhMj6QbmNe5VFywWNPZGYPKNVn9SInUmxg_w5OsLTFsVvXELYWIIqwPjGbXNaKBe9YtNbw=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ぷす(fromTUYU)","liveBroadcastContent":"none","publishTime":"2021-08-11T16:03:23Z"}},{"kind":"youtube#searchResult","etag":"_n-81QNXu3s0Jux1YSEQqGjroXQ","id":{"kind":"youtube#channel","channelId":"UCzWCYM0vr67bjzZ9ubvk6-A"},"snippet":{"publishedAt":"2020-07-29T21:32:26Z","channelId":"UCzWCYM0vr67bjzZ9ubvk6-A","title":"ツユ","description":"σɦαყσ!^^ ɓเεɳѵεɳเ∂σ α εรƭε ɦµɱเℓ∂ε-? cαɳαℓ      ‍","thumbnails":{"default":{"url":"https://yt3.ggpht.com/0W5YlSVH-ZSX0kpNwebcwOeM-QIKKhhwcrakl6xxR1mgBUF0dDc2hdqcWSFAk_Mbc9KpmyCo9XU=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/0W5YlSVH-ZSX0kpNwebcwOeM-QIKKhhwcrakl6xxR1mgBUF0dDc2hdqcWSFAk_Mbc9KpmyCo9XU=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/0W5YlSVH-ZSX0kpNwebcwOeM-QIKKhhwcrakl6xxR1mgBUF0dDc2hdqcWSFAk_Mbc9KpmyCo9XU=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2020-07-29T21:32:26Z"}},{"kind":"youtube#searchResult","etag":"KMMa0KyxhvubHv4zOxkRBwD-4IA","id":{"kind":"youtube#channel","channelId":"UCxNwOL0yz5oOur7KfejNE5w"},"snippet":{"publishedAt":"2016-10-16T12:38:23Z","channelId":"UCxNwOL0yz5oOur7KfejNE5w","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/ytc/AMLnZu9776KSJsDalHiTw3QrKRuNerZoV-sDF8KKVJM6=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/ytc/AMLnZu9776KSJsDalHiTw3QrKRuNerZoV-sDF8KKVJM6=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/ytc/AMLnZu9776KSJsDalHiTw3QrKRuNerZoV-sDF8KKVJM6=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2016-10-16T12:38:23Z"}},{"kind":"youtube#searchResult","etag":"lcLDjjZZ1pus9IVQR_ZfjtoTN94","id":{"kind":"youtube#channel","channelId":"UCqWIuSoQAUKmkOpW4GIhYBA"},"snippet":{"publishedAt":"2021-08-29T07:54:46Z","channelId":"UCqWIuSoQAUKmkOpW4GIhYBA","title":"☔ドＳつゆ-ASMR-☔","description":"女性向けASMRを配信してます！ 雨の日の推しにならない？ コラボお仕事依頼は↓からお願いします！ Twitter: ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/E1yOQlWWax4hlBtck6ePlEXidGYmwEmf5APk9RhZzoKi3m5bh1A30pBiFfklGXytwhdxq5uSEaY=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/E1yOQlWWax4hlBtck6ePlEXidGYmwEmf5APk9RhZzoKi3m5bh1A30pBiFfklGXytwhdxq5uSEaY=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/E1yOQlWWax4hlBtck6ePlEXidGYmwEmf5APk9RhZzoKi3m5bh1A30pBiFfklGXytwhdxq5uSEaY=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"☔ドＳつゆ-ASMR-☔","liveBroadcastContent":"upcoming","publishTime":"2021-08-29T07:54:46Z"}},{"kind":"youtube#searchResult","etag":"vAvRW_pC3aYVCWOYijicSwSgehM","id":{"kind":"youtube#channel","channelId":"UC5BjneXmkFt1WXDRUkQGocw"},"snippet":{"publishedAt":"2017-04-29T09:52:45Z","channelId":"UC5BjneXmkFt1WXDRUkQGocw","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://yt3.ggpht.com/u1O9CjCcflKOqeFr4g5GTp02qXl7_m7AVwFRQ0GV46sz-Q8SOEFaTZF0e4NA8YAOSrGo4zRP6yg=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/u1O9CjCcflKOqeFr4g5GTp02qXl7_m7AVwFRQ0GV46sz-Q8SOEFaTZF0e4NA8YAOSrGo4zRP6yg=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/u1O9CjCcflKOqeFr4g5GTp02qXl7_m7AVwFRQ0GV46sz-Q8SOEFaTZF0e4NA8YAOSrGo4zRP6yg=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2017-04-29T09:52:45Z"}},{"kind":"youtube#searchResult","etag":"FjDrnOA-RncYBK6p8DlyKvgzpiY","id":{"kind":"youtube#channel","channelId":"UCEnd7OmJItQbJzYuTsxgWmQ"},"snippet":{"publishedAt":"2021-06-09T03:47:50Z","channelId":"UCEnd7OmJItQbJzYuTsxgWmQ","title":"☔️つゆ-Tsuyu-☔️ASMR","description":"女性向けASMRを配信してます！ 貴女に癒しを届けられたら嬉しいな・・・ 貴女の心にひっそりと暮らしたいそんな思いで配信し ...","thumbnails":{"default":{"url":"https://yt3.ggpht.com/DGtVgQLIggogw6M475ErUn_LsGPjkz-tgDbjktaKtWiTQV4Y5_tMJgfFibCJEqWmnEtslwG3lrc=s88-c-k-c0xffffffff-no-rj-mo"},"medium":{"url":"https://yt3.ggpht.com/DGtVgQLIggogw6M475ErUn_LsGPjkz-tgDbjktaKtWiTQV4Y5_tMJgfFibCJEqWmnEtslwG3lrc=s240-c-k-c0xffffffff-no-rj-mo"},"high":{"url":"https://yt3.ggpht.com/DGtVgQLIggogw6M475ErUn_LsGPjkz-tgDbjktaKtWiTQV4Y5_tMJgfFibCJEqWmnEtslwG3lrc=s800-c-k-c0xffffffff-no-rj-mo"}},"channelTitle":"☔️つゆ-Tsuyu-☔️ASMR","liveBroadcastContent":"none","publishTime":"2021-06-09T03:47:50Z"}}]}"""
                ResourceType.PLAYLIST -> """{"kind":"youtube#searchListResponse","etag":"BM2y_hloSS5ytbL6TFDEXw2Ckz0","nextPageToken":"CAoQAA","regionCode":"NL","pageInfo":{"totalResults":15989,"resultsPerPage":10},"items":[{"kind":"youtube#searchResult","etag":"37w6wWybAAHNnIcoo6YVTYs0xEM","id":{"kind":"youtube#playlist","playlistId":"PLwBnYkSZTLgIGr1_6l5pesUY0TZZFIy_b"},"snippet":{"publishedAt":"2019-06-12T14:11:30Z","channelId":"UCB2tP2QfRG7hTra0KTOtTBg","title":"ツユ MV","description":"TUYU Music Video.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/TBoBfT-_sfM/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ツユ","liveBroadcastContent":"none","publishTime":"2019-06-12T14:11:30Z"}},{"kind":"youtube#searchResult","etag":"oGY-fePJLEy9YBqRAhnc-JYWcg0","id":{"kind":"youtube#playlist","playlistId":"PLYYLyJyK7RmFp0qHWXwyKyI6_-e9AilhS"},"snippet":{"publishedAt":"2020-05-06T00:47:18Z","channelId":"UCQpVZLfSlH_g9Swywys0eHA","title":"ツユメドレー","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/5xfNTyy-Xhk/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/5xfNTyy-Xhk/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/5xfNTyy-Xhk/hqdefault.jpg","width":480,"height":360}},"channelTitle":"すがさん。","liveBroadcastContent":"none","publishTime":"2020-05-06T00:47:18Z"}},{"kind":"youtube#searchResult","etag":"2tKuiNaxaMrkdETOA22GoPtLadY","id":{"kind":"youtube#playlist","playlistId":"PLAsBHK1reTZaPB6Dk47jGDJhgsQfqbEIJ"},"snippet":{"publishedAt":"2020-06-10T20:44:11Z","channelId":"UCLoecsegV_i2c3183UlbEHQ","title":"ツユ 全曲","description":"ツユのオフィシャルアカウントから投稿されたもののみです。","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"瀬端霞","liveBroadcastContent":"none","publishTime":"2020-06-10T20:44:11Z"}},{"kind":"youtube#searchResult","etag":"WPOn5CpDGUKZJs5iWgVfeb__4p8","id":{"kind":"youtube#playlist","playlistId":"PLDNjvlOWpHgpAVtH14iv1yll-4HwA6PA1"},"snippet":{"publishedAt":"2022-08-27T09:00:26Z","channelId":"UCnX4hfw41Zqc0_mE8AEFHiw","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/UArb6-27kwM/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/UArb6-27kwM/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/UArb6-27kwM/hqdefault.jpg","width":480,"height":360}},"channelTitle":"𝐆𝐧𝐞𝐢𝐒𝐬","liveBroadcastContent":"none","publishTime":"2022-08-27T09:00:26Z"}},{"kind":"youtube#searchResult","etag":"xjYPLYVZtUK9KJ0N6Y99nHc3S58","id":{"kind":"youtube#playlist","playlistId":"PLNXxEUczfoD-XwsElupA6McvfRu-gEOBa"},"snippet":{"publishedAt":"2021-07-16T09:15:57Z","channelId":"UC-mxWa9e8CewHAvsUGsHYxA","title":"TUYU (ツユ) Piano Arrangments","description":"oldfrenchguy's TUYU Piano Arrangments.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/oCGiKvJUvYw/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/oCGiKvJUvYw/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/oCGiKvJUvYw/hqdefault.jpg","width":480,"height":360}},"channelTitle":"oldfrenchguy","liveBroadcastContent":"none","publishTime":"2021-07-16T09:15:57Z"}},{"kind":"youtube#searchResult","etag":"yRsoDjBwUSFZgShwzw_b0Kw9Rk8","id":{"kind":"youtube#playlist","playlistId":"PLFAYlFKp2kqadBcTRzRkvz8wkVLoRnaBs"},"snippet":{"publishedAt":"2020-08-30T14:36:21Z","channelId":"UCDukeCFjXSb9FQnkpJjIzJQ","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/RJJQ2emN478/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/RJJQ2emN478/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/RJJQ2emN478/hqdefault.jpg","width":480,"height":360}},"channelTitle":"ねむみある","liveBroadcastContent":"none","publishTime":"2020-08-30T14:36:21Z"}},{"kind":"youtube#searchResult","etag":"vl4dh3EPIzuHoFwVXLlG2SrxsKY","id":{"kind":"youtube#playlist","playlistId":"PLi3eTn_pS3k13BkxSZMAFohKmhtq4vLXj"},"snippet":{"publishedAt":"2020-10-22T05:09:35Z","channelId":"UCyz3im994KnGDo6ID9T901g","title":"[Haoto] ツユ - Piano Arrangements","description":"Playlist is sorted by UPLOAD order, not RELEASE order. On October 22nd, 2020, I finally made this playlist lol. I kept forgetting to.","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/2a1Ra3nkBJI/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/2a1Ra3nkBJI/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/2a1Ra3nkBJI/hqdefault.jpg","width":480,"height":360}},"channelTitle":"Haoto 葉音 - Anime on Piano","liveBroadcastContent":"none","publishTime":"2020-10-22T05:09:35Z"}},{"kind":"youtube#searchResult","etag":"F6k3uFx7FHF-nwd4aWzMb-sdW_Q","id":{"kind":"youtube#playlist","playlistId":"PLKqUGdYEtm-XY92Vkyqo2Wn6ykVsfyHRu"},"snippet":{"publishedAt":"2020-12-31T04:13:10Z","channelId":"UCz_wt22sn_ZE-xhTTIH1WKg","title":"ツユ　トピック","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/hqdefault.jpg","width":480,"height":360}},"channelTitle":"夜世","liveBroadcastContent":"none","publishTime":"2020-12-31T04:13:10Z"}},{"kind":"youtube#searchResult","etag":"YeuWwBjC7ZZP6tTVtQ7_Ixa14Ag","id":{"kind":"youtube#playlist","playlistId":"PLzOOAYPdJwZVrbYWY9HJuF3b3zIJv9SF0"},"snippet":{"publishedAt":"2022-02-03T10:33:40Z","channelId":"UCJJs_vvba7wE9yXLfQbj9AA","title":"ツユ","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/NXzZik68hTE/hqdefault.jpg","width":480,"height":360}},"channelTitle":"レックスたいちょう","liveBroadcastContent":"none","publishTime":"2022-02-03T10:33:40Z"}},{"kind":"youtube#searchResult","etag":"MGTunrtlBtJwSSOV4gyIweq7wLg","id":{"kind":"youtube#playlist","playlistId":"PLcgu28mP0Hc5Oa-32hD0fCwxE0yBWg02T"},"snippet":{"publishedAt":"2021-05-24T15:36:16Z","channelId":"UCmA90KgDT2HfzKW3kvrJEuw","title":"ツユ☔","description":"","thumbnails":{"default":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/default.jpg","width":120,"height":90},"medium":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/mqdefault.jpg","width":320,"height":180},"high":{"url":"https://i.ytimg.com/vi/D0ehC_8sQuU/hqdefault.jpg","width":480,"height":360}},"channelTitle":"槙-sin-","liveBroadcastContent":"none","publishTime":"2021-05-24T15:36:16Z"}}]}"""
            }

            Log.d("", data)

//            val request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/search")
//            request.addParam("key", MainActivity.getString(R.string.data_api_key))
//            request.addParam("part", "snippet")
//            request.addParam("type", when (type) {
//                ResourceType.SONG -> "video"
//                ResourceType.ARTIST -> "channel"
//                ResourceType.PLAYLIST -> "playlist"
//            })
//            request.addParam("q", query)
//            request.addParam("maxResults", max_results.toString())
//            request.addParam("safeSearch", "none")
//
//            if (channel_id != null) {
//                request.addParam("channelId", channel_id)
//            }

//            val results = klaxon.parse<SearchResults>(request.getResult())!!.items
            return klaxon.parse<SearchResults>(data)!!.items
        }

        private class Extractor(val callback: (url: String?) -> Any?): YouTubeExtractor(MainActivity.instance!!.baseContext) {
            override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, vMeta: VideoMeta?) {
                callback(ytFiles?.get(140)?.url)
            }
        }

        fun getDownloadUrl(id: String, callback: (url: String?) -> Any?) {
            Extractor(callback).extract(id)
        }

    }
}

