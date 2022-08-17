package com.spectre7.ytmusicapi

import com.beust.klaxon.Klaxon
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.ResourceType

class Api {

    class YtMusicApi(private val credentials: String) {

        private val json = Python.getInstance().getModule("json")
        private val api: PyObject = Python.getInstance().getModule("ytmusicapi").callAttr("YTMusic")
        private val klaxon: Klaxon = Klaxon()

        init {
            api.callAttr("setup", null, credentials)
        }

        private fun pyToJson(obj: PyObject): String {
            return json.callAttr("dumps", obj).toString()
        }

        class SearchResult(
            val resultType: String,
            val videoId: String = "",
            val browseId: String = ""
        )

        class SearchResultContainer {
            val songs = mutableListOf<String>()
            val videos = mutableListOf<String>()
            val artists = mutableListOf<String>()
            val albums = mutableListOf<String>()
            val playlists = mutableListOf<String>()
        }

//        fun search(query: String, type: ResourceType, scope: String? = null, limit: Int = 10, ignore_spelling: Boolean = false): SearchResultContainer {
//            val results = klaxon.parseArray<SearchResult>(pyToJson(api.callAttr("search", query,
//                when (type) {
//                    ResourceType.SONG -> "songs"
//                    ResourceType.VIDEO -> "videos"
//                    ResourceType.ARTIST -> "artists"
//                    ResourceType.ALBUM -> "albums"
//                    ResourceType.PLAYLIST -> "playlists"
//                }, scope, limit, ignore_spelling)))!!
//
//            val ret = SearchResultContainer()
//
//            for (result in results) {
//                when (result.resultType) {
//                    "song" -> ret.songs.add(result.videoId)
//                    "video" -> ret.videos.add(result.videoId)
//                    "artist" -> ret.artists.add(result.browseId)
//                    "album" -> ret.albums.add(result.browseId)
//                    "playlist" -> ret.playlists.add(result.browseId)
//                }
//            }
//
//            return ret
//        }

        data class ArtistSongList(val tracks: List<ArtistSongInfo>) {
            data class ArtistSongInfo(val videoId: String, val duration: String, val isAvailable: Boolean, val artists: List<ArtistInfo>)
            data class ArtistInfo(val id: String)
        }

        data class SongInfo(val playabilityStatus: PlayabilityStatus) {
            data class PlayabilityStatus(val status: String)

            fun isPlayable(): Boolean {
                return playabilityStatus.status != "ERROR" && playabilityStatus.status != "UNPLAYABLE"
            }
        }

        fun getSong(video_id: String, signature_timestamp: Int? = null): SongInfo {
            return Klaxon().parse<SongInfo>(pyToJson(api.callAttr("get_song", video_id, signature_timestamp)))!!
        }

        fun isSongAvailable(video_id: String): Boolean {
            return getSong(video_id).isPlayable()
        }

    }

}