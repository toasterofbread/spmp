package com.spectre7.spmp

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Binder
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spectre7.spmp.api.YoutubeVideoFormat
import com.spectre7.spmp.api.cast
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

private const val FILE_DOWNLOADING_SUFFIX = ".part"
private const val NOTIFICATION_ID = 1
private const val NOTIFICATION_CHANNEL_ID = "download_channel"

class PlayerDownloadService: Service() {
    enum class DownloadStatus { IDLE, PAUSED, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }

    inner class Download(
        val id: String,
        val quality: Song.AudioQuality
    ) {
        var status: DownloadStatus = DownloadStatus.IDLE
            set(value) {
                if (field != value) {
                    field = value
                    broadcastStatus(this@PlayerDownloadService)
                }
            }
        val song: Song get() = Song.fromId(id)

        val finished: Boolean get() = status == DownloadStatus.ALREADY_FINISHED || status == DownloadStatus.FINISHED
        val downloading: Boolean get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED

        var cancelled: Boolean = false
            private set

        var downloaded: Long = 0
        var total_size: Long = -1
        var file: File? = null

        val progress: Float get() = if (total_size < 0f) 0f else downloaded.toFloat() / total_size
        val percent_progress: Int get() = (progress * 100).toInt()

        init {
            val files = download_dir.listFiles()
            if (files != null) {
                for (f in files) {
                    if (matchesFile(f) == true) {
                        status = DownloadStatus.ALREADY_FINISHED
                        file = f
                        break
                    }
                }
            }
        }

        fun cancel() {
            cancelled = true
        }

        fun matchesFile(file: File): Boolean? {
            return fileMatchesDownload(file.name, id, quality)
        }

        fun generatePath(extension: String, in_progress: Boolean): String {
            return getDownloadPath(id, quality, extension, in_progress)
        }

        fun broadcastResult(context: Context, result: Result<File?>?, instance: Int) {
            val result_intent = Intent(RESULT_INTENT_ACTION)
            result_intent.putExtra("action", IntentAction.START_DOWNLOAD)
            result_intent.putExtra("song_id", id)
            result_intent.putExtra("status", status)
            result_intent.putExtra("result", result ?: Result.success(null))
            result_intent.putExtra("instance", instance)
            LocalBroadcastManager.getInstance(context).sendBroadcast(result_intent)
        }

        private fun broadcastStatus(context: Context) {
            val result_intent = Intent(RESULT_INTENT_ACTION)
            result_intent.putExtra("action", IntentAction.STATUS_CHANGED)
            result_intent.putExtra("song_id", id)
            result_intent.putExtra("status", status)
            result_intent.putExtra("file", file)
            LocalBroadcastManager.getInstance(context).sendBroadcast(result_intent)
        }
    }

    private fun getDownload(song_id: String): Download {
        val quality: Song.AudioQuality = Settings.getEnum(Settings.KEY_DOWNLOAD_AUDIO_QUALITY)
        synchronized(downloads) {
            for (download in downloads) {
                if (download.id == song_id && download.quality == quality) {
                    return download
                }
            }
            return Download(song_id, quality)
        }
    }

    private fun Collection<Download>.getTotalProgress(): Float {
        if (isEmpty()) {
            return 1f
        }

        var total_progress = 0f
        for (download in this) {
            total_progress += download.progress
        }
        return total_progress / size
    }

    enum class IntentAction {
        STOP, START_DOWNLOAD, CANCEL_DOWNLOAD, CANCEL_ALL, PAUSE_RESUME, STATUS_CHANGED
    }

    companion object {
        const val RESULT_INTENT_ACTION: String = "com.spectre7.spmp.PlayerDownloadService.result"

        // Filename format: id.quality.mediatype(.part)
        // Return values: true = match, false = match (partial file), null = no match
        fun fileMatchesDownload(filename: String, id: String, quality: Song.AudioQuality): Boolean? {
            if (!filename.startsWith("$id.${quality.ordinal}.")) {
                return null
            }
            return !filename.endsWith(FILE_DOWNLOADING_SUFFIX)
        }

        fun getDownloadPath(id: String, quality: Song.AudioQuality, extension: String, in_progress: Boolean): String {
            return "$id.${quality.ordinal}.$extension${ if (in_progress) FILE_DOWNLOADING_SUFFIX else ""}"
        }
    }

    private lateinit var notification_builder: Notification.Builder
    private lateinit var notification_manager: NotificationManagerCompat

    private val download_dir: File get() = PlayerDownloadManager.getDownloadDir(this)
    private val downloads: MutableList<Download> = mutableListOf()
    private val executor = Executors.newFixedThreadPool(3)
    private var stopping = false

    private var start_time: Long = 0
    private var completed_downloads: Int = 0
    private var failed_downloads: Int = 0
    private var cancelled: Boolean = false
    private var notification_update_time: Long = -1

    private var paused: Boolean = false
        set(value) {
            field = value
            pause_resume_action.title = if (value) "Resume" else "Pause"
        }

    private val broadcast_receiver = object : BroadcastReceiver() {
        override fun onReceive(_context: Context, intent: Intent) {
            val action = intent.extras?.get("action")
            if (action is IntentAction) {
                onActionIntentReceived(action, intent)
            }
        }
    }
    private lateinit var notification_delete_intent: PendingIntent
    private lateinit var pause_resume_action: Notification.Action

    private fun onActionIntentReceived(action: IntentAction, intent: Intent) {
        when (action) {
            IntentAction.STOP -> {
                println("Download service stopping...")
                synchronized(executor) {
                    stopping = true
                }
                stopSelf()
            }
            IntentAction.START_DOWNLOAD -> startDownload(intent)
            IntentAction.CANCEL_DOWNLOAD -> cancelDownload(intent)
            IntentAction.CANCEL_ALL -> cancelAllDownloads(intent)
            IntentAction.PAUSE_RESUME -> {
                paused = !paused
                onDownloadProgress()
            }
            IntentAction.STATUS_CHANGED -> throw IllegalStateException("STATUS_CHANGED is for output only")
        }
    }

    fun getDownloadStatus(song_id: String): DownloadStatus {
        return getDownload(song_id).status
    }

    fun getDownloadProgress(song_id: String): Float {
        return getDownload(song_id).progress
    }

    private fun startDownload(intent: Intent) {
        val instance = intent.extras!!.get("instance") as Int
        val download = getDownload(intent.getStringExtra("song_id")!!)

        synchronized(download) {
            if (download.finished) {
                download.broadcastResult(this, Result.success(download.file), instance)
                return
            }

            if (download.downloading) {
                if (paused) {
                    paused = false
                }
                download.broadcastResult(this, null, instance)
                return
            }

            synchronized(downloads) {
                if (downloads.isEmpty()) {
                    notification_builder = getNotificationBuilder()
                    startForeground(NOTIFICATION_ID, notification_builder.build())
                    start_time = System.currentTimeMillis()
                    completed_downloads = 0
                    failed_downloads = 0
                    cancelled = false
                }

                downloads.add(download)
            }
        }

        onDownloadProgress()

        executor.submit {
            runBlocking {
                var result: Result<File?>? = null
                while (result == null || download.status == DownloadStatus.IDLE || download.status == DownloadStatus.PAUSED) {
                    if (paused && !download.cancelled) {
                        onDownloadProgress()
                        delay(1000)
                        continue
                    }

                    result = try {
                        performDownload(download)
                    }
                    catch (e: Exception) {
                        Result.failure(e)
                    }
                }

                synchronized(downloads) {
                    downloads.removeIf { it.id == download.id }
                    if (downloads.isEmpty()) {
                        cancelled = download.cancelled
                        stopForeground(false)
                    }

                    if (result.isFailure) {
                        failed_downloads += 1
                    }
                    else {
                        completed_downloads += 1
                    }
                }

                download.broadcastResult(this@PlayerDownloadService, result, instance)

                if (notification_update_time > 0) {
                    val delay_duration = 1000 - (System.currentTimeMillis() - notification_update_time)
                    if (delay_duration > 0) {
                        delay(delay_duration)
                    }
                }
                onDownloadProgress()
            }
        }
    }

    private fun cancelDownload(intent: Intent) {
        val id = intent.getStringExtra("id")!!
        synchronized(downloads) {
            downloads.firstOrNull { it.id == id }?.cancel()
        }
    }

    private fun cancelAllDownloads(_intent: Intent) {
        synchronized(downloads) {
            downloads.forEach { it.cancel() }
        }
    }

    private fun performDownload(download: Download): Result<File?> {
        var file: File? = null

        if (download_dir.exists()) {
            for (f in download_dir.listFiles()!!) {
                when (download.matchesFile(f)) {
                    true -> {
                        // Download fully completed
                        download.status = DownloadStatus.ALREADY_FINISHED
                        return Result.success(f)
                    }
                    false -> {
                        // Download partially completed
                        download.downloaded = f.length()
                        file = f
                        break
                    }
                    null -> {}
                }
            }
        }
        else {
            download_dir.mkdirs()
        }

        val format = download.song.getFormatByQuality(download.quality)
        if (format.isFailure) {
            return format.cast()
        }

        val connection = URL(format.getOrThrow().stream_url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Range", "bytes=${download.downloaded}-")
        connection.connect()

        if (connection.responseCode != 200 && connection.responseCode != 206) {
            return Result.failure(ConnectException(
                "${download.id}: Server returned code ${connection.responseCode} ${connection.responseMessage}"
            ))
        }

        if (file == null) {
            val extension = when (connection.contentType) {
                "audio/webm" -> "webm"
                else -> return Result.failure(NotImplementedError(connection.contentType))
            }
            file = download_dir.resolve(download.generatePath(extension, true))
        }

        check(file.name.endsWith(FILE_DOWNLOADING_SUFFIX))

        val data = ByteArray(4096)
        val output = FileOutputStream(file, true)
        val input = connection.inputStream

        fun close(status: DownloadStatus) {
            input.close()
            output.close()
            connection.disconnect()
            download.status = status
        }

        download.total_size = connection.contentLengthLong + download.downloaded
        download.status = DownloadStatus.DOWNLOADING

        while (true) {
            val size = input.read(data)
            if (size < 0) {
                break
            }

            synchronized(executor) {
                if (stopping || download.cancelled) {
                    close(DownloadStatus.CANCELLED)
                    return Result.success(null)
                }
                if (paused) {
                    close(DownloadStatus.PAUSED)
                    return Result.success(null)
                }
            }

            download.downloaded += size
            output.write(data, 0, size)

            onDownloadProgress()
        }

        close(DownloadStatus.FINISHED)
        file.renameTo(file.resolveSibling(file.name.dropLast(FILE_DOWNLOADING_SUFFIX.length)))

        download.status = DownloadStatus.FINISHED
        return Result.success(file)
    }

    private fun getNotificationText(): String {
        var text = ""
        var additional = ""

        synchronized(downloads) {
            var downloading = 0
            for (dl in downloads) {
                if (dl.status != DownloadStatus.DOWNLOADING && dl.status != DownloadStatus.PAUSED) {
                    continue
                }

                if (text.isNotEmpty()) {
                    text += ", ${dl.percent_progress}%"
                }
                else {
                    text += "${dl.percent_progress}%"
                }

                downloading += 1
            }

            if (downloading < downloads.size) {
                additional += "${downloads.size - downloading} queued"
            }
            if (completed_downloads > 0) {
                if (additional.isNotEmpty()) {
                    additional += ", $completed_downloads finished"
                }
                else {
                    additional += "$completed_downloads finished"
                }
            }
            if (failed_downloads > 0) {
                if (additional.isNotEmpty()) {
                    additional += ", $failed_downloads failed"
                }
                else {
                    additional += "$failed_downloads failed"
                }
            }
        }

        return if (additional.isEmpty()) text else "$text ($additional)"
    }

    private fun onDownloadProgress() {
        val total_progress = downloads.getTotalProgress()

        if (downloads.isEmpty()) {
            notification_builder.setProgress(0, 0, false);
            notification_builder.setOngoing(false)
            notification_builder.setDeleteIntent(notification_delete_intent)

            if (cancelled) {
                notification_builder.setContentTitle("Download cancelled")
                notification_builder.setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            }
            else if (completed_downloads == 0) {
                notification_builder.setContentTitle("Download failed")
                notification_builder.setSmallIcon(android.R.drawable.stat_notify_error)
            }
            else {
                notification_builder.setContentTitle("Download completed")
                notification_builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
            }

            notification_builder.setContentText("")
        }
        else {
            notification_builder.setProgress(100, (total_progress * 100).toInt(), false)

            val title = if (downloads.size == 1) {
                if (downloads.first().song.title != null) {
                    "Downloading ${downloads.first().song.title}"
                }
                else {
                    "Downloading 1 song"
                }
            }
            else {
                "Downloading ${downloads.size} songs"
            }

            notification_builder.setContentTitle(if (paused) "$title (paused)" else title)
            notification_builder.setContentText(getNotificationText())

            val elapsed_minutes = ((System.currentTimeMillis() - start_time) / 60000f).toInt()
            notification_builder.setSubText(when(elapsed_minutes) {
                0 -> "Just started"
                1 -> "Started 1 min ago"
                else -> "Started $elapsed_minutes mins ago"
            })
        }

        NotificationManagerCompat.from(this).notify(
            NOTIFICATION_ID, notification_builder.build().apply {
                if (downloads.isEmpty() || total_progress == 1f) {
                    actions = arrayOf<Notification.Action>()
                }
            }
        )
        notification_update_time = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.extras?.get("action")
        if (action is IntentAction) {
            onActionIntentReceived(action, intent)
            return super.onStartCommand(intent, flags, startId)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotificationBuilder(): Notification.Builder {
        val content_intent: PendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, getNotificationChannel())
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(content_intent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(pause_resume_action)
            .addAction(Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                "Cancel",
                PendingIntent.getService(
                    this,
                    IntentAction.CANCEL_ALL.ordinal,
                    Intent(this, PlayerDownloadService::class.java).putExtra("action", IntentAction.CANCEL_ALL),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ).build())
    }

    private fun getNotificationChannel(): String {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.download_service_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setSound(null, null)

        notification_manager.createNotificationChannel(channel)
        return NOTIFICATION_CHANNEL_ID
    }

    override fun onCreate() {
        super.onCreate()

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast_receiver, IntentFilter(PlayerDownloadService::class.java.canonicalName))
        notification_manager = NotificationManagerCompat.from(this)

        notification_delete_intent = PendingIntent.getService(
            this,
            IntentAction.STOP.ordinal,
            Intent(this, PlayerDownloadService::class.java).putExtra("action", IntentAction.STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        pause_resume_action = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Pause",
            PendingIntent.getService(
                this,
                IntentAction.PAUSE_RESUME.ordinal,
                Intent(this, PlayerDownloadService::class.java).putExtra("action", IntentAction.PAUSE_RESUME),
                PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast_receiver)
        executor.shutdownNow()
    }

    private val binder = ServiceBinder()
    inner class ServiceBinder: Binder() {
        fun getService(): PlayerDownloadService = this@PlayerDownloadService
    }
    override fun onBind(intent: Intent?): ServiceBinder {
        return binder
    }
}