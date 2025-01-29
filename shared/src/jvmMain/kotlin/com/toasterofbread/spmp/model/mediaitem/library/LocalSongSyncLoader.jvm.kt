package com.toasterofbread.spmp.model.mediaitem.library

import PlatformIO
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.playerservice.ClientServerPlayerService
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.download.LocalSongMetadataProcessor
import com.toasterofbread.spmp.platform.download.SongDownloader
import dev.toastbits.composekit.context.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal actual class LocalSongSyncLoader: SyncLoader<DownloadStatus>() {
    override suspend fun internalPerformSync(context: AppContext): Map<String, DownloadStatus> {
        val downloads: List<DownloadStatus> =
            getAllLocalSongFiles(context, true)

        SpMp._player_state?.interactService { service: Any ->
            if (service is ClientServerPlayerService) {
                service.onLocalSongsSynced(downloads)
            }
        }

        return downloads.associateBy { it.song.id }
    }
}

private suspend fun getAllLocalSongFiles(context: AppContext, allow_partial: Boolean = false): List<DownloadStatus> = withContext(Dispatchers.PlatformIO) {
    val files: List<PlatformFile> = (
        MediaItemLibrary.getLocalSongsDir(context)?.listFiles().orEmpty()
        + if (allow_partial) MediaItemLibrary.getSongDownloadsDir(context)?.listFiles().orEmpty() else emptyList()
    )

    val results: Array<DownloadStatus?> = arrayOfNulls(files.size)
    val db_mutex: Mutex = Mutex()

    files.mapIndexed { index, file ->
        launch(Dispatchers.PlatformIO) {
            val file_info: SongDownloader.Companion.DownloadFileInfo = SongDownloader.getFileDownloadInfo(file)
            if (!allow_partial && file_info.is_partial) {
                return@launch
            }

            var song: Song? =
                file_info.id?.let { SongRef(it) }
                ?: LocalSongMetadataProcessor.readLocalSongMetadata(file, context, load_data = true)
                    ?.also {
                        db_mutex.withLock {
                            it.saveToDatabase(context.database)
                        }
                    }

            if (song == null) {
                song = SongRef('!' + file.absolute_path.hashCode().toString())

                db_mutex.withLock {
                    song.createDbEntry(context.database)
                    song.Title.set(file.name.split('.', limit = 2).firstOrNull() ?: "???", context.database)
                }
            }

            val result: DownloadStatus =
                DownloadStatus(
                    song = song,
                    status = if (file_info.is_partial) DownloadStatus.Status.IDLE else DownloadStatus.Status.FINISHED,
                    quality = null,
                    progress = if (file_info.is_partial) -1f else 1f,
                    id = file_info.file.name,
                    file = file_info.file
                )

            synchronized(results) {
                results[index] = result
            }
        }
    }.joinAll()

    return@withContext results.filterNotNull()
}
