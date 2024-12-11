package com.toasterofbread.spmp.model.mediaitem.playlist

import SpMp
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.MediaItemSortType
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import PlatformIO
import okio.BufferedSink
import okio.buffer
import okio.use
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.new_playlist_title

// Reads and writes M3U files based on https://en.wikipedia.org/wiki/M3U
object PlaylistFileConverter {
    fun getPlaylistFileName(playlist: Playlist): String =
        "${playlist.id}.${getFileExtension()}"
    fun getFileExtension(): String = "m3u"

    private fun BufferedSink.writeFileHeaders(playlist: PlaylistData, context: AppContext) {
        writeUtf8(
            buildString {
                appendLine("#EXTM3U")

                val title: String? = playlist.name
                if (title != null) {
                    appendLine("#PLAYLIST:$title")
                }

                val play_count: Int = playlist.getPlayCount(context.database)
                if (play_count > 0) {
                    appendLine("#PLAYCOUNT:$play_count")
                }

                val sort_type: MediaItemSortType? = playlist.sort_type
                if (sort_type != null) {
                    appendLine("#SORTTYPE:${sort_type.ordinal}")
                }

                val image_url: String? = playlist.custom_image_url ?: playlist.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH)
                if (image_url != null) {
                    appendLine("#IMAGE:$image_url")
                }

                val image_width: Float? = playlist.image_width
                if (image_width != null) {
                    appendLine("#IMAGEWIDTH:$image_width")
                }
            }
        )
    }

    suspend fun PlaylistData.saveToFile(file: PlatformFile, context: AppContext): Result<Unit> = withContext(Dispatchers.PlatformIO) {
        val playlists_dir: PlatformFile =
            MediaItemLibrary.getLocalPlaylistsDir(context)
            ?: throw RuntimeException("Local playlists dir not available")

        val temp_file: PlatformFile = file.getSibling("${file.name}.tmp")

        val result: Result<Unit> = runCatching {
            temp_file.outputStream().buffer().use { stream ->
                stream.writeFileHeaders(this@saveToFile, context)

                for (song in items ?: emptyList()) {
                    val song_path: String

                    val local_file: PlatformFile? = MediaItemLibrary.getLocalSong(song, context)?.file

                    if (local_file != null) {
                        local_file.path
                        song_path = local_file.getRelativePath(playlists_dir)
                    }
                    else {
                        song_path = "spmp://${song.id}"
                    }

                    val song_title: String? = song.getActiveTitle(context.database)
                    if (song_title != null) {
                        stream.writeUtf8("\n#EXTINF:-1,$song_title")
                    }

                    stream.writeUtf8("\n$song_path\n")
                }

                stream.flush()
            }

            check(file.delete()) { "Deleting existing file failed ($file)" }
            check(temp_file.renameTo(file.name).is_file) { "Renaming temporary file failed ($temp_file -> $file)" }
        }

        return@withContext result
    }

    suspend fun loadFromFile(file: PlatformFile, context: AppContext, save: Boolean = true): LocalPlaylistData? = withContext(Dispatchers.PlatformIO) {
        val required_suffix: String = ".${getFileExtension()}"
        if (!file.name.endsWith(required_suffix)) {
            return@withContext null
        }

        val playlist_id: String = file.name.dropLast(required_suffix.length)
        val playlist = LocalPlaylistData(playlist_id)

        var current_song: SongData = SongData("TEMP")
        val songs: MutableList<SongData> = mutableListOf()
        playlist.items = songs

        var i: Int = 0
        var enable_ext: Boolean = false
        file.inputStream().buffer().use { stream ->
            while (true) {
                val line: String = stream.readUtf8Line() ?: break
                if (i++ == 0 && line == "#EXTM3U") {
                    enable_ext = true
                    continue
                }

                if (line.isBlank()) {
                    continue
                }

                try {
                    if (line.first() == '#') {
                        val line_split: List<String> = line.split(':', limit = 2)

                        when (line_split[0]) {
                            "#PLAYLIST" -> playlist.name = line_split[1]
                            "#PLAYCOUNT" -> playlist.play_count = line_split[1].toInt()
                            "#SORTTYPE" -> playlist.sort_type = MediaItemSortType.entries[line_split[1].toInt()]
                            "#IMAGE" -> playlist.custom_image_url = line_split[1]
                            "#IMAGEWIDTH" -> playlist.image_width = line_split[1].toFloat()
                            "#EXTINF" -> {
                                if (!enable_ext) {
                                    continue
                                }

                                val split: List<String> = line_split[1].split(',', limit = 2)

                                val duration_seconds: Int? = split.getOrNull(0)?.toIntOrNull()
                                if (duration_seconds != null && duration_seconds > 0) {
                                    current_song.duration = duration_seconds * 1000L
                                }

                                val song_title: String? = split.getOrNull(1)
                                if (song_title != null) {
                                    current_song.name = song_title
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
                    println("WARNING: Failed to parse line ${i + 1} of playlist file at ${file.absolute_path}\nLine: $line\nError: $e")
                }
            }
        }

        if (playlist.name == null) {
            playlist.name = getString(Res.string.new_playlist_title)
        }

        if (save) {
            context.database.transaction {
                for (song in songs) {
                    song.saveToDatabase(context.database)
                }
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
