package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.enums.SongAudioQuality

expect class PlayerDownloadManager(context: PlatformContext) {
    class DownloadStatus {
        val song: Song
        val status: Status
        val quality: SongAudioQuality
        val progress: Float
        val id: String
        enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    }

    open class DownloadStatusListener() {
        open fun onDownloadAdded(status: DownloadStatus)
        open fun onDownloadRemoved(id: String)
        open fun onDownloadChanged(status: DownloadStatus)
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener)
    fun removeDownloadStatusListener(listener: DownloadStatusListener)
    
    fun getDownload(song: Song, callback: (DownloadStatus?) -> Unit)
    fun getDownloads(callback: (List<DownloadStatus>) -> Unit)

    @Synchronized
    fun startDownload(song_id: String, silent: Boolean = false, onCompleted: ((DownloadStatus) -> Unit)? = null)

    fun release()
}
