package com.spectre7.ytmusicapi

import android.util.Log
import com.beust.klaxon.Klaxon
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Song
import kotlin.concurrent.thread

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

        fun getSongEquivalent(song: Song): String? {
            val lyrics = api.callAttr("get_watch_playlist", song.getId(), null, 1).callAttr("get", "lyrics")
            if (lyrics != null) {
                return lyrics.toString()
            }

            val results = api.callAttr("search", song.getTitle(), "songs").asList()
            for (video in results) {
                Log.d("CANDIDATE", video.callAttr("get", "title").toString())
                for (artist in video.callAttr("get", "artists").asList()) {

                    Log.d("A", artist.callAttr("get", "id").toString())
                    Log.d("B", song.artist.getEquivalent().toString())

                    if (artist.callAttr("get", "id").toString() == song.artist.getEquivalent()) {
                        val id = video.callAttr("get", "videoId").toString()

                        Log.d("ID", id)

                        val playlist = api.callAttr("get_watch_playlist", id, null, 1)

                        Log.d("PLAYLIST", playlist.toString())

                        return playlist.callAttr("get", "lyrics").toString()
                    }
                }
            }
            return null
        }

        fun getArtistEquivalent(artist: Artist): String? {
            val results = api.callAttr("search", artist.nativeData.name, "artists", null, 1, true).asList()
            return results.first().callAttr("get", "browseId").toString()
        }

        fun getSongLyrics(song: Song, callback: (Song.Lyrics?) -> Unit) {
            thread {
                try {
                    val result = api.callAttr("get_lyrics", song.getEquivalent())
                    callback(Song.Lyrics(result.callAttr("get", "lyrics").toString(), result.callAttr("get", "source").toString()))
                }
                catch (e: PyException) {
                    callback(null)
                }
            }
        }

    }
}