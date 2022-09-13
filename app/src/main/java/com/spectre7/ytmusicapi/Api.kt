package com.spectre7.ytmusicapi

// TODO : Merge with DataApi

import android.util.Log
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import kotlin.concurrent.thread

class Api {

    class YtMusicApi(private val credentials: String) {

        private val api = Python.getInstance().getModule("ytmusicapi").callAttr("YTMusic")

        init {
            api.callAttr("setup", null, credentials)
        }

        fun getSongCounterpartId(song: Song): String? {
            val counterpart = api.callAttr("get_watch_playlist", song.getId(), null, 1).callAttr("get", "tracks").asList().first().callAttr("get", "conuterpart")
            if (counterpart == null) {
                return null
            }
            return counterpart.callAttr("get", "videoId")
        }

        fun getArtistCounterpartId(artist: Artist): String? {
            val results = api.callAttr("search", artist.nativeData.name, "artists", null, 1, true).asList()
            return results.first().callAttr("get", "browseId").toString()
        }

        fun getSongLyricsId(song: Song): String? {
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

            return getLyricsid(song.getCounterpartId())
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            thread {
                try {
                    val lyrics = api.callAttr("get_lyrics", song.getLyricsId())
                    callback(Song.Lyrics(lyrics.callAttr("get", "lyrics").toString(), lyrics.callAttr("get", "source").toString()))
                }
                catch (e: PyException) {
                    callback(null)
                }
            }
        }

        fun getSongRadio(song: Song, limit: Int = 25): List<String> {
            val radio = api.callAttr("get_watch_playlist", song.getId(), null, limit).callAttr("get", "tracks").asList()
            return List(radio.size) {
                radio[it].callAttr("get", "videoId")
            }
        }

        fun getSongRelated(song: Song) {
            // TODO : https://ytmusicapi.readthedocs.io/en/latest/reference.html#ytmusicapi.YTMusic.get_watch_playlist
        }

    }
}