package com.toasterofbread.spmp.platform.download

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.service.playercontroller.DownloadRequestCallback

actual class PlayerDownloadManager actual constructor(context: AppContext) {
    actual open class DownloadStatusListener actual constructor() {
        actual open fun onDownloadAdded(status: DownloadStatus) {}
        actual open fun onDownloadRemoved(id: String) {}
        actual open fun onDownloadChanged(status: DownloadStatus) {}
    }

    actual fun addDownloadStatusListener(listener: DownloadStatusListener) {}

    actual fun removeDownloadStatusListener(listener: DownloadStatusListener) {}

    actual suspend fun getDownload(song: Song): DownloadStatus? = null

    actual suspend fun getDownloads(): List<DownloadStatus> = emptyList()

    actual fun canStartDownload(): Boolean = false

    actual fun startDownload(
        song: Song,
        silent: Boolean,
        custom_uri: String?,
        download_lyrics: Boolean,
        direct: Boolean,
        callback: DownloadRequestCallback?
    ) {
        throw IllegalStateException()
    }

    actual suspend fun deleteSongLocalAudioFile(song: Song) {}

    actual fun release() {}
}
