package com.spectre7.spmp.platform

import com.spectre7.spmp.model.Song

actual class PlayerDownloadManager actual constructor(context: PlatformContext) {
    actual class DownloadStatus {
        actual val song: Song
            get() = TODO("Not yet implemented")
        actual val status: Status
            get() = TODO("Not yet implemented")
        actual val quality: Song.AudioQuality
            get() = TODO("Not yet implemented")
        actual val progress: Float
            get() = TODO("Not yet implemented")

        actual enum class Status { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }

    }

    actual interface DownloadStatusListener {
        actual fun onSongDownloadStatusChanged(song_id: String, status: DownloadStatus.Status)
    }

    actual fun addDownloadStatusListener(listener: DownloadStatusListener) {
    }

    actual fun removeDownloadStatusListener(listener: DownloadStatusListener) {
    }

    actual fun getDownloadedSongs(): List<DownloadStatus> {
        TODO("Not yet implemented")
    }

    @Synchronized
    actual fun startDownload(
        song_id: String,
        silent: Boolean,
        onCompleted: ((DownloadStatus) -> Unit)?
    ) {
    }

    actual fun getSongDownloadStatus(
        song_id: String,
        callback: (DownloadStatus) -> Unit
    ) {
    }

    actual fun release() {
    }

}