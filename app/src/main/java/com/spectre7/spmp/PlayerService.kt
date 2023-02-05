package com.spectre7.spmp

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.ResolvingDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.spectre7.spmp.api.DATA_API_USER_AGENT
import com.spectre7.spmp.api.RadioInstance
import com.spectre7.spmp.model.*
import com.spectre7.utils.sendToast
import com.spectre7.utils.setAlpha
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

const val VOL_NOTIF_SHOW_DURATION: Long = 1000

class PlayerService : Service() {

    lateinit var player: ExoPlayer

    val session_started: Boolean get() = _session_started

    var active_queue_index: Int by mutableStateOf(0)
    fun updateActiveQueueIndex(delta: Int) {
        active_queue_index = (active_queue_index + delta).coerceIn(player.currentMediaItemIndex, player.mediaItemCount - 1)
    }

    fun getSong(index: Int): Song? {
        if (index >= player.mediaItemCount) {
            return null
        }
        return player.getMediaItemAt(index).localConfiguration?.getSong()
    }

    fun getCurrentSong(): Song? {
        val song = getSong(player.currentMediaItemIndex)
        if (song?.loaded == true) {
            return song
        }
        return null
    }

    fun playSong(song: Song, start_radio: Boolean = true) {
        clearQueue()

        addToQueue(song)

        if (!start_radio) {
            return
        }

        thread {
            addMultipleToQueueAndLoad(radio.startNewRadio(song), 1, true)
        }
    }

    fun continueRadio() {
        if (!radio.has_continuation) {
            return
        }
        thread {
            addMultipleToQueueAndLoad(radio.getRadioContinuation(), 1, false)
        }
    }

    fun clearQueue(from: Int = 0, keep_current: Boolean = false): List<Pair<Song, Int>> {
        val ret = mutableListOf<Pair<Song, Int>>()
        for (i in player.mediaItemCount - 1 downTo from) {
            if (keep_current && i == player.currentMediaItemIndex) {
                continue
            }
            ret.add(Pair(removeFromQueue(i), i))
        }
        ret.sortBy { it.second }
        return ret
    }

    fun shuffleQueue(start: Int = -1, return_swaps: Boolean = false): List<Pair<Int, Int>>? {
        val range: IntRange =
        if (start < 0) {
            player.currentMediaItemIndex + 1 until player.mediaItemCount
        }
        else if (player.mediaItemCount - start <= 1) {
            return if (return_swaps) emptyList() else null
        }
        else {
            start until player.mediaItemCount
        }

        val ret: MutableList<Pair<Int, Int>>? = if (return_swaps) mutableListOf() else null

        for (i in range) {
            val swap = Random.nextInt(range)
            swapQueuePositions(i, swap)

            if (return_swaps) {
                ret!!.add(Pair(i, swap))
            }
        }

        return ret
    }

    fun swapQueuePositions(a: Int, b: Int) {
        if (a == b) {
            return
        }

        assert(a >= 0 && a < player.mediaItemCount)
        assert(b >= 0 && b < player.mediaItemCount)

        val offset_b = b + (if (b > a) -1 else 1)
        player.moveMediaItem(a, b)
        player.moveMediaItem(offset_b, a)

        onSongMoved(a, b)
        onSongMoved(offset_b, a)
    }

    fun addToQueue(song: Song, index: Int? = null, is_active_queue: Boolean = false, start_radio: Boolean = false): Int {
        val item = ExoMediaItem.Builder().setTag(song).setUri(song.id).build()

        val added_index: Int
        if (index == null) {
            player.addMediaItem(item)
            added_index = player.mediaItemCount - 1
        }
        else {
            player.addMediaItem(index, item)
            added_index = if (index < player.mediaItemCount) index else player.mediaItemCount - 1
        }

        if (is_active_queue) {
            active_queue_index = added_index
        }

        onSongAdded(song, added_index)
        addNotificationToPlayer()

        if (start_radio) {
            clearQueue(added_index)
            thread {
                addMultipleToQueueAndLoad(radio.startNewRadio(song), added_index)
            }
        }

        return added_index
    }

    fun addMultipleToQueue(songs: List<Song>, index: Int = 0, skip_first: Boolean = false) {
        if (songs.isEmpty()) {
            return
        }

        val index_offset = if (skip_first) -1 else 0
        for (song in songs.withIndex()) {
            if (skip_first && song.index == 0) {
                continue
            }

            val item = ExoMediaItem.Builder().setTag(song).setUri(song.value.id).build()
            val item_index = index + song.index + index_offset

            player.addMediaItem(item_index, item)
            onSongAdded(song.value, if (item_index < player.mediaItemCount) item_index else player.mediaItemCount - 1)
        }

        addNotificationToPlayer()
    }

    fun addMultipleToQueueAndLoad(songs: List<Song>, index: Int = 0, skip_first: Boolean = false) {
        MainActivity.runInMainThread {
            addMultipleToQueue(songs, index, skip_first)
        }

        runBlocking { withContext(Dispatchers.IO) { coroutineScope {
            var skipped = !skip_first
            for (song in songs) {
                if (!skipped) {
                    skipped = true
                    continue
                }
                launch {
                    song.loadData()
                }
            }
        }}}
    }

    fun removeFromQueue(index: Int): Song {
        val song = getSong(index)!!
        player.removeMediaItem(index)
        onSongRemoved(song, index)
        return song
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


    // --- Internal ---

    private val radio = RadioInstance()

    private lateinit var cache: SimpleCache
    private var _session_started: Boolean by mutableStateOf(false)

    private var queue_listeners: MutableList<PlayerServiceHost.PlayerQueueListener> = mutableListOf()

    private val NOTIFICATION_ID = 2
    private val NOTIFICATION_CHANNEL_ID = "playback_channel"
    private var notification_manager: PlayerNotificationManager? = null

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

        val cache_dir = cacheDir.resolve("exoplayer-cache")
        if (!cache_dir.exists()) {
            cache_dir.mkdir()
        }
        cache = SimpleCache(cache_dir, NoOpCacheEvictor(), StandaloneDatabaseProvider(this))

        player = ExoPlayer.Builder(
            MainActivity.context,
            DefaultMediaSourceFactory(
                createDataSourceFactory(),
                { arrayOf(MatroskaExtractor(), FragmentedMp4Extractor()) }
            )
        )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setUsePlatformDiagnostics(false)
            .build()
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

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(
            CacheDataSource.Factory().setCache(cache).apply {
                setUpstreamDataSourceFactory(
                    DefaultHttpDataSource.Factory()
                        .setConnectTimeoutMs(16000)
                        .setReadTimeoutMs(8000)
                        .setUserAgent(DATA_API_USER_AGENT)
                )
            }
        ) { data_spec: DataSpec ->
            data_spec.withUri(Uri.parse(Song.fromId(data_spec.uri.toString()).loadStreamUrl()))
        }
    }

    override fun onDestroy() {
        _session_started = false
        notification_manager?.setPlayer(null)
        notification_manager = null
        media_session?.release()
        player.release()
        cache.release()

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

                if (long) {
                    if (up) player.seekToNextMediaItem()
                    else player.seekToPreviousMediaItem()
                }
                else {
                    player.volume = player.volume + (if (up) getCustomVolumeChangeAmount() else -getCustomVolumeChangeAmount())
                    if (vol_notif_enabled) {
                        showVolumeNotification(up, player.volume)
                    }
                }
            }
            else -> throw NotImplementedError(action.toString())
        }
    }

    private fun getCustomVolumeChangeAmount(): Float {
        return 1f / Settings.get<Int>(Settings.KEY_VOLUME_STEPS).toFloat()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(media_session, intent)
        return START_NOT_STICKY
    }

    private fun onSongAdded(song: Song, index: Int) {
        for (listener in queue_listeners) {
            listener.onSongAdded(song, index)
        }
    }

    private fun onSongRemoved(song: Song, index: Int) {
        for (listener in queue_listeners) {
            listener.onSongRemoved(song, index)
        }
    }

    private fun onSongMoved(from: Int, to: Int) {
        for (listener in queue_listeners) {
            listener.onSongMoved(from, to)
        }
    }

    fun iterateSongs(action: (i: Int, song: Song) -> Unit) {
        for (i in 0 until player.mediaItemCount) {
            action(i, getSong(i)!!)
        }
    }

    private fun addNotificationToPlayer() {
        if (notification_manager != null) {
            return
        }
        notification_manager = PlayerNotificationManager.Builder(
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
                    fun getCroppedThumbnail(image: Bitmap?): Bitmap? {
                        if (image == null) {
                            return null
                        }

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
                        if (song.isThumbnailLoaded(MediaItem.ThumbnailQuality.HIGH)) {
                            return getCroppedThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH))
                        }

                        thread {
                            val cropped = getCroppedThumbnail(song.loadThumbnail(MediaItem.ThumbnailQuality.HIGH))
                            if (cropped != null) {
                                callback.onBitmap(cropped)
                            }
                        }

                        return null
                    }
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

        notification_manager?.setUseFastForwardAction(false)
        notification_manager?.setUseRewindAction(false)

        notification_manager?.setUseNextActionInCompactView(true)
        notification_manager?.setUsePreviousActionInCompactView(true)

        notification_manager?.setPlayer(player)
        notification_manager?.setMediaSessionToken(media_session!!.sessionToken)

        _session_started = true
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
