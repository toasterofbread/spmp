package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.toastercomposetools.platform.PlatformFile

actual class PlayerDownloadManager actual constructor(context: AppContext) {
    actual class DownloadStatus {
        actual val song: Song
            get() = TODO("Not yet implemented")
        actual val status: Status
            get() = TODO("Not yet implemented")
        actual val quality: SongAudioQuality?
            get() = TODO("Not yet implemented")
        actual val progress: Float
            get() = TODO("Not yet implemented")
        actual val id: String
            get() = TODO("Not yet implemented")

        actual enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }

        actual fun isCompleted(): Boolean = TODO()
    }

    actual open class DownloadStatusListener actual constructor() {
        actual open fun onDownloadAdded(status: DownloadStatus) {
        }

        actual open fun onDownloadRemoved(id: String) {
        }

        actual open fun onDownloadChanged(status: DownloadStatus) {
        }
    }

    actual fun addDownloadStatusListener(listener: DownloadStatusListener) {
    }

    actual fun removeDownloadStatusListener(listener: DownloadStatusListener) {
    }

    @Synchronized
    actual fun startDownload(
        song_id: String,
        silent: Boolean,
        onCompleted: ((DownloadStatus) -> Unit)?,
    ) {
    }

    actual fun release() {
    }

    actual suspend fun getDownload(song: Song): DownloadStatus? {
        return null // TODO
    }

    actual suspend fun getDownloads(): List<DownloadStatus> {
        return emptyList() // TODO
    }

    actual suspend fun deleteSongLocalAudioFile(song: Song) {
    }
}

actual fun Song.getLocalAudioFile(
    context: AppContext,
    allow_partial: Boolean,
): PlatformFile? {
    return null // TODO
}
