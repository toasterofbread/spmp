package com.toasterofbread.spmp.platform

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.getSongStreamFormat
import com.toasterofbread.spmp.platform.playerservice.AUTO_DOWNLOAD_SOFT_TIMEOUT
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException

@UnstableApi
internal suspend fun processMediaDataSpec(data_spec: DataSpec, context: AppContext, metered: Boolean): DataSpec {
    val song: SongRef = SongRef(data_spec.uri.toString())

    val download_manager: PlayerDownloadManager = context.download_manager
    var local_file: PlatformFile? = song.getLocalSongFile(context)
    if (local_file != null) {
        println("Playing song ${song.id} from local file $local_file")
        return data_spec.withUri(Uri.parse(local_file.uri))
    }

    if (
        song.getPlayCount(context.database, 7) >= Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.get<Int>(context)
        && (Settings.KEY_AUTO_DOWNLOAD_ON_METERED.get(context) || !metered)
    ) {
        var done: Boolean = false
        runBlocking {
            val initial_status: PlayerDownloadManager.DownloadStatus? = download_manager.getDownload(song)
            when (initial_status?.status) {
                PlayerDownloadManager.DownloadStatus.Status.IDLE, PlayerDownloadManager.DownloadStatus.Status.CANCELLED, PlayerDownloadManager.DownloadStatus.Status.PAUSED, null -> {
                    download_manager.startDownload(song.id, true) { status ->
                        local_file = status.file
                        done = true
                    }
                }
                PlayerDownloadManager.DownloadStatus.Status.ALREADY_FINISHED, PlayerDownloadManager.DownloadStatus.Status.FINISHED -> throw IllegalStateException()
                else -> {}
            }

            val listener: PlayerDownloadManager.DownloadStatusListener = object : PlayerDownloadManager.DownloadStatusListener() {
                override fun onDownloadChanged(status: PlayerDownloadManager.DownloadStatus) {
                    if (status.song.id != song.id) {
                        return
                    }

                    when (status.status) {
                        PlayerDownloadManager.DownloadStatus.Status.IDLE, PlayerDownloadManager.DownloadStatus.Status.DOWNLOADING -> return
                        PlayerDownloadManager.DownloadStatus.Status.PAUSED -> throw IllegalStateException()
                        PlayerDownloadManager.DownloadStatus.Status.CANCELLED -> {
                            done = true
                        }
                        PlayerDownloadManager.DownloadStatus.Status.FINISHED, PlayerDownloadManager.DownloadStatus.Status.ALREADY_FINISHED -> {
                            local_file = song.getLocalSongFile(context)
                            done = true
                        }
                    }

                    download_manager.removeDownloadStatusListener(this)
                }
            }
            download_manager.addDownloadStatusListener(listener)

            var elapsed: Int = 0
            while (!done && elapsed < AUTO_DOWNLOAD_SOFT_TIMEOUT) {
                delay(100)
                elapsed += 100
            }
        }

        if (local_file != null) {
            println("Playing song ${song.id} from local file $local_file")
            return data_spec.withUri(Uri.parse(local_file!!.uri))
        }
    }

    val format: YoutubeVideoFormat =
        getSongStreamFormat(song.id, context).fold(
            { it },
            { throw it }
        )

    try {
        song.LoudnessDbById.setNotNull(format.loudness_db, context.database)
    }
    catch (e: Throwable) {
        e.printStackTrace()
    }

    if (local_file != null) {
        println("Playing song ${song.id} from local file $local_file")
        return data_spec.withUri(Uri.parse(local_file!!.uri))
    }
    else {
        println("Playing song ${song.id} from external format $format stream_url=${format.url}")
        return data_spec.withUri(Uri.parse(format.url))
    }
}
