package com.toasterofbread.spmp.service.playerservice

import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.db.incrementPlayCount
import com.toasterofbread.spmp.platform.MediaPlayerService
import com.toasterofbread.spmp.platform.MediaPlayerState
import com.toasterofbread.spmp.platform.PlatformPreferences
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random
import kotlin.random.nextInt

private const val UPDATE_INTERVAL: Long = 5000 // ms
//private const val VOL_NOTIF_SHOW_DURATION: Long = 1000
private const val SONG_MARK_WATCHED_POSITION = 1000 // ms

class PlayerService: MediaPlayerService() {
    var active_queue_index: Int by mutableStateOf(0)
    var stop_after_current_song: Boolean by mutableStateOf(false)

    val radio_loading: Boolean get() = radio.instance.loading
    val radio_item: MediaItem? get() = radio.instance.state.item?.first
    val radio_item_index: Int? get() = radio.instance.state.item?.second
    val radio_filters: List<List<RadioBuilderModifier>>? get() = radio.instance.state.filters
    var radio_current_filter: Int?
        get() = radio.instance.state.current_filter
        set(value) {
            radio.setRadioFilter(value)
        }

    fun updateActiveQueueIndex(delta: Int = 0) {
        if (delta != 0) {
            active_queue_index = (active_queue_index + delta).coerceIn(current_song_index, song_count - 1)
        } else if (active_queue_index >= song_count) {
            active_queue_index = current_song_index
        }
    }

    fun playSong(song: Song, start_radio: Boolean = true, shuffle: Boolean = false) {
        require(start_radio || !shuffle)

        undoableAction {
            if (song.id == getSong()?.id && start_radio) {
                clearQueue(keep_current = true, save = false)
            } else {
                clearQueue(keep_current = false, save = false, cancel_radio = !start_radio)
                addToQueue(song)

                if (!start_radio) {
                    return@undoableAction
                }
            }

            startRadioAtIndex(
                1,
                song,
                0,
                skip_first = true,
                shuffle = shuffle
            )
        }
    }

    fun startRadioAtIndex(
        index: Int,
        item: MediaItem? = null,
        item_index: Int? = null,
        skip_first: Boolean = false,
        shuffle: Boolean = false,
        onLoad: (suspend (success: Boolean) -> Unit)? = null
    ) {
        require(item_index == null || item != null)

        customUndoableAction { furtherAction ->
            synchronized(radio) {
                clearQueue(from = index, keep_current = false, save = false, cancel_radio = false)

                val final_item = item ?: getSong(index)!!
                val final_index = if (item != null) item_index else index

                if (final_item !is Song) {
                    final_item.incrementPlayCount(context)
                }

                return@customUndoableAction radio.getRadioChangeUndoRedo(
                    radio.instance.playMediaItem(final_item, final_index, shuffle),
                    index,
                    furtherAction = furtherAction,
                    onLoad = onLoad
                )
            }
        }
    }

    fun continueRadio() {
        synchronized(radio) {
            if (radio.instance.loading) {
                return
            }
            radio.instance.loadContinuation(
                context,
                can_retry = true
            ) { result, is_retry ->
                result.onSuccess { songs ->
                    withContext(Dispatchers.Main) {
                        addMultipleToQueue(songs, song_count, false, skip_existing = true)
                    }
                }
            }
        }
    }

    fun clearQueue(from: Int = 0, keep_current: Boolean = false, save: Boolean = true, cancel_radio: Boolean = true) {
        if (cancel_radio) {
            radio.instance.cancelRadio()
        }

        undoableAction {
            for (i in song_count - 1 downTo from) {
                if (keep_current && i == current_song_index) {
                    continue
                }
                removeFromQueue(i, save = false)
            }
        }

        if (save) {
            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }
        }

        updateActiveQueueIndex()
    }

    fun shuffleQueue(start: Int = -1, end: Int = song_count - 1) {
        val range: IntRange =
            if (start < 0) {
                current_song_index + 1..end
            } else if (song_count - start <= 1) {
                return
            } else {
                start..end
            }
        shuffleQueue(range)
    }

    fun shuffleQueue(range: IntRange) {
        undoableAction {
            for (i in range) {
                val swap = Random.nextInt(range)
                swapQueuePositions(i, swap, false)
            }
        }
        coroutine_scope.launch {
            persistent_queue.savePersistentQueue()
        }
    }

    fun shuffleQueueIndices(indices: List<Int>) {
        undoableAction {
            for (i in indices.withIndex()) {
                val swap_index = Random.nextInt(indices.size)
                swapQueuePositions(i.value, indices[swap_index], false)
            }
        }
        coroutine_scope.launch {
            persistent_queue.savePersistentQueue()
        }
    }

    fun swapQueuePositions(a: Int, b: Int, save: Boolean = true) {
        if (a == b) {
            return
        }

        assert(a in 0 until song_count)
        assert(b in 0 until song_count)

        val offset_b = b + (if (b > a) -1 else 1)

        undoableAction {
            moveSong(a, b)
            moveSong(offset_b, a)
        }

        if (save) {
            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }
        }
    }

    fun addToQueue(song: Song, index: Int? = null, is_active_queue: Boolean = false, start_radio: Boolean = false, save: Boolean = true): Int {
        val add_to_index: Int
        if (index == null) {
            add_to_index = song_count - 1
        } else {
            add_to_index = if (index < song_count) index else song_count - 1
        }

        if (is_active_queue) {
            active_queue_index = add_to_index
        }

        customUndoableAction { furtherAction ->
            addSong(song, add_to_index)
            if (start_radio) {
                clearQueue(add_to_index + 1, save = false, cancel_radio = false)

                synchronized(radio) {
                    return@customUndoableAction radio.getRadioChangeUndoRedo(
                        radio.instance.playMediaItem(song, add_to_index),
                        add_to_index + 1,
                        save = save,
                        furtherAction = furtherAction
                    )
                }
            } else if (save) {
                coroutine_scope.launch {
                    persistent_queue.savePersistentQueue()
                }
            }

            return@customUndoableAction null
        }

        return add_to_index
    }

    fun addMultipleToQueue(
        songs: List<Song>,
        index: Int = 0,
        skip_first: Boolean = false,
        save: Boolean = true,
        is_active_queue: Boolean = false,
        skip_existing: Boolean = false
    ) {
        val to_add: List<Song> =
            if (!skip_existing) {
                songs
            } else {
                songs.toMutableList().apply {
                    iterateSongs { _, song ->
                        removeAll { it == song }
                    }
                }
            }

        if (to_add.isEmpty()) {
            return
        }

        val index_offset = if (skip_first) -1 else 0

        undoableAction {
            for (song in to_add.withIndex()) {
                if (skip_first && song.index == 0) {
                    continue
                }

                val item_index = index + song.index + index_offset
                addSong(song.value, item_index)
            }
        }

        if (is_active_queue) {
            active_queue_index = index + to_add.size - 1 + index_offset
        }

        if (save) {
            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }
        }
    }

    fun removeFromQueue(index: Int, save: Boolean = true): Song {
        val song = getSong(index)!!

        undoableAction {
            removeSong(index)
        }

        if (save) {
            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }
        }
        return song
    }

    fun seekBy(delta_ms: Long) {
        seekTo(current_position_ms + delta_ms)
    }

    override fun seekToNext() {
        stop_after_current_song = false
        super.seekToNext()
    }

    override fun seekToPrevious() {
        stop_after_current_song = false
        super.seekToPrevious()
    }

    @Composable
    fun RadioLoadStatus(modifier: Modifier, expanded_modifier: Modifier) {
        radio.instance.LoadStatus(modifier, expanded_modifier)
    }

    inline fun iterateSongs(action: (i: Int, song: Song) -> Unit) {
        for (i in 0 until song_count) {
            action(i, getSong(i)!!)
        }
    }

    // --- Internal ---

    private val coroutine_scope = CoroutineScope(Dispatchers.Main)
    private var update_timer: Timer? = null

    private lateinit var radio: RadioHandler
    internal lateinit var persistent_queue: PersistentQueueHandler
    private lateinit var discord_status: DiscordStatusHandler

    private var song_marked_as_watched: Boolean = false
    private var tracking_song_index = 0

    // Volume notification
//    private var vol_notif_enabled: Boolean = false
//    private lateinit var vol_notif: ComposeView
//    private var vol_notif_visible by mutableStateOf(true)
//    private var vol_notif_size by mutableStateOf(Dp.Unspecified)
//    private val vol_notif_params = WindowManager.LayoutParams(
//        WindowManager.LayoutParams.WRAP_CONTENT,
//        WindowManager.LayoutParams.WRAP_CONTENT,
//        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//        PixelFormat.TRANSLUCENT
//    )
//    private var vol_notif_instance: Int = 0

    private val prefs_listener = object : PlatformPreferences.Listener {
        override fun onChanged(prefs: PlatformPreferences, key: String) {
            when (key) {
                Settings.KEY_DISCORD_ACCOUNT_TOKEN.name -> {
                    discord_status.onDiscordAccountTokenChanged()
                }
//                Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name -> {
//                    vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)
//                }
            }
        }
    }

    private val player_listener = object : Listener() {
        var current_song: Song? = null

        val song_metadata_listener = Query.Listener {
            discord_status.updateDiscordStatus(current_song)
        }

        override fun onSongTransition(song: Song?) {
            with(context.database) {
                current_song?.also { current ->
                    mediaItemQueries.titleById(current.id).removeListener(song_metadata_listener)
                    songQueries.artistById(current.id).removeListener(song_metadata_listener)
                }
                current_song = song
                current_song?.also { current ->
                    mediaItemQueries.titleById(current.id).addListener(song_metadata_listener)
                    songQueries.artistById(current.id).addListener(song_metadata_listener)
                }
            }

            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }

            if (current_song_index == tracking_song_index + 1) {
                onSongEnded()
            }
            tracking_song_index = current_song_index
            song_marked_as_watched = false

            radio.checkRadioContinuation()
            discord_status.updateDiscordStatus(song)
        }

        override fun onSongMoved(from: Int, to: Int) {
            radio.checkRadioContinuation()
            radio.instance.onSongMoved(from, to)
        }

        override fun onSongRemoved(index: Int) {
            radio.checkRadioContinuation()
            radio.instance.onSongRemoved(index)
        }

        override fun onStateChanged(state: MediaPlayerState) {
            if (state == MediaPlayerState.ENDED) {
                onSongEnded()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        addListener(player_listener)
        context.getPrefs().addListener(prefs_listener)

        radio = RadioHandler(this, context)
        persistent_queue = PersistentQueueHandler(this, context)
        discord_status = DiscordStatusHandler(this, context)

        coroutine_scope.launch {
            persistent_queue.loadPersistentQueue()
        }

        if (update_timer == null) {
            update_timer = createUpdateTimer()
        }

        discord_status.onDiscordAccountTokenChanged()

//        // Create volume notification view
//        vol_notif = ComposeView(this@PlayerService)
//
//        val lifecycle_owner = object : SavedStateRegistryOwner {
//            override val lifecycle: Lifecycle = LifecycleRegistry(this)
//            private val saved_state_registry_controller: SavedStateRegistryController = SavedStateRegistryController.create(this)
//
//            override val savedStateRegistry: SavedStateRegistry
//                get() = saved_state_registry_controller.savedStateRegistry
//
//            init {
//                saved_state_registry_controller.performRestore(null)
//            }
//        }
//
//        ViewTreeLifecycleOwner.set(vol_notif, lifecycle_owner)
//        vol_notif.setViewTreeSavedStateRegistryOwner(lifecycle_owner)
//        ViewTreeViewModelStoreOwner.set(vol_notif) { ViewModelStore() }
//
//        val coroutineContext = AndroidUiDispatcher.CurrentThread
//        val recomposer = Recomposer(coroutineContext)
//        vol_notif.compositionContext = recomposer
//        CoroutineScope(coroutineContext).launch {
//            recomposer.runRecomposeAndApplyChanges()
//        }

//        vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)

//        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast_receiver, IntentFilter(PlayerService::class.java.canonicalName))
    }

    override fun onDestroy() {
        removeListener(player_listener)
        coroutine_scope.cancel()

        update_timer?.cancel()
        update_timer = null

        discord_status.release()

//        if (vol_notif.isShown) {
//            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(vol_notif)
//        }
//        ProjectContext(this).getPrefs().removeListener(prefs_listener)

//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast_receiver)

        super.onDestroy()
    }

    private fun onSongEnded() {
        if (stop_after_current_song) {
            pause()
            stop_after_current_song = false
        }
    }

    private fun createUpdateTimer(): Timer {
        return Timer().apply {
            scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        coroutine_scope.launch(Dispatchers.Main) {
                            persistent_queue.savePersistentQueue()
                            markWatched()
                        }
                    }

                    suspend fun markWatched() = withContext(Dispatchers.Main) {
                        if (
                            !song_marked_as_watched
                            && is_playing
                            && current_position_ms >= SONG_MARK_WATCHED_POSITION
                        ) {
                            song_marked_as_watched = true

                            val song = getSong() ?: return@withContext

                            withContext(Dispatchers.IO) {
                                song.incrementPlayCount(context)

                                val mark_endpoint = context.ytapi.user_auth_state?.MarkSongAsWatched
                                if (mark_endpoint?.isImplemented() == true && Settings.KEY_ADD_SONGS_TO_HISTORY.get(context)) {
                                    val result = mark_endpoint.markSongAsWatched(song)
                                    if (result.isFailure) {
                                        SpMp.error_manager.onError("autoMarkSongAsWatched", result.exceptionOrNull()!!)
                                    }
                                }
                            }
                        }
                    }
                },
                0,
                UPDATE_INTERVAL
            )
        }
    }

//    private fun onActionIntentReceived(intent: Intent) {
//        val action = SERVICE_INTENT_ACTIONS.values()[intent.extras!!.get("action") as Int]
//        when (action) {
//            SERVICE_INTENT_ACTIONS.STOP -> {
//                stopForeground(true)
//                stopSelf()
//
//                // TODO | Stop service properly
//            }
//            SERVICE_INTENT_ACTIONS.BUTTON_VOLUME -> {
//                val long = intent.getBooleanExtra("long", false)
//                val up = intent.getBooleanExtra("up", false)
//
//                if (long) {
//                    vibrateShort()
//                    if (up) seekToNext()
//                    else seekToPrevious()
//                }
//                else {
//                    volume = volume + (if (up) getCustomVolumeChangeAmount() else -getCustomVolumeChangeAmount())
//                    if (vol_notif_enabled) {
//                        showVolumeNotification(up, volume)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun getCustomVolumeChangeAmount(): Float {
//        return 1f / Settings.KEY_VOLUME_STEPS.get<Int>(context).toFloat()
//    }
//
//    private fun showVolumeNotification(increasing: Boolean, volume: Float) {
//        val FADE_DURATION: Long = 200
//        val BACKGROUND_COLOUR = Color.Black.setAlpha(0.5f)
//        val FOREGROUND_COLOUR = Color.White
//
//        vol_notif_visible = true
//
//        vol_notif.setContent {
//            val volume_i = (volume * 100f).roundToInt()
//            AnimatedVisibility(vol_notif_visible, exit = fadeOut(tween((FADE_DURATION).toInt()))) {
//                Column(
//                    Modifier
//                        .background(BACKGROUND_COLOUR, RoundedCornerShape(16))
//                        .requiredSize(vol_notif_size)
//                        .alpha(if (vol_notif_size.isUnspecified) 0f else 1f)
//                        .onSizeChanged { size ->
//                            if (vol_notif_size.isUnspecified) {
//                                vol_notif_size = max(size.width.dp, size.height.dp) / 2
//                            }
//                        },
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Icon(
//                        if (volume_i == 0) Icons.Filled.VolumeOff else if (volume_i < 50) Icons.Filled.VolumeDown else Icons.Filled.VolumeUp,
//                        null,
//                        Modifier.requiredSize(100.dp),
//                        tint = FOREGROUND_COLOUR
//                    )
//
//                    Text("$volume_i%", color = FOREGROUND_COLOUR, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
//                }
//            }
//        }
//
//        if (!vol_notif.isShown) {
//            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(vol_notif, vol_notif_params)
//        }
//
//        val instance = ++vol_notif_instance
//
//        thread {
//            Thread.sleep(VOL_NOTIF_SHOW_DURATION - FADE_DURATION)
//            if (vol_notif_instance != instance) {
//                return@thread
//            }
//            vol_notif_visible = false
//
//            Thread.sleep(FADE_DURATION)
//            runInMainThread {
//                if (vol_notif_instance == instance && vol_notif.isShown) {
//                    (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeViewImmediate(vol_notif)
//                }
//            }
//        }
//    }
}