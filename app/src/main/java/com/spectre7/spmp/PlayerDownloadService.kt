package com.spectre7.spmp

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.spectre7.spmp.model.Song
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

    data class Download(
        val id: String
    ) {
        enum class Status { IDLE, DOWNLOADING, CANCELLED, ALREADY_FINISHED, FINISHED }
        var status: Status = Status.IDLE
        val finished: Boolean get() = status == Status.ALREADY_FINISHED || status == Status.FINISHED

        var cancelled: Boolean = false
            private set

        val song: Song get() = Song.fromId(id)
        var downloaded: Long = 0
        var total_size: Long = -1

        val progress: Float get() = if (total_size < 0f) 0f else downloaded.toFloat() / total_size
        val percent_progress: Int get() = (progress * 100).toInt()

        fun cancel() {
            cancelled = true
        }

        fun broadcastResult(context: Context, result: Result<File?>?, instance: Int) {
            val result_intent = Intent(RESULT_INTENT_ACTION)
            result_intent.putExtra("action", ResultIntentAction.DOWNLOAD_RESULT)
            result_intent.putExtra("song_id", id)
            result_intent.putExtra("status", status)
            result_intent.putExtra("result", result ?: Result.success(null))
            result_intent.putExtra("instance", instance)
            LocalBroadcastManager.getInstance(context).sendBroadcast(result_intent)
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
        STOP, START_DOWNLOAD, CANCEL_DOWNLOAD
    }

    enum class ResultIntentAction {
        DOWNLOAD_RESULT
    }

    companion object {
        const val RESULT_INTENT_ACTION: String = "com.spectre7.spmp.PlayerDownloadService.result"
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

    private val broadcast_receiver = object : BroadcastReceiver() {
        override fun onReceive(_context: Context, intent: Intent) {
            val action = intent.extras?.get("action")
            if (action is IntentAction) {
                onActionIntentReceived(action, intent)
            }
        }
    }
    private lateinit var notification_delete_intent: PendingIntent

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
        }
    }

    private fun startDownload(intent: Intent) {
        val download = Download(intent.getStringExtra("song_id")!!)
        val instance = intent.extras!!.get("instance") as Int

        synchronized(downloads) {
            for (dl in downloads) {
                if (dl.id == download.id) {
                    dl.broadcastResult(this, null, instance)
                    return
                }
            }

            if (downloads.isEmpty()) {
                startForeground(NOTIFICATION_ID, notification_builder.build())
                start_time = System.currentTimeMillis()
                completed_downloads = 0
                failed_downloads = 0
            }

            downloads.add(download)
        }

        onDownloadProgress()

        executor.submit {
            val result: Result<File?> = try {
                performDownload(download)
            }
            catch (e: Exception) { Result.failure(e) }

            synchronized(downloads) {
                downloads.removeIf { it.id == download.id }
                if (downloads.isEmpty()) {
                    stopForeground(false)
                }

                if (result.isFailure) {
                    failed_downloads += 1
                }
                else {
                    completed_downloads += 1
                }

                download.broadcastResult(this, result, instance)
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

    private fun performDownload(download: Download): Result<File?> {
        var file: File? = null

        if (download_dir.exists()) {
            for (f in download_dir.listFiles()!!) {
                val split = f.name.split('.', limit = 2)
                if (split.first() != download.id) {
                    continue
                }

                // Download partially completed
                if (split.last().endsWith(FILE_DOWNLOADING_SUFFIX)) {
                    download.downloaded = f.length()
                    file = f
                    break
                }
                // Download fully completed
                else {
                    download.status = Download.Status.ALREADY_FINISHED
                    return Result.success(f)
                }
            }
        }
        else {
            download_dir.mkdirs()
        }

        val connection = URL(download.song.getStreamUrl()).openConnection() as HttpURLConnection
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
            file = download_dir.resolve("${download.id}.$extension$FILE_DOWNLOADING_SUFFIX")
        }

        assert(file.name.endsWith(FILE_DOWNLOADING_SUFFIX))

        val data = ByteArray(4096)
        val output = FileOutputStream(file, true)
        val input = connection.inputStream

        fun close(status: Download.Status) {
            input.close()
            output.close()
            connection.disconnect()
            download.status = status
        }

        download.total_size = connection.contentLengthLong + download.downloaded
        download.status = Download.Status.DOWNLOADING

        while (true) {
            val size = input.read(data)
            if (size < 0) {
                break
            }

            synchronized(executor) {
                if (stopping || download.cancelled) {
                    close(Download.Status.CANCELLED)
                    return Result.success(null)
                }
            }

            download.downloaded += size
            output.write(data, 0, size)

            onDownloadProgress()
        }

        close(Download.Status.FINISHED)
        file.renameTo(file.resolveSibling(file.name.dropLast(FILE_DOWNLOADING_SUFFIX.length)))

        download.status = Download.Status.FINISHED
        return Result.success(file)
    }

    private fun getNotificationText(): String {
        var text = ""
        var additional = ""

        synchronized(downloads) {
            var downloading = 0
            for (dl in downloads) {
                if (dl.status != Download.Status.DOWNLOADING) {
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
        notification_builder.setDeleteIntent(notification_delete_intent)

        if (downloads.isEmpty() || total_progress == 1f) {
            notification_builder.setProgress(0, 0, false);
            notification_builder.setOngoing(false)

            if (completed_downloads == 0) {
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
            notification_builder.setOngoing(true)

            notification_builder.setContentTitle(
                if (downloads.size == 1) {
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
            )

            notification_builder.setContentText(getNotificationText())
            notification_builder.setSmallIcon(android.R.drawable.stat_sys_download)

            val elapsed_minutes = ((System.currentTimeMillis() - start_time) / 60000f).toInt()
            notification_builder.setSubText(when(elapsed_minutes) {
                0 -> "Just started"
                1 -> "Started 1 min ago"
                else -> "Started $elapsed_minutes mins ago"
            })
        }

        NotificationManagerCompat.from(this).notify(
            NOTIFICATION_ID, notification_builder.build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.extras?.get("action")
        if (action is IntentAction) {
            onActionIntentReceived(action, intent)
            return super.onStartCommand(intent, flags, startId)
        }

        notification_delete_intent = PendingIntent.getService(
            this,
            69,
            Intent(this, PlayerDownloadService::class.java).putExtra("action", IntentAction.STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        notification_manager = NotificationManagerCompat.from(this)
        notification_builder = getNotificationBuilder()

        startForeground(NOTIFICATION_ID, notification_builder.build())

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotificationBuilder(): Notification.Builder {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, getNotificationChannel())
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .setDeleteIntent(notification_delete_intent)
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