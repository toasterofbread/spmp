package com.toasterofbread.spmp.platform.download

import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.model.lyrics.LyricsFileConverter
import com.toasterofbread.spmp.model.mediaitem.library.MediaItemLibrary
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongAudioQuality
import com.toasterofbread.spmp.model.mediaitem.song.getSongAudioFormatByQuality
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.util.platform.getPlatformForbiddenFilenameCharacters
import dev.toastbits.ytmkt.model.external.YoutubeVideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.buffer
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService

private const val FILE_DOWNLOADING_SUFFIX = ".part"

abstract class SongDownloader(
    private val context: AppContext,
    private val download_executor: ExecutorService,
    private val max_retry_count: Int = 3
) {
    private fun getSongDownloadDir(): PlatformFile = MediaItemLibrary.getSongDownloadsDir(context)!!
    private suspend fun getSongStorageDir(): PlatformFile = MediaItemLibrary.getLocalSongsDir(context)!!

    protected abstract fun getAudioFileDurationMs(file: PlatformFile): Long?
    protected abstract fun onDownloadStatusChanged(download: Download, started: Boolean = false)
    protected open fun onDownloadProgress() {}
    protected open fun onPausedChanged() {}
    protected open suspend fun onFirstDownloadStarting(download: Download) {}
    protected open fun onLastDownloadFinished() {}

    inner class Download(
        val song: Song,
        val quality: SongAudioQuality,
        var silent: Boolean,
        val instance: Int,
        val custom_uri: String?,
        val download_lyrics: Boolean = true,
        val direct: Boolean = false
    ) {
        val mutex: Mutex by lazy { Mutex() }

        // This is fine :)
        var song_file: PlatformFile? = runBlocking {
            MediaItemLibrary.getLocalSong(song, context)?.file
        }
        var lyrics_file: PlatformFile? = runBlocking {
            MediaItemLibrary.getLocalLyrics(context, song, allow_partial = true)
        }

        var status: DownloadStatus.Status =
            if (song_file?.let { getFileDownloadInfo(it).is_partial } == false) DownloadStatus.Status.ALREADY_FINISHED
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

        suspend fun getDownloadFile(extension: String): PlatformFile {
            if (direct) {
                return getDestinationFile(extension)
            }

            val song_download_dir: PlatformFile = getSongDownloadDir()
            check(song_download_dir.mkdirs()) { "Could not create song download directory $song_download_dir" }
            return song_download_dir.resolve(generatePath(extension, true))
        }

        suspend fun getDestinationFile(extension: String): PlatformFile =
            custom_uri?.let { context.getUserDirectoryFile(it) } ?: getSongStorageDir().resolve(generatePath(extension, false))

        override fun toString(): String =
            "Download(id=${song.id}, quality=$quality, silent=$silent, instance=$instance, file=$song_file)"

        private fun generatePath(extension: String, in_progress: Boolean): String {
            return getDownloadPath(song, extension, !direct && in_progress, context)
        }
    }

    private var download_inc: Int = 0
    private suspend fun getOrCreateDownload(song: Song, silent: Boolean, custom_uri: String?, download_lyrics: Boolean, direct: Boolean): Download {
        val audio_quality = context.settings.Streaming.DOWNLOAD_AUDIO_QUALITY.get()
        synchronized(downloads) {
            for (download in downloads) {
                if (download.song.id == song.id) {
                    return download
                }
            }
            return Download(song, audio_quality, silent, download_inc++, custom_uri, download_lyrics, direct)
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

        data class DownloadFileInfo(val id: String?, val is_partial: Boolean, val file: PlatformFile)

        fun getFileDownloadInfo(file: PlatformFile): DownloadFileInfo {
            val is_partial: Boolean = file.name.endsWith(FILE_DOWNLOADING_SUFFIX)
            return DownloadFileInfo(
                if (is_partial) file.name.split('.', limit = 2).first() else null,
                is_partial,
                file
            )
        }
    }

    val downloads: MutableList<Download> = mutableListOf()
    val downloads_mutex: Mutex = Mutex()

    suspend inline fun withDownloads(action: (MutableList<Download>) -> Unit) {
        downloads_mutex.withLock {
            action(downloads)
        }
    }

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

    suspend fun startDownload(
        song: Song,
        silent: Boolean,
        custom_uri: String?,
        download_lyrics: Boolean,
        direct: Boolean,
        callback: (Download, Result<PlatformFile?>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val download: Download =
            getOrCreateDownload(
                song,
                silent = silent,
                custom_uri = custom_uri,
                download_lyrics = download_lyrics,
                direct = direct
            )

        download.mutex.withLock {
            if (download.finished) {
                callback(download, Result.success(download.song_file))
                return@withContext
            }

            if (download.downloading) {
                if (paused) {
                    paused = false
                }
                callback(download, Result.success(null))
                return@withContext
            }

            withDownloads {
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

                withDownloads {
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

    suspend fun cancelDownloads(filter: (Download) -> Boolean) = withContext(Dispatchers.IO) {
        withDownloads {
            for (download in downloads) {
                if (filter(download)) {
                    download.cancel()
                }
            }
        }
    }

    private suspend fun performDownload(download: Download): Result<PlatformFile?> = withContext(Dispatchers.IO) {
        val format: YoutubeVideoFormat = getSongAudioFormatByQuality(download.song.id, download.quality, context).fold(
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
            return@withContext Result.failure(RuntimeException("Connection to '${connection.url}' with bytes=${download.downloaded} failed", e))
        }

        if (connection.responseCode != 200 && connection.responseCode != 206) {
            return@withContext Result.failure(
                ConnectException("Connection to '${connection.url}' with bytes=${download.downloaded} failed ${connection.responseCode} ${connection.responseMessage}")
            )
        }

        val format_extension: String =
            when (connection.contentType) {
                "audio/webm" -> "webm"
                "audio/mp4" -> "mp4"
                else -> return@withContext Result.failure(NotImplementedError(connection.contentType))
            }

        var file: PlatformFile? = download.song_file
        if (file == null) {
            file = download.getDownloadFile(format_extension)
        }

        val data: ByteArray = ByteArray(4096)
        val input_stream: InputStream = connection.inputStream
        val output_stream: BufferedSink = file.outputStream(true).buffer()

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
                    if (download.custom_uri == null) {
                        LocalSongMetadataProcessor.addMetadataToLocalSong(download.song, file, format_extension, context)
                    }
                }
                launch {
                    if (download.lyrics_file == null && download.download_lyrics) {
                        val lyrics_file: PlatformFile = MediaItemLibrary.getLocalLyricsFile(download.song, context) ?: return@launch
                        SongLyricsLoader.loadBySong(download.song, context)?.onSuccess { lyrics ->
                            with (LyricsFileConverter) {
                                val exception: Throwable? = lyrics.saveToFile(lyrics_file, context).exceptionOrNull()
                                exception?.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
        catch (e: Throwable) {
            e.printStackTrace()
            close(DownloadStatus.Status.CANCELLED)
            return@withContext Result.failure(e)
        }

        try {
            close(DownloadStatus.Status.FINISHED)

            val destination_file: PlatformFile = download.getDestinationFile(format_extension)
            file.moveTo(destination_file)
            download.song_file = destination_file

            return@withContext Result.success(download.song_file)
        }
        catch (e: Throwable) {
            e.printStackTrace()
            download.status = DownloadStatus.Status.CANCELLED
            return@withContext Result.failure(e)
        }
    }
}