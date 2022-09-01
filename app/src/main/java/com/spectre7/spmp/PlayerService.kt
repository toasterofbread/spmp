package com.spectre7.spmp

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.spectre7.spmp.model.Song
import kotlin.concurrent.thread

fun sendToast(text: String) {
    Toast.makeText(MainActivity.instance!!, text, Toast.LENGTH_SHORT).show()
}

class PlayerHost(private var context: Context) {

    private var service: PlayerService? = null
    private var service_bound: Boolean = false
    private var service_connection: ServiceConnection? = null
    private var service_intent: Intent? = null

    fun addListener(listener: Player.Listener) {
        interact {
            it.player.addListener(listener)
        }
    }

    fun removeListener(listener: Player.Listener) {
        interact {
            it.player.removeListener(listener)
        }
    }

    fun interact(action: (service: PlayerService) -> Unit) {
        if (service == null) {
            getService() {
                action(service!!)
            }
        }
        else {
            action(service!!)
        }
    }

    fun release() {
        if (service_connection != null) {
            context.unbindService(service_connection!!)
            service_connection = null
        }
    }

    private fun getService(on_connected: (() -> Unit)? = {}) {
        service_intent = Intent(context, PlayerService::class.java)
        if (!isServiceRunning()) {
            context.startForegroundService(service_intent)
        }

        context.bindService(service_intent,
            object : ServiceConnection {

                override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                    service = (binder as PlayerService.PlayerBinder).getService()
                    service_bound = true
                    on_connected?.invoke()
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    service_bound = false
                }

            }, 0)
    }

    fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
            if (PlayerService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    class PlayerService : Service() {

        val p_queue: MutableList<Song> = mutableListOf()
        var p_index: Int = 0

        internal lateinit var player: ExoPlayer
        private val NOTIFICATION_ID = 2
        private val NOTIFICATION_CHANNEL_ID = "playback_channel"
        private var playerNotificationManager: PlayerNotificationManager? = null

        private var media_session: MediaSessionCompat? = null
        private var media_session_connector: MediaSessionConnector? = null

        private val binder = PlayerBinder()
        inner class PlayerBinder: Binder() {
            fun getService(): PlayerService = this@PlayerService
        }
        override fun onBind(intent: Intent?): PlayerBinder {
            return binder
        }
        override fun onUnbind(intent: Intent?): Boolean {
            return super.onUnbind(intent)
        }

        override fun onCreate() {
            super.onCreate()
            player = ExoPlayer.Builder(MainActivity.instance!!.baseContext).build()
            player.prepare()
            media_session = MediaSessionCompat(MainActivity.instance!!, "spmp")
            media_session_connector = MediaSessionConnector(media_session!!)
            media_session_connector!!.setPlayer(player)
        }

        override fun onDestroy() {
            playerNotificationManager?.setPlayer(null)
            playerNotificationManager = null
            media_session?.release()
            player.release()

            super.onDestroy()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            addNotificationToPlayer()

            when (intent?.getIntExtra("action", -1)) {
                0 -> {
                    stopForeground(true)
                    stopSelf()

                    // TODO
                }
            }

            return START_NOT_STICKY
        }

        fun addToQueue(song: Song, i: Int = p_queue.size, onFinished: (() -> Unit)? = null) {
            var index = i
            if (index < 0 || index > p_queue.size) {
                index = p_queue.size
            }

            p_queue.add(index, song)
            song.getDownloadUrl {
                player.addMediaItem(index, MediaItem.Builder().setUri(it).setTag(song).build())
                onFinished?.invoke()
            }
        }

        fun replaceQueue(queue: List<Song>) {
            p_queue.clear()
            p_index = 0
            player.clearMediaItems()

            for ((i, song) in queue.withIndex()) {
                p_queue.add(song)
                song.getDownloadUrl() {
                    player.addMediaItem(i, MediaItem.Builder().setUri(it).setTag(song).build())
                }
            }
        }

        fun play(index: Int? = null): Boolean {

            if (p_queue.isEmpty()) {
                p_index = 0
                return false
            }

            if (index != null) {
                p_index = index
            }

            if (p_index < 0 || p_index >= p_queue.size) {
                p_index = p_queue.size - 1
            }

            player.prepare()
            player.play()

            return true
        }

        fun playPause(): Boolean {
            if (p_queue.isEmpty()) {
                p_index = -1
                return false
            }

            if (p_index < 0 || p_index >= p_queue.size) {
                p_index = 0
            }

            player.prepare()

            if (player.isPlaying) {
                player.pause()
            }
            else {
                player.play()
            }

            return true
        }

        private fun addNotificationToPlayer() {
            if (playerNotificationManager == null) {
                playerNotificationManager = PlayerNotificationManager.Builder(
                    MainActivity.instance!!.baseContext,
                    NOTIFICATION_ID,
                    getNotificationChannel(),
                    object : PlayerNotificationManager.MediaDescriptionAdapter {

                        override fun createCurrentContentIntent(player: Player): PendingIntent? {
                            return PendingIntent.getActivity(
                                MainActivity.instance!!,
                                1,
                                Intent(MainActivity.instance!!, MainActivity::class.java),
                                PendingIntent.FLAG_IMMUTABLE
                            )
                        }

                        override fun getCurrentContentText(player: Player): String? {
                            try {
                                val song = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration!!.tag as Song
                                return song.artist.nativeData.name
                            }
                            catch (e: IndexOutOfBoundsException) {
                                return null
                            }
                        }

                        override fun getCurrentContentTitle(player: Player): String {
                            try {
                                val song = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration!!.tag as Song
                                return song.nativeData?.title ?: "Unknown"
                            }
                            catch (e: IndexOutOfBoundsException) {
                                return "Unknown"
                            }
                        }

                        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                            try {
                                val song = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration!!.tag as Song
                                if (song.thumbnailLoaded(false)) {
                                    return song.loadThumbnail(false)
                                }

                                thread {
                                    callback.onBitmap(song.loadThumbnail(false))
                                }

                                return null                            }
                            catch (e: IndexOutOfBoundsException) {
                                return null
                            }
                        }

                    }
                ).setNotificationListener(
                    object : PlayerNotificationManager.NotificationListener {
                        override fun onNotificationPosted(notificationId: Int,
                                                          notification: Notification,
                                                          ongoing: Boolean) {
                            super.onNotificationPosted(notificationId, notification, ongoing)
//                            if (!ongoing) {
//                                stopForeground(false)
//                            } else {
                                startForeground(notificationId, notification)
//                            }

                        }
                        override fun onNotificationCancelled(notificationId: Int,
                                                             dismissedByUser: Boolean) {
                            super.onNotificationCancelled(notificationId, dismissedByUser)
                            stopSelf()
                        }
                    }
                ).setCustomActionReceiver(
                    object : PlayerNotificationManager.CustomActionReceiver {
                        override fun createCustomActions(
                            context: Context,
                            instanceId: Int
                        ): MutableMap<String, NotificationCompat.Action> {
                            val pendingIntent = PendingIntent.getService(
                                context,
                                1,
                                Intent(context, PlayerService::class.java).putExtra("action", 0),
                                PendingIntent.FLAG_IMMUTABLE
                            )
                            return mutableMapOf(
                                Pair("CLOSE", NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "namae", pendingIntent))
                            )
                        }

                        override fun getCustomActions(player: Player): MutableList<String> {
                            return mutableListOf("CLOSE")
                        }

                        override fun onCustomAction(
                            player: Player,
                            action: String,
                            intent: Intent
                        ) {
                            sendToast(action)
                        }

                    }
                ).build()

                playerNotificationManager?.setUseFastForwardAction(false)
                playerNotificationManager?.setUseRewindAction(false)

                playerNotificationManager?.setUseNextActionInCompactView(true)
                playerNotificationManager?.setUsePreviousActionInCompactView(true)

                playerNotificationManager?.setColor(Color.Red.toArgb())
                playerNotificationManager?.setColorized(true)

                playerNotificationManager?.setPlayer(player)
                playerNotificationManager?.setMediaSessionToken(media_session!!.sessionToken)

                media_session?.isActive = true
            }
        }

        private fun getNotificationChannel(): String{
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                MainActivity.getString(R.string.player_service_name),
                NotificationManager.IMPORTANCE_NONE
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            return NOTIFICATION_CHANNEL_ID
        }
    }
}

