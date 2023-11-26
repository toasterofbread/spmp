package com.toasterofbread.spmp.platform.download

import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.platform.getPlatformForbiddenFilenameCharacters
import com.toasterofbread.spmp.model.lyrics.LyricsFileConverter
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.mediaitem.song.getSongFormatByQuality
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.StreamingSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.YoutubeVideoFormat
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrThrowHere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService

private const val FILE_DOWNLOADING_SUFFIX = ".part"

abstract class SongDownloader(
    private val context: AppContext,
    private val download_executor: ExecutorService,
    private val max_retry_count: Int = 3
) {
    protected abstract fun getAudioFileDurationMs(file: PlatformFile): Long?
    protected abstract fun onDownloadStatusChanged(download: Download, started: Boolean = false)
    protected open fun onDownloadProgress() {}
    protected open fun onPausedChanged() {}
    protected open fun onFirstDownloadStarting(download: Download) {}
    protected open fun onLastDownloadFinished() {}

    inner class Download(
        val song: Song,
        val quality: SongAudioQuality,
        var silent: Boolean,
        val instance: Int,
        val file_uri: String?
    ) {
        var song_file: PlatformFile? = runBlocking {
            // This is fine :)
            song.getLocalSongFile(context, allow_partial = true)
        }
        var lyrics_file: PlatformFile? = song.getLocalLyricsFile(context, allow_partial = true)

        var status: DownloadStatus.Status =
            if (song_file?.let { isFileDownloadInProgressForSong(it, song) } == false) DownloadStatus.Status.ALREADY_FINISHED
            else DownloadStatus.Status.IDLE
            set(value) {
                if (field != value) {
                    field = value
                    onDownloadStatusChanged(this)
                }
            }

        val finished: Boolean get() = status == DownloadStatus.Status.ALREADY_FINISHED || status == DownloadStatus.Status.FINISHED
        val downloading: Boolean get() = status == DownloadStatus.Status.DOWNLOADING || status == DownloadStatus.Status.PAUSED

        var cancelled: Boolean = false
            private set

        var downloaded: Long = 0
        var total_size: Long = -1

        val progress: Float get() = if (total_size < 0f) 0f else downloaded.toFloat() / total_size
        val percent_progress: Int get() = (progress * 100).toInt()

        fun getStatusObject(): DownloadStatus =
            DownloadStatus(
                song,
                status,
                quality,
                progress,
                instance.toString(),
                song_file
            )

        fun cancel() {
            cancelled = true
        }

        fun generatePath(extension: String, in_progress: Boolean): String {
            return getDownloadPath(song, extension, in_progress, context)
        }

        override fun toString(): String =
            "Download(id=${song.id}, quality=$quality, silent=$silent, instance=$instance, file=$song_file)"
    }

    private var download_inc: Int = 0
    private fun getOrCreateDownload(song: Song, silent: Boolean, file_uri: String?): Download {
        synchronized(downloads) {
            for (download in downloads) {
                if (download.song.id == song.id) {
                    return download
                }
            }
            return Download(song, Settings.getEnum(StreamingSettings.Key.DOWNLOAD_AUDIO_QUALITY), silent, download_inc++, file_uri)
        }
    }

    fun getAllDownloadsStatus(): List<DownloadStatus> =
        downloads.map { it.getStatusObject() }

    fun getDownloadStatus(song: Song): DownloadStatus? =
        downloads.firstOrNull { it.song.id == song.id }?.getStatusObject()

    fun getTotalDownloadProgress(): Float {
        if (downloads.isEmpty()) {
            return 1f
        }

        val finished = completed_downloads + failed_downloads

        var total_progress: Float = finished.toFloat()
        for (download in downloads) {
            total_progress += download.progress
        }
        return total_progress / (downloads.size + finished)
    }

    companion object {
        fun getDownloadPath(song: Song, extension: String, in_progress: Boolean, context: AppContext): String {
            val forbidden_chars: String = getPlatformForbiddenFilenameCharacters()

            val filename: String = (
                if (in_progress) song.id
                else song.getActiveTitle(context.database) ?: song.id
            ).filter { !forbidden_chars.contains(it) }

            if (in_progress) {
                return "$filename.$extension$FILE_DOWNLOADING_SUFFIX"
            }
            else {
                return "$filename.$extension"
            }
        }

        fun isFileDownloadInProgress(file: PlatformFile): Boolean =
            file.name.endsWith(FILE_DOWNLOADING_SUFFIX)

        fun isFileDownloadInProgressForSong(file: PlatformFile, song: Song): Boolean =
            isFileDownloadInProgress(file) && file.name.startsWith("${song.id}.")

        fun getSongIdOfInProgressDownload(file: PlatformFile): String? =
            if (file.name.endsWith(FILE_DOWNLOADING_SUFFIX)) file.name.split('.', limit = 2).first() else null
    }

    private val song_download_dir: PlatformFile get() = MediaItemLibrary.getLocalSongsDir(context)

    val downloads: MutableList<Download> = mutableListOf()
    private var stopping: Boolean = false
    fun stop() {
        synchronized(download_executor) {
            stopping = true
        }
    }

    var start_time_ms: Long = 0
        private set
    var completed_downloads: Int = 0
        private set
    var failed_downloads: Int = 0
        private set
    var cancelled: Boolean = false
        private set

    var paused: Boolean = false
        set(value) {
            field = value
            onPausedChanged()
        }

    fun release() {
        download_executor.shutdownNow()
        try {
            downloads.clear()
        }
        catch (_: Throwable) {}
    }

    fun startDownload(song: Song, silent: Boolean, file_uri: String?, callback: (Download, Result<PlatformFile?>) -> Unit) {
        val download: Download = getOrCreateDownload(
            song,
            silent = silent,
            file_uri = file_uri
        )

        synchronized(download) {
            if (download.finished) {
                callback(download, Result.success(download.song_file))
                return
            }

            if (download.downloading) {
                if (paused) {
                    paused = false
                }
                callback(download, Result.success(null))
                return
            }

            synchronized(downloads) {
                if (downloads.isEmpty()) {
                    onFirstDownloadStarting(download)
                    start_time_ms = System.currentTimeMillis()
                    completed_downloads = 0
                    failed_downloads = 0
                    cancelled = false
                }

                downloads.add(download)
                onDownloadStatusChanged(download, true)
            }
        }

        onDownloadProgress()

        download_executor.submit {
            runBlocking {
                var result: Result<PlatformFile?>? = null
                var retry_count: Int = 0

                while (
                    retry_count++ < max_retry_count && (
                        result == null || download.status == DownloadStatus.Status.IDLE || download.status == DownloadStatus.Status.PAUSED
                    )
                ) {
                    if (paused && !download.cancelled) {
                        onDownloadProgress()
                        delay(500)
                        continue
                    }

                    result =
                        try {
                            performDownload(download)
                        }
                        catch (e: Exception) {
                            Result.failure(e)
                        }
                }

                synchronized(downloads) {
                    downloads.removeAll { it.song.id == download.song.id }

                    if (downloads.isEmpty()) {
                        cancelled = download.cancelled
                        onLastDownloadFinished()
                    }

                    if (result?.isSuccess == true) {
                        completed_downloads += 1
                    }
                    else {
                        failed_downloads += 1
                    }
                }

                callback(download, result ?: Result.failure(RuntimeException("Download not performed")))

                onDownloadProgress()
            }
        }
    }

    fun cancelDownloads(filter: (Download) -> Boolean) {
        synchronized(downloads) {
            for (download in downloads) {
                if (filter(download)) {
                    download.cancel()
                }
            }
        }
    }

    private suspend fun performDownload(download: Download): Result<PlatformFile?> = withContext(Dispatchers.IO) {
        val format: YoutubeVideoFormat = getSongFormatByQuality(download.song.id, download.quality, context).fold(
            { it },
            { return@withContext Result.failure(it) }
        )

        val connection: HttpURLConnection = URL(format.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 3000
        connection.setRequestProperty("Range", "bytes=${download.downloaded}-")

        try {
            connection.connect()
        }
        catch (e: Throwable) {
            return@withContext Result.failure(RuntimeException(connection.url.toString(), e))
        }

        if (connection.responseCode != 200 && connection.responseCode != 206) {
            return@withContext Result.failure(
                ConnectException(
                "${download.song.id}: Server returned code ${connection.responseCode} ${connection.responseMessage}"
            )
            )
        }

        val format_extension: String =
            when (connection.contentType) {
                "audio/webm" -> "webm"
                "audio/mp4" -> "mp4"
                else -> return@withContext Result.failure(NotImplementedError(connection.contentType))
            }

        var file: PlatformFile? = download.song_file
        check(song_download_dir.mkdirs()) { song_download_dir.toString() }

        if (file == null) {
            file = song_download_dir.resolve(download.generatePath(format_extension, true))
        }

        check(file.name.endsWith(FILE_DOWNLOADING_SUFFIX))

        val data: ByteArray = ByteArray(4096)
        val input_stream: InputStream = connection.inputStream
        val output_stream: OutputStream = file.outputStream(true)

        fun close(status: DownloadStatus.Status) {
            input_stream.close()
            output_stream.close()
            connection.disconnect()
            download.status = status
        }

        try {
            download.total_size = connection.contentLengthLong + download.downloaded
            download.status = DownloadStatus.Status.DOWNLOADING

            while (true) {
                val size = input_stream.read(data)
                if (size < 0) {
                    break
                }

                synchronized(download_executor) {
                    if (stopping || download.cancelled) {
                        close(DownloadStatus.Status.CANCELLED)
                        return@withContext Result.success(null)
                    }
                    if (paused) {
                        close(DownloadStatus.Status.PAUSED)
                        return@withContext Result.success(null)
                    }
                }

                download.downloaded += size
                output_stream.write(data, 0, size)

                onDownloadProgress()
            }

            runBlocking {
                launch {
                    download.song.Duration.setNotNull(getAudioFileDurationMs(file), context.database)
                    LocalSongMetadataProcessor.addMetadataToLocalSong(download.song, file, format_extension, context)
                }
                launch {
                    if (download.lyrics_file == null) {
                        val lyrics_file: PlatformFile = MediaItemLibrary.getLocalLyricsFile(download.song, context)
                        SongLyricsLoader.loadBySong(download.song, context)?.onSuccess { lyrics ->
                            with (LyricsFileConverter) {
                                val exception: Throwable? = lyrics.saveToFile(lyrics_file, context).exceptionOrNull()
                                exception?.printStackTrace()
                            }
                        }
                    }
                }
            }

            close(DownloadStatus.Status.FINISHED)
        }
        catch (e: Throwable) {
            e.printStackTrace()
            close(DownloadStatus.Status.CANCELLED)
        }

        if (download.file_uri != null) {
            val uri_file: PlatformFile = context.getUserDirectoryFile(download.file_uri)
            uri_file.outputStream().use { output ->
                Files.copy(Path.of(URI.create(file.absolute_path)), output)
            }
            file.delete()
            download.song_file = uri_file
        }
        else {
            val renamed: PlatformFile = file.renameTo(
                download.generatePath(format_extension, false)
            )
            download.song_file = renamed
        }

        download.status = DownloadStatus.Status.FINISHED

        return@withContext Result.success(download.song_file)
    }
}
