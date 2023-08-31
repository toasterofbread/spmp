package com.toasterofbread.spmp.model.mediaitem.playlist

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.platform.getLocalAudioFile
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.OutputStreamWriter

// Reads and writes M3U files based on https://en.wikipedia.org/wiki/M3U
object PlaylistFileConverter {
    fun getPlaylistFileName(playlist: Playlist): String =
        "${playlist.id}.${getFileExtension()}"
    fun getFileExtension(): String = "m3u"

    private fun OutputStreamWriter.writeFileHeaders(playlist: PlaylistData, context: PlatformContext) {
        write(
            buildString {
                appendLine("#EXTM3U")
                appendLine("#PLAYLIST:${playlist.title}")

                val play_count: Int
                if (playlist is LocalPlaylistData) {
                    play_count = playlist.play_count
                }
                else {
                    play_count = playlist.getPlayCount(context.database)
                }
                appendLine("#PLAYCOUNT:$play_count")

                val image_url: String? = playlist.thumbnail_provider?.getThumbnailUrl(MediaItemThumbnailProvider.Quality.HIGH)
                if (image_url != null) {
                    appendLine("#IMAGE:$image_url")
                }
            }
        )
    }

    suspend fun PlaylistData.saveToFile(file: PlatformFile, context: PlatformContext): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            file.outputStream().use { stream ->
                with(stream.writer()) {
                    writeFileHeaders(this@saveToFile, context)

                    val library_dir: PlatformFile = MediaItemLibrary.getLibraryDir(context)

                    for (song in items ?: emptyList()) {
                        val song_path: String

                        val local_file = song.getLocalAudioFile(context)

                        if (local_file != null) {
                            local_file.path
                            song_path = local_file.getRelativePath(library_dir)
                        }
                        else {
                            song_path = "spmp://${song.id}"
                        }

                        val song_title: String? = song.Title.get(context.database)
                        if (song_title != null) {
                            write("\n#EXTINF:-1,$song_title")
                        }

                        write("\n$song_path\n")
                    }

                    flush()
                }
            }
        }
    }

    suspend fun loadFromFile(file: PlatformFile, context: PlatformContext): LocalPlaylistData = withContext(Dispatchers.IO) {
        val playlist_id: String = file.name.removeSuffix(".${getFileExtension()}")
        val playlist = LocalPlaylistData(playlist_id)

        var current_song: SongData = SongData("TEMP")
        val songs: MutableList<SongData> = mutableListOf()
        playlist.items = songs

        var i: Int = 0
        var enable_ext: Boolean = false
        file.inputStream().use { stream ->
            stream.reader().forEachLine { line ->
                if (i++ == 0 && line == "#EXTM3U") {
                    enable_ext = true
                    return@forEachLine
                }

                if (line.isBlank()) {
                    return@forEachLine
                }

                try {
                    if (line.first() == '#') {
                        if (!enable_ext) {
                            return@forEachLine
                        }

                        val line_split = line.split(':', limit = 2)

                        when (line_split[0].uppercase()) {
                            "#PLAYLIST" -> playlist.title = line_split[1]
                            "#PLAYCOUNT" -> playlist.play_count = line_split[1].toInt()
                            "#IMAGE" -> playlist.thumbnail_provider = MediaItemThumbnailProvider.fromImageUrl(line_split[1])
                            "#EXTINF" -> {
                                val split = line_split[1].split(',', limit = 2)

                                val duration_seconds: Int? = split.getOrNull(0)?.toIntOrNull()
                                if (duration_seconds != null && duration_seconds > 0) {
                                    current_song.duration = duration_seconds * 1000L
                                }

                                val song_title: String? = split.getOrNull(1)
                                if (song_title != null) {
                                    current_song.title = song_title
                                }
                            }
                        }
                    }
                    else {
                        val song_id: String = parseLocalSongPath(line)!!
                        current_song.id = song_id
                        songs.add(current_song)

                        current_song = SongData("TEMP")
                    }
                }
                catch (e: Throwable) {
                    SpMp.Log.warning("Failed to parse line ${i + 1} of playlist file at ${file.absolute_path}: \n$e")
                }
            }
        }

        context.database.transaction {
            for (song in songs) {
                song.saveToDatabase(context.database)
            }
        }

        return@withContext playlist
    }

    private fun parseLocalSongPath(path: String): String? {
        if (path.startsWith("spmp://")) {
            return path.substring(7)
        }

        val last_slash_index: Int = path.lastIndexOf('/')
        if (last_slash_index == -1) {
            return null
        }

        val filename: String = path.substring(last_slash_index + 1)
        val dot_index: Int = filename.indexOf('.')

        if (dot_index == -1) {
            return filename
        }

        return filename.substring(0, dot_index)
    }
}
