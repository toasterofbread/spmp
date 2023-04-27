package com.spectre7.spmp.platform

import com.spectre7.spmp.model.Song

expect class PlayerDownloadManager(context: PlatformContext) {
    class DownloadStatus {
        val song: Song
        val status: Status
        val quality: Song.AudioQuality
        val progress: Float
        val id: String
        enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    }

    interface DownloadStatusListener {
        fun onDownloadAdded(status: DownloadStatus)
        fun onDownloadRemoved(id: String)
        fun onDownloadChanged(status: DownloadStatus)
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener)
    fun removeDownloadStatusListener(listener: DownloadStatusListener)
    
    fun getDownloads(callback: (List<DownloadStatus>) -> Unit)

    @Synchronized
    fun startDownload(song_id: String, silent: Boolean = false, onCompleted: ((DownloadStatus) -> Unit)? = null)

    fun release()
}
