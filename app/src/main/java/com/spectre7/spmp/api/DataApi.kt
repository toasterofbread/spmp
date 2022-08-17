package com.spectre7.spmp.api

import android.util.Log
import com.beust.klaxon.*
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.parseIsoString

class DataApi {

    private class HTTPGetRequest(request_url: String) {
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

    private class GetChannelResponse(val items: List<ChannelItem>) {
        class ChannelItem(
            val id: String,
            val contentDetails: ContentDetails
        )
        class ContentDetails(
            val relatedPlaylists: RelatedPlaylists
        )
        class RelatedPlaylists(
            val uploads: String
        )

        fun getUploadsPlaylistId(): String {
            return items[0].contentDetails.relatedPlaylists.uploads
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
            class Snippet(val title: String, val description: String, val publishedAt: String, val defaultLanguage: String? = null, val country: String? = null)
            class Statistics(val viewCount: String, val subscriberCount: String, val hiddenSubscriberCount: Boolean, val videoCount: String)
            class Localisation(val title: String, val description: String)
        }

        fun getChannel(): ChannelItem {
            return items[0]
        }
    }

    companion object {

        fun getChannelSongs(channelId: String): List<Song> {
            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/channels")
            request.addParam("key", MainActivity.getString(R.string.data_api_key))
            request.addParam("part", "contentDetails")
            request.addParam("id", channelId)

            val playlist_id = Klaxon().parse<GetChannelResponse>(request.getResult())?.getUploadsPlaylistId()

            if (playlist_id == null) {
                return listOf()
            }

            request.reset("https://www.googleapis.com/youtube/v3/playlistItems")
            request.addParam("key", MainActivity.getString(R.string.data_api_key))
            request.addParam("part", "contentDetails,snippet")
            request.addParam("playlistId", playlist_id)
            request.addParam("maxResults", "50")

            return Klaxon().parse<GetPlaylistItemsResponse>(request.getResult())?.getSongList() ?: emptyList()
        }

        // TODO | Song and artist cache
        fun getSong(videoId: String): Song? {
            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/videos")
            request.addParam("key", MainActivity.getString(R.string.data_api_key))
            request.addParam("part", "contentDetails,snippet,localizations")
            request.addParam("id", videoId)

            val video = Klaxon().parse<VideoInfoResponse>(request.getResult())?.getVideo()
            if (video == null) {
                return null
            }

            return Song(videoId, SongData("", video.snippet.title, video.snippet.description), getArtist(video.snippet.channelId)!!, Date.from(Instant.parse(video.snippet.publishedAt)), java.time.Duration.parse(video.contentDetails.duration))
        }

        val artistCache: MutableMap<String, Artist> = mutableMapOf()

        fun getArtist(channelId: String): Artist? {

            if (artistCache.containsKey(channelId)) {
                return artistCache.getValue(channelId)
            }

            var request = HTTPGetRequest("https://www.googleapis.com/youtube/v3/channels")
            request.addParam("key", MainActivity.getString(R.string.data_api_key))
            request.addParam("part", "contentDetails,snippet,localizations,statistics")
            request.addParam("id", channelId)

            val channel = Klaxon().parse<ChannelInfoResponse>(request.getResult())?.getChannel()
            if (channel == null) {
                return null
            }

            val ret = Artist(channelId, ArtistData(channel.snippet.defaultLanguage, channel.snippet.title, channel.snippet.description),
                Date.from(Instant.parse(channel.snippet.publishedAt)),
                channel.statistics.viewCount,
                channel.statistics.subscriberCount,
                channel.statistics.hiddenSubscriberCount,
                channel.statistics.videoCount
            )

            artistCache[channelId] = ret

            return ret
        }
    }
}

