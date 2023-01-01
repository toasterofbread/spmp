package com.spectre7.spmp

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.session.MediaButtonReceiver
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Settings
import com.spectre7.spmp.model.Song
import com.spectre7.utils.sendToast
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.concurrent.thread
import kotlin.math.roundToInt

const val VOL_NOTIF_SHOW_DURATION: Long = 1000

class PlayerService : Service() {

    private var queue_listeners: MutableList<PlayerServiceHost.PlayerQueueListener> = mutableListOf()

    internal lateinit var player: ExoPlayer
    private val NOTIFICATION_ID = 2
    private val NOTIFICATION_CHANNEL_ID = "playback_channel"
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val metadata_builder: MediaMetadataCompat.Builder = MediaMetadataCompat.Builder()
    private var media_session: MediaSessionCompat? = null
    private var media_session_connector: MediaSessionConnector? = null

    private val broadcast_receiver = object : BroadcastReceiver() {
        override fun onReceive(_context: Context, intent: Intent) {
            if (intent.hasExtra("action")) {
                onActionIntentReceived(intent)
            }
        }
    }

    private val prefs_change_listener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name -> {
                    vol_notif_enabled = Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION, preferences = prefs)
                }
            }
        }

    // Volume notification
    private var vol_notif_enabled: Boolean = false
    private lateinit var vol_notif: ComposeView
    private var vol_notif_visible by mutableStateOf(true)
    private var vol_notif_size by mutableStateOf(Dp.Unspecified)
    private val vol_notif_params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )
    private var vol_notif_instance: Int = 0

    private var integrated_server: PyObject? = null
    private val integrated_server_start_mutex = Mutex()
    fun getIntegratedServer(): PyObject? {
        return integrated_server
    }

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
        player.playWhenReady = false
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
                        play()
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

        // Create volume notification view
        vol_notif = ComposeView(MainActivity.context)

        val lifecycle_owner = object : SavedStateRegistryOwner {
            private val lifecycle_registry: LifecycleRegistry = LifecycleRegistry(this)
            private val saved_state_registry_controller: SavedStateRegistryController = SavedStateRegistryController.create(this)

            override val savedStateRegistry: SavedStateRegistry
                get() = saved_state_registry_controller.savedStateRegistry

            override fun getLifecycle(): Lifecycle {
                return lifecycle_registry
            }

            init {
                saved_state_registry_controller.performRestore(null)
                lifecycle_registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            }
        }
        ViewTreeLifecycleOwner.set(vol_notif, lifecycle_owner)
        vol_notif.setViewTreeSavedStateRegistryOwner(lifecycle_owner)
        ViewTreeViewModelStoreOwner.set(vol_notif) { ViewModelStore() }

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val recomposer = Recomposer(coroutineContext)
        vol_notif.compositionContext = recomposer
        CoroutineScope(coroutineContext).launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        val prefs = MainActivity.getSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefs_change_listener)
        vol_notif_enabled = Settings.get(Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION, preferences = prefs)

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast_receiver, IntentFilter(PlayerService::class.java.canonicalName))
    }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        media_session?.release()
        player.release()
        stopIntegratedServer()

        if (vol_notif.isShown) {
            MainActivity.context.windowManager.removeView(vol_notif)
        }

        MainActivity.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefs_change_listener)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast_receiver)

        super.onDestroy()
    }

    private fun onActionIntentReceived(intent: Intent) {
        val action = intent.getIntExtra("action", -1)
        when (action) {
            -1 -> {}
            SERVICE_INTENT_ACTIONS.STOP.ordinal -> {
                stopForeground(true)
                stopSelf()

                // TODO | Stop service properly
            }
            SERVICE_INTENT_ACTIONS.BUTTON_VOLUME.ordinal -> {
                val long = intent.getBooleanExtra("long", false)
                val up = intent.getBooleanExtra("up", false)

                val vol_ch = 0.1f

                if (long) {
                    if (up) player.seekToNextMediaItem()
                    else player.seekToPreviousMediaItem()
                }
                else {
                    player.volume = player.volume + (if (up) vol_ch else -vol_ch)
                    if (vol_notif_enabled) {
                        showVolumeNotification(up, player.volume)
                    }
                }

                println("$up | $long | ${player.volume}")
            }
            else -> throw RuntimeException(action.toString())
        }
    }

    private fun showVolumeNotification(increasing: Boolean, volume: Float) {
        val FADE_DURATION: Long = 200
        val BACKGROUND_COLOUR = Color.Black.setAlpha(0.5)
        val FOREGROUND_COLOUR = Color.White

        vol_notif_visible = true

        vol_notif.setContent {
            val volume_i = (volume * 100f).roundToInt()
            AnimatedVisibility(vol_notif_visible, exit = fadeOut(tween((FADE_DURATION).toInt()))) {
                Column(
                    Modifier
                        .background(BACKGROUND_COLOUR, RoundedCornerShape(16))
                        .requiredSize(vol_notif_size)
                        .alpha(if (vol_notif_size.isUnspecified) 0f else 1f)
                        .onSizeChanged { size ->
                            if (vol_notif_size.isUnspecified) {
                                vol_notif_size = max(size.width.dp, size.height.dp) / 2
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (volume_i == 0) Icons.Filled.VolumeOff else if (volume_i < 50) Icons.Filled.VolumeDown else Icons.Filled.VolumeUp,
                        null,
                        Modifier.requiredSize(100.dp),
                        tint = FOREGROUND_COLOUR
                    )

                    Text("$volume_i%", color = FOREGROUND_COLOUR, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                }
            }
        }

        if (!vol_notif.isShown) {
            MainActivity.context.windowManager.addView(vol_notif, vol_notif_params)
        }

        val instance = ++vol_notif_instance

        thread {
            Thread.sleep(VOL_NOTIF_SHOW_DURATION - FADE_DURATION)
            if (vol_notif_instance != instance) {
                return@thread
            }
            vol_notif_visible = false

            Thread.sleep(FADE_DURATION)
            MainActivity.runInMainThread {
                if (vol_notif_instance == instance && vol_notif.isShown) {
                    MainActivity.context.windowManager.removeViewImmediate(vol_notif)
                }
            }
        }
    }

    suspend fun startIntegratedServer(): Boolean {
        if (integrated_server != null || integrated_server_start_mutex.isLocked) {
            return false
        }
        integrated_server_start_mutex.lock()
        integrated_server = Python.getInstance().getModule("main").callAttr("runServer", true)
        integrated_server_start_mutex.unlock()

        return true
    }

    fun stopIntegratedServer(): Boolean {
        if (integrated_server == null) {
            return false
        }

        integrated_server!!.callAttr("stop")
        integrated_server!!.close()
        integrated_server = null
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(media_session, intent)
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
            action(i, getSong(i)!!)
        }
    }

    fun getSong(index: Int): Song? {
        return try {
            player.getMediaItemAt(index).localConfiguration?.tag as Song?
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    fun getCurrentSong(): Song? {
        return getSong(player.currentMediaItemIndex)
    }

    fun playSong(song: Song, add_radio: Boolean = true) {
        clearQueue()

        addToQueue(song) {
            if (add_radio) {
                thread {
                    val radio = DataApi.getSongRadio(song.id, false)
                    for (i in radio.indices) {
                        Song.fromId(radio[i]).loadData(
                            process_queue = false,
                            get_stream_url = true
                        ) {
                            if (it != null) {
                                MainActivity.runInMainThread {
                                    addToQueue(it as Song)
                                }
                            }
                        }
                    }
                    DataApi.processYtItemLoadQueue()
                }
            }
        }
    }

    fun clearQueue(keep_current: Boolean = false): List<Pair<Song, Int>> {
        val ret = mutableListOf<Pair<Song, Int>>()
        if (keep_current) {
            var i = 0
            while (player.currentMediaItemIndex > 0) {
                ret.add(Pair(getSong(0)!!, i++))
                removeFromQueue(0)
            }
            while (player.mediaItemCount > 1) {
                ret.add(Pair(getSong(player.mediaItemCount - 1)!!, player.mediaItemCount - 1 + i))
                removeFromQueue(player.mediaItemCount - 1)
            }
        }
        else {
            iterateSongs { i, song ->
                ret.add(Pair(song, i))
            }
            player.clearMediaItems()
            for (listener in queue_listeners) {
                listener.onCleared()
            }
        }
        return ret.sortedBy {
            it.second
        }
    }

    fun addToQueue(song: Song, index: Int? = null, onFinished: (() -> Unit)? = null) {
        song.getStreamUrl { url ->
            val item = MediaItem.Builder().setUri(url).setTag(song).build()
            if (index == null) {
                player.addMediaItem(item)
            }
            else {
                player.addMediaItem(index, item)
            }
            onSongAdded(item)
            addNotificationToPlayer()
            onFinished?.invoke()
        }
    }

    fun addMultipleToQueue(songs: List<Song>, index: Int, onFinished: (() -> Unit)? = null) {
        if (songs.isEmpty()) {
            onFinished?.invoke()
            return
        }

        val loaded = MutableList<MediaItem?>(songs.size) { null }
        var added = 0

        for (song in songs.withIndex()) {
            song.value.getStreamUrl {
                loaded[song.index] = MediaItem.Builder().setUri(it).setTag(song.value).build()

                if (++added == songs.size) {
                    player.addMediaItems(index, loaded as MutableList<MediaItem>)
                    for (item in loaded) {
                        onSongAdded(item)
                    }
                    onFinished?.invoke()
                }
            }
        }
    }

    fun removeFromQueue(index: Int) {
        val song = getSong(index)!!
        player.removeMediaItem(index)
        onSongRemoved(song, index)
    }

    fun addQueueListener(listener: PlayerServiceHost.PlayerQueueListener) {
        queue_listeners.add(listener)
    }

    fun removeQueueListener(listener: PlayerServiceHost.PlayerQueueListener) {
        queue_listeners.remove(listener)
    }

    fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
        }
        player.play()
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        }
        else {
            play()
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
                        return getCurrentSong()?.artist?.name
                    }

                    override fun getCurrentContentTitle(player: Player): String {
                        return getCurrentSong()?.title ?: "NULL"
                    }

                    override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                        fun getCroppedThumbnail(image: Bitmap): Bitmap {
                            if (Build.VERSION.SDK_INT >= 33) {
                                metadata_builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
                                media_session!!.setMetadata(metadata_builder.build())
                                return image
                            }
                            else {
                                return Bitmap.createBitmap(image, (image.width - image.height) / 2, 0, image.height, image.height)
                            }
                        }

                        try {
                            val song = getCurrentSong() ?: return null
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
