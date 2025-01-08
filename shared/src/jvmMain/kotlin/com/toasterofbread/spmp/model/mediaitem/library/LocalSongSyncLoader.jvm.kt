package com.toasterofbread.spmp.model.mediaitem.library

import PlatformIO
import SpMp
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.playlist.LocalPlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.LocalSongMetadataProcessor
import com.toasterofbread.spmp.platform.download.SongDownloader
import com.toasterofbread.spmp.platform.download.getItemWithOrForTitle
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import dev.toastbits.composekit.platform.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath

internal actual class LocalSongSyncLoader: SyncLoader<DownloadStatus>() {
    override suspend fun internalPerformSync(context: AppContext): Map<String, DownloadStatus> {
        val downloads: List<DownloadStatus> =
            try {
                getAllLocalSongFiles(context, true)
            }
            catch (e: Throwable) {
                RuntimeException("getAllLocalSongFiles in internalPerformSync failed", e).printStackTrace()
                throw e
            }

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onLocalSongsSynced(downloads)
            }
        }

        return downloads.associateBy { it.song.id }
    }
}

fun PlatformFile.walk(): Sequence<PlatformFile> =
    sequence {
        for (file in listFiles().orEmpty()) {
            if (file.is_file) {
                yield(file)
            }
            else if (file.is_directory) {
                yieldAll(file.walk())
            }
        }
    }

private suspend fun getAllLocalSongFiles(context: AppContext, allow_partial: Boolean = false): List<DownloadStatus> = withContext(Dispatchers.PlatformIO) {
    val jobs: MutableList<Deferred<DownloadStatus?>> = mutableListOf()

    val db_mutex: Mutex = Mutex()
    val metadata_mutex: Mutex = Mutex()
    val artists: MutableMap<String, Artist> = mutableMapOf()
    val playlists: MutableMap<Pair<String, String>, LocalPlaylistData> = mutableMapOf()

    val songsDirectory: PlatformFile? = MediaItemLibrary.getLocalSongsDir(context)
    if (songsDirectory != null) {
        val songsPath: Path = songsDirectory.path.toPath()

        for (file in songsDirectory.walk()) {
            if (!file.is_file) {
                continue
            }

            jobs.add(
                getDownloadStatusFromFile(
                    file = file,
                    allow_partial = allow_partial,
                    context = context,
                    db_mutex = db_mutex,
                    withSong = { song ->
                        val filePath: Path = file.path.toPath()
                        val path: Path = filePath.relativeTo(songsPath)

                        if (path.segments.size <= 1) {
                            return@getDownloadStatusFromFile
                        }

                        val artistName: String = path.segments[0]
                        val playlistName: String? =
                            if (path.segments.size == 2) null
                            else path.segments[1]

                        val artist: Artist? =
                            metadata_mutex.withLock {
                                if (playlistName != null) {
                                    val playlist: LocalPlaylistData =
                                        playlists.getOrPut(artistName to playlistName) {
                                            LocalPlaylistData("!${songsPath.resolve(artistName).resolve(playlistName)}").apply {
                                                name = playlistName
                                                loaded = true
                                            }
                                        }
                                    playlist.items = playlist.items.orEmpty() + SongData(song.id)
                                }

                                if (song.Artists.get(context.database) == null) {
                                    artists.getOrPut(artistName) {
                                        song.getItemWithOrForTitle(null, artistName) { ArtistData(it) }!!.apply {
                                            loaded = true
                                            db_mutex.withLock {
                                                saveToDatabase(context.database)
                                            }
                                        }
                                    }
                                }
                                else null
                            }

                        if (artist != null) {
                            db_mutex.withLock {
                                song.Artists.setAlt(listOf(artist), context.database)
                            }
                        }
                    }
                )
            )
        }
    }

    for (file in MediaItemLibrary.getSongDownloadsDir(context)?.listFiles().orEmpty()) {
        if (!file.is_file) {
            continue
        }

        jobs.add(getDownloadStatusFromFile(file, allow_partial, context, db_mutex))
    }

    val ret: List<DownloadStatus> = jobs.mapNotNull { it.await() }

    MediaItemLibrary.updateExtraLocalPlaylists(playlists.values.toList())

    return@withContext ret
}

private fun CoroutineScope.getDownloadStatusFromFile(
    file: PlatformFile,
    allow_partial: Boolean,
    context: AppContext,
    db_mutex: Mutex,
    withSong: suspend (Song) -> Unit = {}
): Deferred<DownloadStatus?> = async(Dispatchers.PlatformIO) {
    val file_info: SongDownloader.Companion.DownloadFileInfo =
        SongDownloader.getFileDownloadInfo(file)
    if (!allow_partial && file_info.is_partial) {
        return@async null
    }

    var song: Song? =
        file_info.id?.let { SongRef(it) }
            ?: LocalSongMetadataProcessor.readLocalSongMetadata(file, context, load_data = true)
                ?.also {
                    withSong(it)

                    db_mutex.withLock {
                        it.saveToDatabase(context.database)
                    }
                }

    if (song == null) {
        song = SongRef('!' + file.absolute_path.hashCode().toString())

        db_mutex.withLock {
            song.createDbEntry(context.database)

            val lastDot: Int = file.name.lastIndexOf('.')
            song.Title.set(
                if (lastDot == -1) file.name
                else file.name.substring(0, lastDot),
                context.database
            )
        }

        withSong(song)
    }


    return@async DownloadStatus(
        song = song,
        status = if (file_info.is_partial) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
        quality = null,
        progress = if (file_info.is_partial) -1f else 1f,
        id = file_info.file.name,
        file = file_info.file
    )
}
