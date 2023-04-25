package com.spectre7.spmp.platform

import com.spectre7.spmp.model.Song

expect class PlayerDownloadManager(context: PlatformContext) {
    class DownloadStatus {
        val song: Song
        val status: Status
        val quality: Song.AudioQuality
        val progress: Float

        enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
    }

    interface DownloadStatusListener {
        fun onSongDownloadStatusChanged(song_id: String, status: DownloadStatus.Status)
    }

    fun addDownloadStatusListener(listener: DownloadStatusListener)
    fun removeDownloadStatusListener(listener: DownloadStatusListener)


    fun getDownloadedSongs(): List<DownloadStatus>
    @Synchronized
    fun startDownload(song_id: String, silent: Boolean = false, onCompleted: ((DownloadStatus) -> Unit)? = null)

    fun getSongDownloadStatus(song_id: String, callback: (DownloadStatus) -> Unit)

    fun release()
}
