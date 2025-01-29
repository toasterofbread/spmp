package com.toasterofbread.spmp.platform.download

import SpMp
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.drawable.IconCompat
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlatformBinder
import com.toasterofbread.spmp.platform.PlatformServiceImpl
import com.toasterofbread.spmp.platform.getUiLanguage
import dev.toastbits.composekit.context.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_cancel
import spmp.shared.generated.resources.action_download_pause
import spmp.shared.generated.resources.action_download_resume
import spmp.shared.generated.resources.download_just_started
import spmp.shared.generated.resources.download_service_name
import spmp.shared.generated.resources.`download_started_$x_minutes_ago`
import spmp.shared.generated.resources.`downloading_$x_songs`
import spmp.shared.generated.resources.`downloading_song_$title`
import java.util.concurrent.Executors

private const val NOTIFICATION_ID = 1
private const val NOTIFICATION_CHANNEL_ID = "download_channel"

class PlayerDownloadService: PlatformServiceImpl() {
    internal inner class SongDownloaderImpl: SongDownloader(
        context,
        Executors.newFixedThreadPool(3)
    ) {
        override fun getAudioFileDurationMs(file: PlatformFile): Long? {
            val metadata_retriever: MediaMetadataRetriever = MediaMetadataRetriever()
            metadata_retriever.setDataSource(context.ctx, Uri.parse(file.uri))
            return metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        }

        override fun onDownloadStatusChanged(download: Download, started: Boolean) {
            sendMessageOut(
                PlayerDownloadManager.PlayerDownloadMessage(
                    IntentAction.STATUS_CHANGED,
                    mapOf("status" to download.getStatusObject(), "started" to started)
                )
            )
        }

        override fun onPausedChanged() {
            context.coroutineScope.launch {
                pause_resume_action?.title =
                    if (paused) getString(Res.string.action_download_resume)
                    else getString(Res.string.action_download_pause)
            }
        }

        override suspend fun onFirstDownloadStarting(download: Download) {
            if (!download.silent) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(context.ctx, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED
                ) {
                    context.sendToast("(BUG) No notification permission")
                    return
                }

                notification_builder = getNotificationBuilder()

                ServiceCompat.startForeground(this@PlayerDownloadService, NOTIFICATION_ID, notification_builder!!.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }
        }

        override fun onLastDownloadFinished() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
            else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        }

        override fun onDownloadProgress() {
            context.coroutineScope.launch {
                updateNotification()
            }
        }

        @SuppressLint("MissingPermission")
        suspend fun updateNotification() = withContext(Dispatchers.IO) {
            withDownloads {
                if (downloads.isNotEmpty() && downloads.all { it.silent }) {
                    return@withContext
                }

                notification_builder?.also { builder ->
                    notification_update_time = System.currentTimeMillis()
                    val total_progress: Float = getTotalDownloadProgress()

                    if (!downloads.any { !it.silent }) {
                        builder.setProgress(0, 0, false)
                        builder.setOngoing(false)
                        builder.setDeleteIntent(notification_delete_intent)

                        if (cancelled) {
                            builder.setContentTitle("Download cancelled")
                            builder.setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                        }
                        else if (completed_downloads == 0) {
                            builder.setContentTitle("Download failed")
                            builder.setSmallIcon(android.R.drawable.stat_notify_error)
                        }
                        else {
                            NotificationManagerCompat.from(context.ctx).cancel(NOTIFICATION_ID)
                            return@withContext
                        }

                        builder.setContentText("")
                    }
                    else {
                        builder.setProgress(100, (total_progress * 100).toInt(), false)

                        var title: String? = null
                        if (downloads.size == 1) {
                            val song_title = downloads.first().song.getActiveTitle(context.database)
                            if (song_title != null) {
                                title = getString(Res.string.`downloading_song_$title`).replace("\$title", song_title)
                            }
                        }

                        if (title == null) {
                            title = getString(Res.string.`downloading_$x_songs`).replace("\$x", downloads.size.toString())
                        }

                        builder.setContentTitle(if (paused) "$title (paused)" else title)
                        builder.setContentText(getNotificationText())

                        val elapsed_minutes = ((System.currentTimeMillis() - start_time_ms) / 60000f).toInt()
                        builder.setSubText(
                            if (elapsed_minutes == 0) getString(Res.string.download_just_started)
                            else getString(Res.string.`download_started_$x_minutes_ago`).replace("\$x", elapsed_minutes.toString())
                        )
                    }

                    if (ActivityCompat.checkSelfPermission(context.ctx, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) {
                        try {
                            NotificationManagerCompat.from(context.ctx).notify(
                                NOTIFICATION_ID, builder.build().apply {
                                    if (downloads.isEmpty() || total_progress == 1f) {
                                        actions = arrayOf<Notification.Action>()
                                    }
                                }
                            )
                        }
                        catch (_: Throwable) {}
                    }
                }
            }
        }
    }

    internal var downloader: SongDownloaderImpl? = null

    enum class IntentAction {
        STOP, START_DOWNLOAD, CANCEL_DOWNLOAD, CANCEL_ALL, PAUSE_RESUME, STATUS_CHANGED
    }

    private var notification_builder: NotificationCompat.Builder? = null
    private lateinit var notification_manager: NotificationManagerCompat
    private var notification_update_time: Long = -1
    private lateinit var notification_delete_intent: PendingIntent
    private var pause_resume_action: NotificationCompat.Action? = null

    override fun onCreate() {
        super.onCreate()

        if (downloader == null) {
            downloader = SongDownloaderImpl()
        }

        downloader?.downloads?.also { downloads ->
            synchronized(downloads) {
                downloads.clear()
            }
        }

        notification_manager = NotificationManagerCompat.from(this)
        notification_delete_intent = PendingIntent.getService(
            this,
            IntentAction.STOP.ordinal,
            Intent(this, PlayerDownloadService::class.java).putExtra("action", IntentAction.STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
    }

    override fun onDestroy() {
        downloader?.release()
        downloader = null
    }

    override fun onMessage(data: Any?) {
        require(data is PlayerDownloadManager.PlayerDownloadMessage)
        context.coroutineScope.launch {
            onActionIntentReceived(data)
        }
    }

    private suspend fun onActionIntentReceived(message: PlayerDownloadManager.PlayerDownloadMessage) {
        when (message.action) {
            IntentAction.STOP -> {
                println("Download service stopping...")
                downloader?.stop()
                stopSelf()
            }
            IntentAction.START_DOWNLOAD -> startDownload(message)
            IntentAction.CANCEL_DOWNLOAD -> cancelDownload(message)
            IntentAction.CANCEL_ALL -> cancelAllDownloads(message)
            IntentAction.PAUSE_RESUME -> {
                downloader?.also { downloader ->
                    downloader.paused = !downloader.paused
                    downloader.updateNotification()
                }
            }
            IntentAction.STATUS_CHANGED -> throw IllegalStateException("STATUS_CHANGED is for output only")
        }
    }

    private suspend fun startDownload(message: PlayerDownloadManager.PlayerDownloadMessage) {
        require(message.instance != null)

        downloader?.startDownload(
            SongRef(message.data["song_id"] as String),
            silent = message.data["silent"] as Boolean,
            custom_uri = message.data["custom_uri"] as String?,
            download_lyrics = (message.data["download_lyrics"] as Boolean?) ?: true,
            direct = (message.data["direct"] as Boolean?) ?: false,
        ) { download, result ->
            sendMessageOut(
                PlayerDownloadManager.PlayerDownloadMessage(
                    IntentAction.START_DOWNLOAD,
                    mapOf(
                        "status" to download.getStatusObject(),
                        "result" to result
                    ),
                    message.instance
                )
            )
        }
    }

    private suspend fun cancelDownload(message: PlayerDownloadManager.PlayerDownloadMessage) {
        val id: String = message.data["id"] as String
        downloader?.cancelDownloads { download ->
            download.song.id == id
        }
    }

    private suspend fun cancelAllDownloads(message: PlayerDownloadManager.PlayerDownloadMessage) {
        downloader?.cancelDownloads { true }
    }

    private suspend fun getNotificationText(): String {
        val downloader: SongDownloaderImpl = downloader ?: return ""

        var text: String = ""
        var additional: String = ""

        downloader.withDownloads { downloads ->
            var downloading = 0
            for (download in downloads) {
                if (download.status != DownloadStatus.Status.DOWNLOADING && download.status != DownloadStatus.Status.PAUSED) {
                    continue
                }

                if (text.isNotEmpty()) {
                    text += ", ${download.percent_progress}%"
                }
                else {
                    text += "${download.percent_progress}%"
                }

                downloading += 1
            }

            if (downloading < downloads.size) {
                additional += "${downloads.size - downloading} queued"
            }
            if (downloader.completed_downloads > 0) {
                if (additional.isNotEmpty()) {
                    additional += ", ${downloader.completed_downloads} finished"
                }
                else {
                    additional += "${downloader.completed_downloads} finished"
                }
            }
            if (downloader.failed_downloads > 0) {
                if (additional.isNotEmpty()) {
                    additional += ", ${downloader.failed_downloads} failed"
                }
                else {
                    additional += "${downloader.failed_downloads} failed"
                }
            }
        }

        return if (additional.isEmpty()) text else "$text ($additional)"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action: Any? = intent?.extras?.get("action")
        if (action is IntentAction) {
            println("Download service received action $action")
            context.coroutineScope.launch {
                onActionIntentReceived(
                    PlayerDownloadManager.PlayerDownloadMessage(
                        action,
                        emptyMap()
                    )
                )
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun getNotificationBuilder(): NotificationCompat.Builder {
        val content_intent: PendingIntent = PendingIntent.getActivity(
            this, 0,
            AppContext.getMainActivityIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )

        if (pause_resume_action == null) {
            pause_resume_action =
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(this@PlayerDownloadService, android.R.drawable.ic_menu_close_clear_cancel),
                    org.jetbrains.compose.resources.getString(Res.string.action_download_pause),
                    PendingIntent.getService(
                        this@PlayerDownloadService,
                        IntentAction.PAUSE_RESUME.ordinal,
                        Intent(this@PlayerDownloadService, PlayerDownloadService::class.java).putExtra("action", IntentAction.PAUSE_RESUME),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
        }

        return NotificationCompat.Builder(this, getNotificationChannel())
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(content_intent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .addAction(pause_resume_action)
            .addAction(
                NotificationCompat.Action.Builder(
                    IconCompat.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    getString(Res.string.action_cancel),
                    PendingIntent.getService(
                        this,
                        IntentAction.CANCEL_ALL.ordinal,
                        Intent(this, PlayerDownloadService::class.java).putExtra("action", IntentAction.CANCEL_ALL),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    foregroundServiceBehavior = Notification.FOREGROUND_SERVICE_IMMEDIATE
                }
            }
    }

    private suspend fun getNotificationChannel(): String {
        val channel =
            NotificationChannelCompat.Builder(
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
            )
            .setName(getString(Res.string.download_service_name))
            .setSound(null, null)
            .build()

        notification_manager.createNotificationChannel(channel)
        return NOTIFICATION_CHANNEL_ID
    }

    inner class ServiceBinder: PlatformBinder() {
        fun getService(): PlayerDownloadService = this@PlayerDownloadService
    }
    private val binder = ServiceBinder()
    override fun onBind() = binder
}