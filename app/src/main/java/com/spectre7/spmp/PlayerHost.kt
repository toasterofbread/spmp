package com.spectre7.spmp

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerStatus
import com.spectre7.utils.sendToast
import kotlin.concurrent.thread

enum class SERVICE_INTENT_ACTIONS { STOP, BUTTON_VOLUME }

class PlayerHost {

    private lateinit var service: PlayerService
    private var service_connected by mutableStateOf(false)

    private var service_connection: ServiceConnection? = null
    private var service_intent: Intent? = null
    private val context: Context
        get() = MainActivity.context

    init {
        instance = this
        getService()
    }

    companion object {
        private lateinit var instance: PlayerHost

        lateinit var p_status: PlayerStatus

        val service: PlayerService
            get() = instance.service
        val player: ExoPlayer
            get() = service.player
        val service_connected: Boolean
            get() = instance.service_connected

        fun release() {
            instance.release()
        }
    }

    private fun release() {
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

        println("binding service")

        context.bindService(service_intent,
            object : ServiceConnection {

                override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                    println("service connected")
                    service = (binder as PlayerService.PlayerBinder).getService()
                    service_connected = true
                    on_connected?.invoke()
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    service_connected = false
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

    interface PlayerQueueListener {
        fun onSongAdded(song: Song, index: Int)
        fun onSongRemoved(song: Song, index: Int)
        fun onCleared()
    }

    class PlayerService : Service() {

        private var queue_listeners: MutableList<PlayerQueueListener> = mutableListOf()

        internal lateinit var player: ExoPlayer
        private val NOTIFICATION_ID = 2
        private val NOTIFICATION_CHANNEL_ID = "playback_channel"
        private var playerNotificationManager: PlayerNotificationManager? = null

        private val metadata_builder: MediaMetadataCompat.Builder = MediaMetadataCompat.Builder()
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

            player = ExoPlayer.Builder(MainActivity.context).setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            ).build()
            player.playWhenReady = true
            player.prepare()

            media_session = MediaSessionCompat(MainActivity.context, "spmp")
            media_session_connector = MediaSessionConnector(media_session!!)
            media_session_connector!!.setPlayer(player)
            media_session!!.setMediaButtonReceiver(null)
            media_session?.isActive = true

            media_session!!.setCallback(object: MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(event_intent: Intent?): Boolean {

                    val event = event_intent?.extras?.get("android.intent.extra.KEY_EVENT") as KeyEvent?
                    if (event == null || event.action != KeyEvent.ACTION_DOWN) {
                        return super.onMediaButtonEvent(event_intent)
                    }

                    when (event.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            playPause()
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            player.play()
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            player.pause()
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            player.seekToNextMediaItem()
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            player.seekToPreviousMediaItem()
                        }
                        else -> {
                            sendToast("Unhandled media event: ${event.keyCode}")
                            return super.onMediaButtonEvent(event_intent)
                        }
                    }

                    return true
                }
            })

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
            MediaButtonReceiver.handleIntent(media_session, intent)

            val action = intent?.getIntExtra("action", -1)
            when (action) {
                -1 -> {}
                SERVICE_INTENT_ACTIONS.STOP.ordinal -> {
                    stopForeground(true)
                    stopSelf()

                    // TODO | Stop service properly
                }
                SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal -> {
                    val long = intent.getBooleanExtra("long", false)
                    val key_code = intent.getIntExtra("key_code", -1)

                    println("INTENT RECEIVED $key_code $long")

                    when (key_code) {
                        KeyEvent.KEYCODE_VOLUME_UP -> {}
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {}
                        else -> TODO()
                    }
                }
                else -> throw RuntimeException(action.toString())
            }

            return START_NOT_STICKY
        }

        private fun onSongAdded(song: Song, index: Int) {
            for (listener in queue_listeners) {
                listener.onSongAdded(song, index)
            }
        }

        private fun onSongAdded(media_item: MediaItem) {
            for (i in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(i)
                if (item == media_item) {
                    onSongAdded(item.localConfiguration!!.tag as Song, i)
                    return
                }
            }
            throw RuntimeException()
        }

        private fun onSongRemoved(song: Song, index: Int) {
            for (listener in queue_listeners) {
                listener.onSongRemoved(song, index)
            }
        }

        fun iterateSongs(action: (i: Int, song: Song) -> Unit) {
            for (i in 0 until player.mediaItemCount) {
                action(i, player.getMediaItemAt(i).localConfiguration?.tag as Song)
            }
        }

        fun playSong(song: Song) {
            clearQueue()

            addToQueue(song) {
                thread {
                    Song.batchFromId(DataApi.getSongRadio(song.getId(), false)) { i, song ->
                        MainActivity.runInMainThread {
                            if (song != null) {
                                addToQueue(song, i + 1)
                            }
                        }
                    }
                }
            }
        }

        fun clearQueue() {
            player.clearMediaItems()
            for (listener in queue_listeners) {
                listener.onCleared()
            }
        }

        fun addToQueue(song: Song, i: Int? = null, onFinished: (() -> Unit)? = null) {
            song.getDownloadUrl {
                val item = MediaItem.Builder().setUri(it).setTag(song).build()
                if (i == null) {
                    player.addMediaItem(item)
                }
                else {
                    player.addMediaItem(i, item)
                }

                onSongAdded(item)
                onFinished?.invoke()
            }
        }

        fun removeFromQueue(index: Int) {
            val song = player.getMediaItemAt(index).localConfiguration!!.tag as Song
            player.removeMediaItem(index)
            onSongRemoved(song, index)
        }

        fun addQueueListener(listener: PlayerQueueListener) {
            queue_listeners.add(listener)
        }

        fun removeQueueListener(listener: PlayerQueueListener) {
            queue_listeners.remove(listener)
        }

        fun play(index: Int? = null) {
            player.play()
        }

        fun playPause() {
            if (player.isPlaying) {
                player.pause()
            }
            else {
                player.play()
            }
        }

        private fun addNotificationToPlayer() {
            if (playerNotificationManager == null) {
                playerNotificationManager = PlayerNotificationManager.Builder(
                    MainActivity.context,
                    NOTIFICATION_ID,
                    getNotificationChannel(),
                    object : PlayerNotificationManager.MediaDescriptionAdapter {

                        override fun createCurrentContentIntent(player: Player): PendingIntent? {
                            return PendingIntent.getActivity(
                                MainActivity.context,
                                1,
                                Intent(MainActivity.context, MainActivity::class.java),
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
                                return song.title
                            }
                            catch (e: IndexOutOfBoundsException) {
                                return "Unknown"
                            }
                        }

                        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                            fun getCroppedThumbnail(image: Bitmap): Bitmap {
                                metadata_builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
                                media_session!!.setMetadata(metadata_builder.build())
                                return image
                            }

                            try {
                                val song = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration!!.tag as Song
                                if (song.thumbnailLoaded(true)) {
                                    return getCroppedThumbnail(song.loadThumbnail(true))
                                }

                                thread {
                                    callback.onBitmap(getCroppedThumbnail(song.loadThumbnail(true)))
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
                )
                // .setCustomActionReceiver(
                //     object : PlayerNotificationManager.CustomActionReceiver {
                //         override fun createCustomActions(
                //             context: Context,
                //             instanceId: Int
                //         ): MutableMap<String, NotificationCompat.Action> {
                //             val pendingIntent = PendingIntent.getService(
                //                 context,
                //                 1,
                //                 Intent(context, PlayerService::class.java).putExtra("action", SERVICE_INTENT_ACTIONS.STOP),
                //                 PendingIntent.FLAG_IMMUTABLE
                //             )
                //             return mutableMapOf(
                //                 Pair("Play", NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "namae", pendingIntent))
                //             )
                //         }

                //         override fun getCustomActions(player: Player): MutableList<String> {
                //             return mutableListOf("Play")
                //         }

                //         override fun onCustomAction(
                //             player: Player,
                //             action: String,
                //             intent: Intent
                //         ) {
                //             println(action)
                //         }

                //     }
                // )
                .build()

                playerNotificationManager?.setUseFastForwardAction(false)
                playerNotificationManager?.setUseRewindAction(false)

                playerNotificationManager?.setUseNextActionInCompactView(true)
                playerNotificationManager?.setUsePreviousActionInCompactView(true)

                playerNotificationManager?.setColor(Color.Red.toArgb())
                playerNotificationManager?.setColorized(true)

                playerNotificationManager?.setPlayer(player)
                playerNotificationManager?.setMediaSessionToken(media_session!!.sessionToken)
            }
        }

        private fun getNotificationChannel(): String{
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.player_service_name),
                NotificationManager.IMPORTANCE_NONE
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            return NOTIFICATION_CHANNEL_ID
        }
    }
}

