package com.toasterofbread.spmp.platform

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.toasterofbread.spmp.model.mediaitem.db.getPlayCount
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.model.mediaitem.song.getSongTargetAudioFormat
import com.toasterofbread.spmp.platform.download.DownloadStatus
import com.toasterofbread.spmp.platform.download.PlayerDownloadManager
import com.toasterofbread.spmp.platform.playerservice.AUTO_DOWNLOAD_SOFT_TIMEOUT
import dev.toastbits.composekit.context.PlatformFile
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(UnstableApi::class)
internal suspend fun getMediaDataSpecPlaybackUri(
    data_spec: DataSpec,
    context: AppContext,
    endpoint: VideoFormatsEndpoint? = null
): Result<Uri> = runCatching {
    val song: SongRef = SongRef(data_spec.uri.toString())

    println("Loading playback URI for song using ${endpoint ?: "default endpoint"}")

    val download_manager: PlayerDownloadManager = context.download_manager

    var local_file: PlatformFile? =
        withTimeoutOrNull(1000) {
            MediaItemLibrary.getLocalSong(song, context)?.file
        }

    if (local_file != null) {
        println("Playing song ${song.id} from local file $local_file")
        return@runCatching Uri.parse(local_file.uri)
    }

    val auto_download_enabled: Boolean = context.settings.Streaming.AUTO_DOWNLOAD_ENABLED.get()

    if (
        auto_download_enabled
        && song.getPlayCount(context.database, 7) >= context.settings.Streaming.AUTO_DOWNLOAD_THRESHOLD.get()
        && (context.settings.Streaming.AUTO_DOWNLOAD_ON_METERED.get() || !context.isConnectionMetered())
        && !MediaItemLibrary.song_sync_in_progress
    ) {
        var done: Boolean = false
        runBlocking {
            val initial_status: DownloadStatus? = withTimeoutOrNull(1000) {
                download_manager.getDownload(song)
            }
            when (initial_status?.status) {
                DownloadStatus.Status.IDLE, DownloadStatus.Status.CANCELLED, DownloadStatus.Status.PAUSED, null -> {
                    download_manager.startDownload(song, true) { status ->
                        local_file = status?.file
                        done = true
                    }
                }
                DownloadStatus.Status.ALREADY_FINISHED, DownloadStatus.Status.FINISHED -> throw IllegalStateException()
                else -> {}
            }

            val listener: PlayerDownloadManager.DownloadStatusListener = object : PlayerDownloadManager.DownloadStatusListener() {
                override fun onDownloadChanged(status: DownloadStatus) {
                    if (status.song.id != song.id) {
                        return
                    }

                    when (status.status) {
                        DownloadStatus.Status.IDLE, DownloadStatus.Status.DOWNLOADING -> return
                        DownloadStatus.Status.PAUSED -> throw IllegalStateException()
                        DownloadStatus.Status.CANCELLED -> {
                            done = true
                        }
                        DownloadStatus.Status.FINISHED, DownloadStatus.Status.ALREADY_FINISHED -> {
                            launch {
                                local_file = MediaItemLibrary.getLocalSong(song, context)?.file
                                done = true
                            }
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
            return@runCatching Uri.parse(local_file!!.uri)
        }
    }

    println("Loading stream format for song ${song.id}")

    val format: YoutubeVideoFormat =
        getSongTargetAudioFormat(song.id, context, endpoint).fold(
            { it },
            {
                MediaItemLibrary.getLocalSong(song, context)?.file?.also { local_file ->
                    println("Playing song ${song.id} from local file $local_file")
                    return@runCatching Uri.parse(local_file.uri)
                }

                it.printStackTrace()
                throw it
            }
        )

    try {
        song.LoudnessDb.setNotNull(format.loudness_db, context.database)
    }
    catch (e: Throwable) {
        e.printStackTrace()
    }

    println("Playing song ${song.id} from external format $format stream_url=${format.url}")
    return@runCatching Uri.parse(format.url)
}
