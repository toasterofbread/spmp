package com.toasterofbread.spmp.platform.playerservice

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.shared.R
import dev.toastbits.composekit.platform.isAppInForeground
import dev.toastbits.composekit.utils.common.launchSingle
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class PlayerServiceNotificationManager(
    private val context: AppContext,
    private val media_session: MediaSession,
    private val notification_manager: NotificationManager,
    private val service: ForegroundPlayerService
) {
    private var current_song: Song? = null
    private var current_song_thumbnail: Bitmap? = null
    private val thumbnail_load_scope: CoroutineScope = CoroutineScope(Job())
    private var notification_started: Boolean = false

    private val default_image by lazy { createImage(1, 1, Color.BLACK) }

    class NotificationDismissReceiver : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(context: Context, intent: Intent) {
            if (context.isAppInForeground()) {
                return
            }

            val manager: ActivityManager = context.getSystemService() ?: return
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                context.startService(
                    Intent()
                        .setComponent(service.service)
                        .setAction(ForegroundPlayerService.INTENT_SERVICE_STOP)
                )
            }
        }
    }

    private val player_listener: PlayerListener =
        object : PlayerListener() {
            override fun onSongTransition(song: Song?, manual: Boolean) {
                synchronized(this@PlayerServiceNotificationManager) {
                    if (song == current_song) {
                        return
                    }

                    current_song = song
                    current_song_thumbnail = null

                    updateNotification()
                }
            }
        }

    init {
        service.addListener(player_listener)
        updateNotification()
    }

    fun release() {
        service.removeListener(player_listener)
        thumbnail_load_scope.cancel()
    }

    private fun updateNotification() {
        ensureNotificationChannel()

        val notification: Notification = createNotification() ?: return
        if (notification_started) {
            notification_manager.notify(NOTIFICATION_ID, notification)
        }
        else {
            service.startForeground(NOTIFICATION_ID, notification)
            notification_started = true
        }
    }

    private fun createImage(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        val paint: Paint = Paint()
        paint.setColor(color)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private val content_intent: PendingIntent =
        PendingIntent.getActivity(
            context.ctx, 0,
            Intent(context.ctx, AppContext.main_activity),
            PendingIntent.FLAG_IMMUTABLE
        )

    private val delete_intent: PendingIntent =
        PendingIntent.getBroadcast(
            context.ctx, 1,
            Intent(context.ctx, NotificationDismissReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

    private suspend fun loadThumbnail(song: Song) {
        for (quality in ThumbnailProvider.Quality.byQuality()) {
            val image: ImageBitmap =
                MediaItemThumbnailLoader.loadItemThumbnail(song, quality, context).getOrNull()
                ?: continue

            val formatted_image: Bitmap =
                formatMediaNotificationImage(
                    image.asAndroidBitmap(),
                    song,
                    context
                )

            synchronized(this@PlayerServiceNotificationManager) {
                if (current_song == song) {
                    context.onNotificationThumbnailLoaded(formatted_image)
                    current_song_thumbnail = formatted_image
                    updateNotification()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun createNotification(): Notification? = synchronized(this) {
        val song: Song = current_song ?: return null

        if (current_song_thumbnail == null) {
            thumbnail_load_scope.launchSingle {
                loadThumbnail(song)
            }
        }

        return Notification.Builder(
            context.ctx.applicationContext,
            NOTIFICATION_CHANNEL_ID
        )
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.ic_spmp)
            .setLargeIcon(current_song_thumbnail ?: default_image)
            .setOngoing(false)
            .setContentIntent(content_intent)
            .setDeleteIntent(delete_intent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(media_session.platformToken)
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (notification_manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return
        }

        notification_manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Playing media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
        )
    }

    companion object {
        const val NOTIFICATION_ID: Int = 10
        const val NOTIFICATION_CHANNEL_ID: String = "media_channel_id"
    }
}
