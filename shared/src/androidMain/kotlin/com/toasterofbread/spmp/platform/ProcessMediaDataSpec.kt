package com.toasterofbread.spmp.platform

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.SongRef
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.getSongStreamFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.time.Duration

@UnstableApi
internal fun processMediaDataSpec(data_spec: DataSpec, context: PlatformContext, metered: Boolean): DataSpec {
    val song = SongRef(data_spec.uri.toString())

    val download_manager = context.download_manager
    var local_file: File? = download_manager.getSongLocalFile(song)
    if (local_file != null) {
        println("Playing song ${song.id} from local file $local_file")
        return data_spec.withUri(Uri.fromFile(local_file))
    }

    if (
        song.getPlayCount(context.database, Duration.ofDays(7)) >= Settings.KEY_AUTO_DOWNLOAD_THRESHOLD.get<Int>(context)
        && (Settings.KEY_AUTO_DOWNLOAD_ON_METERED.get(context) || !metered)
    ) {
        var done = false
        runBlocking {
            download_manager.getDownload(song) { initial_status ->
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

                val listener = object : PlayerDownloadManager.DownloadStatusListener() {
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
                                local_file = download_manager.getSongLocalFile(song)
                                done = true
                            }
                        }

                        download_manager.removeDownloadStatusListener(this)
                    }
                }
                download_manager.addDownloadStatusListener(listener)
            }

            var elapsed = 0
            while (!done && elapsed < AUTO_DOWNLOAD_SOFT_TIMEOUT) {
                delay(100)
                elapsed += 100
            }
        }

        if (local_file != null) {
            println("Playing song ${song.id} from local file $local_file")
            return data_spec.withUri(Uri.fromFile(local_file))
        }
    }

    val format = getSongStreamFormat(song.id).fold(
        { it },
        { throw IOException(it) }
    )

    return if (local_file != null) {
        println("Playing song ${song.id} from local file $local_file")
        data_spec.withUri(Uri.fromFile(local_file))
    } else {
        println("Playing song ${song.id} from external format $format")
        data_spec.withUri(Uri.parse(format.stream_url))
    }
}
