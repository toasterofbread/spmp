package com.spectre7.spmp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.*
import com.spectre7.spmp.api.RadioInstance
import com.spectre7.spmp.api.RadioModifier
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.api.markSongAsWatched
import com.spectre7.spmp.model.*
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.*
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextInt
import com.spectre7.spmp.platform.*

// Radio continuation will be added if the amount of remaining songs (including current) falls below this
private const val RADIO_MIN_LENGTH: Int = 10
private const val UPDATE_INTERVAL: Long = 5000 // ms
//private const val VOL_NOTIF_SHOW_DURATION: Long = 1000
private const val SONG_MARK_WATCHED_POSITION = 1000 // ms

@Suppress("OPT_IN_USAGE")
class PlayerService : MediaPlayerService() {

    var stop_after_current_song: Boolean by mutableStateOf(false)
    var active_queue_index: Int by mutableStateOf(0)
    private var song_marked_as_watched: Boolean = false

    fun updateActiveQueueIndex(delta: Int = 0) {
        if (delta == 0) {
            if (active_queue_index >= song_count) {
                active_queue_index = current_song_index
            }
            return
        }

        active_queue_index = (active_queue_index + delta).coerceIn(current_song_index, song_count - 1)
    }

    fun playSong(song: Song, start_radio: Boolean = true) {
        if (song == getSong() && start_radio) {
            clearQueue(keep_current = true, save = false)
        }
        else {
            clearQueue(keep_current = false, save = false)
            addToQueue(song)

            if (!start_radio) {
                return
            }
        }

        startRadioAtIndex(1, song, true)
    }

    fun startRadioAtIndex(index: Int, item: MediaItem? = null, skip_first: Boolean = false) {
        synchronized(radio) {
            clearQueue(from = index, keep_current = false, save = false)

            radio.playMediaItem(item ?: getSong(index)!!)
            radio.loadContinuation { result ->
                if (result.isFailure) {
                    SpMp.error_manager.onError("startRadioAtIndex", result.exceptionOrNull()!!)
                    savePersistentQueue()
                }
                else {
                    context.mainThread {
                        addMultipleToQueue(result.getOrThrowHere(), index, skip_first)
                    }
                }
            }
        }
    }

    fun continueRadio() {
        synchronized(radio) {
            if (radio.loading) {
                return
            }
            radio.loadContinuation { result ->
                if (result.isFailure) {
                    SpMp.error_manager.onError("continueRadio", result.exceptionOrNull()!!)
                }
                else {
                    context.mainThread {
                        addMultipleToQueue(result.getOrThrowHere(), song_count, false)
                    }
                }
            }
        }
    }

    fun clearQueue(from: Int = 0, keep_current: Boolean = false, save: Boolean = true, cancel_radio: Boolean = true) {
        if (cancel_radio) {
            radio.cancelRadio()
        }

        for (i in song_count - 1 downTo from) {
            if (keep_current && i == current_song_index) {
                continue
            }
            removeFromQueue(i, save = false)
        }

        if (save) {
            savePersistentQueue()
        }

        updateActiveQueueIndex()
    }

    fun clearQueueWithUndo(from: Int = 0, keep_current: Boolean = false, save: Boolean = true, cancel_radio: Boolean = true): (() -> Unit)? {
        val radio_state =
            if (cancel_radio) radio.cancelRadio()
            else null

        val removed = mutableListOf<Pair<Song, Int>>()
        for (i in song_count - 1 downTo from) {
            if (keep_current && i == current_song_index) {
                continue
            }
            removed.add(Pair(removeFromQueue(i, save = false), i))
        }
        removed.sortBy { it.second }

        if (save) {
            savePersistentQueue()
        }

        updateActiveQueueIndex()

        val index = current_song_index
        return {
            val before = mutableListOf<Song>()
            val after = mutableListOf<Song>()
            for (item in removed.withIndex()) {
                if (item.value.second >= index) {
                    for (i in item.index until removed.size) {
                        after.add(removed[i].first)
                    }
                    break
                }
                before.add(item.value.first)
            }

            addMultipleToQueue(before, 0)
            addMultipleToQueue(after, index + 1)

            if (radio_state != null) {
                radio.setRadioState(radio_state)
            }
        }
    }

    fun shuffleQueue(start: Int = -1, return_swaps: Boolean = false): List<Pair<Int, Int>>? {
        val range: IntRange =
        if (start < 0) {
            current_song_index + 1 until song_count
        }
        else if (song_count - start <= 1) {
            return if (return_swaps) emptyList() else null
        }
        else {
            start until song_count
        }

        val ret: MutableList<Pair<Int, Int>>? = if (return_swaps) mutableListOf() else null

        for (i in range) {
            val swap = Random.nextInt(range)
            swapQueuePositions(i, swap, false)

            if (return_swaps) {
                ret!!.add(Pair(i, swap))
            }
        }

        savePersistentQueue()

        return ret
    }

    fun swapQueuePositions(a: Int, b: Int, save: Boolean = true) {
        if (a == b) {
            return
        }

        assert(a in 0 until song_count)
        assert(b in 0 until song_count)

        val offset_b = b + (if (b > a) -1 else 1)
        moveSong(a, b)
        moveSong(offset_b, a)

        onSongMoved(a, b)
        onSongMoved(offset_b, a)

        if (save) {
            savePersistentQueue()
        }
    }

    fun addToQueue(song: Song, index: Int? = null, is_active_queue: Boolean = false, start_radio: Boolean = false, save: Boolean = true): Int {
        val added_index: Int
        if (index == null) {
            addSong(song)
            added_index = song_count - 1
        }
        else {
            addSong(song, index)
            added_index = if (index < song_count) index else song_count - 1
        }

        if (is_active_queue) {
            active_queue_index = added_index
        }

        onSongAdded(song, added_index)

        if (start_radio) {
            clearQueue(added_index, save = false)

            synchronized(radio) {
                radio.playMediaItem(song)
                radio.loadContinuation { result ->
                    if (result.isFailure) {
                        SpMp.error_manager.onError("addToQueue", result.exceptionOrNull()!!)
                        if (save) {
                            savePersistentQueue()
                        }
                    }
                    else {
                        context.mainThread {
                            addMultipleToQueue(result.getOrThrowHere(), added_index, save = save)
                        }
                    }
                }
            }
        }
        else if (save) {
            savePersistentQueue()
        }

        return added_index
    }

    fun addMultipleToQueue(songs: List<Song>, index: Int = 0, skip_first: Boolean = false, save: Boolean = true) {
        if (songs.isEmpty()) {
            return
        }

        val index_offset = if (skip_first) -1 else 0
        for (song in songs.withIndex()) {
            if (skip_first && song.index == 0) {
                continue
            }

            val item_index = index + song.index + index_offset
            addSong(song.value, item_index)
            onSongAdded(song.value, if (item_index < song_count) item_index else song_count - 1)
        }

        if (save) {
            savePersistentQueue()
        }
    }

    fun removeFromQueue(index: Int, save: Boolean = true): Song {
        val song = getSong(index)!!
        removeSong(index)
        onSongRemoved(song, index)

        if (save) {
            savePersistentQueue()
        }
        return song
    }

    fun addQueueListener(listener: PlayerServiceHost.PlayerQueueListener) {
        queue_listeners.add(listener)
    }

    fun removeQueueListener(listener: PlayerServiceHost.PlayerQueueListener) {
        queue_listeners.remove(listener)
    }

    override fun seekToNext() {
        stop_after_current_song = false
        super.seekToNext()
    }
    override fun seekToPrevious() {
        stop_after_current_song = false
        super.seekToPrevious()
    }

    val radio_loading: Boolean get() = radio.loading
    val radio_item: MediaItem? get() = radio.state.item
    val radio_filters: List<List<RadioModifier>>? get() = radio.state.filters
    var radio_current_filter: Int?
        get() = radio.state.current_filter
        set(value) { radio.setFilter(value) }

    // --- Internal ---

    private val radio = RadioInstance()
    private fun onRadioFiltersChanged(filters: List<RadioModifier>?) {
        radio.cancelJob()
        radio.loadContinuation({
            runBlocking {
                playerLaunch {
                    clearQueue(current_song_index + 1, cancel_radio = false, save = false)
                }
            }
        }) { result ->
            result.fold(
                { songs ->
                    runBlocking {
                        playerLaunch {
                            addMultipleToQueue(songs, current_song_index + 1)
                        }
                    }
                },
                { error ->
                    SpMp.error_manager.onError("onRadioFiltersChanged", error)
                }
            )
        }
    }

    private var queue_listeners: MutableList<PlayerServiceHost.PlayerQueueListener> = mutableListOf()

//    private val broadcast_receiver = object : BroadcastReceiver() {
//        override fun onReceive(_context: Context, intent: Intent) {
//            if (intent.hasExtra("action")) {
//                onActionIntentReceived(intent)
//            }
//        }
//    }

//    private val prefs_change_listener = object : ProjectPreferences.Listener {
//        override fun onChanged(prefs: ProjectPreferences, key: String) {
//            when (key) {
//                Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name -> {
//                    vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)
//                }
//            }
//        }
//    }

    private var tracking_song_index = 0
    private val player_listener = object : MediaPlayerService.Listener() {
        override fun onMediaItemTransition(song: Song?) {
            savePersistentQueue()
            if (current_song_index == tracking_song_index + 1) {
                onSongEnded()
            }
            tracking_song_index = current_song_index
            song_marked_as_watched = false
        }
        override fun onStateChanged(state: MediaPlayerState) {
            if (state == MediaPlayerState.STATE_ENDED) {
                onSongEnded()
            }
        }
    }

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

    // Persistent queue
    private var lock_queue = false
    private val queue_lock = Object()
    private var update_timer: Timer? = null

    fun savePersistentQueue() {
        synchronized(queue_lock) {
            if (lock_queue) {
                return
            }
            lock_queue = true
        }

        val writer = context.openFileOutput("persistent_queue").bufferedWriter()
        writer.write("${current_song_index},${current_position_ms}")
        writer.newLine()

        iterateSongs { _, song: Song ->
            writer.write(song.id)
            writer.newLine()
        }

        writer.close()

        synchronized(queue_lock) {
            lock_queue = false
        }
    }

    fun loadPersistentQueue() {
        synchronized(queue_lock) {
            check(!lock_queue)
            lock_queue = true
        }

        thread {
            val reader: BufferedReader
            try {
                reader = context.openFileInput("persistent_queue").bufferedReader()
            }
            catch (_: FileNotFoundException) {
                synchronized(queue_lock) {
                    lock_queue = false
                }
                return@thread
            }

            val pos_data = reader.readLine().split(',')
            val songs: MutableList<Song?> = mutableListOf()

            var first_song: Song? = null
            val request_limit = Semaphore(10)

            runBlocking { withContext(Dispatchers.IO) { coroutineScope {
                var i = 0
                var line = reader.readLine()
                while (line != null) {
                    val song = Song.fromId(line)
                    val index = i++
                    line = reader.readLine()

                    if (song.title != null) {
                        if (index == 0) {
                            context.mainThread {
                                addToQueue(song, 0, save = false)
                                first_song = song
                            }
                        }
                        else {
                            songs.add(song)
                        }
                        continue
                    }

                    if (index != 0) {
                        songs.add(null)
                    }

                    launch {
                        request_limit.withPermit {
                            song.loadData().onSuccess { loaded ->
                                if (index == 0) {
                                    context.mainThread {
                                        addToQueue(song, 0, save = false)
                                        first_song = song
                                    }
                                }
                                else {
                                    songs[index - 1] = song
                                }
                            }
                        }
                    }
                }
            }}}

            // Pretty sure this is safe?
            while (first_song == null) { runBlocking { delay(100) } }

            context.mainThread {
                if (song_count != 1 || getSong(0) != first_song) {
                    return@mainThread
                }

                addMultipleToQueue(songs as List<Song>, 1)
                seekTo(pos_data[0].toInt(), pos_data[1].toLong())

                synchronized(queue_lock) {
                    lock_queue = false
                }
            }
        }
    }

    inner class PlayerBinder: PlatformBinder() {
        fun getService(): PlayerService = this@PlayerService
    }
    private val binder = PlayerBinder()
    override fun onBind(): PlatformBinder = binder

    override fun onCreate() {
        super.onCreate()

        addListener(player_listener)

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

//        val prefs = ProjectContext(this).getPrefs()
//        prefs.addListener(prefs_change_listener)
//        vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)

//        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast_receiver, IntentFilter(PlayerService::class.java.canonicalName))

        if (update_timer == null) {
            update_timer = createUpdateTimer()
        }
        radio.filter_changed_listeners.add(this::onRadioFiltersChanged)
    }

    override fun onDestroy() {
        radio.filter_changed_listeners.remove(this::onRadioFiltersChanged)

        update_timer?.cancel()
        update_timer = null

//        if (vol_notif.isShown) {
//            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(vol_notif)
//        }
//        ProjectContext(this).getPrefs().removeListener(prefs_change_listener)

//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast_receiver)

        super.onDestroy()
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

    private fun createUpdateTimer(): Timer {
        return Timer().apply {
            scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        context.mainThread {
                            savePersistentQueue()
                            markWatched()
                        }
                    }

                    fun markWatched() {
                        if (
                            !song_marked_as_watched
                            && is_playing
                            && current_position_ms >= SONG_MARK_WATCHED_POSITION
                        ) {
                            song_marked_as_watched = true

                            val song = getSong()!!
                            song.editRegistry {
                                it.play_count++
                            }

                            if (Settings.KEY_ADD_SONGS_TO_HISTORY.get(context)) {
                                context.networkThread {
                                    val result = markSongAsWatched(song.id)
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

    private fun getCustomVolumeChangeAmount(): Float {
        return 1f / Settings.KEY_VOLUME_STEPS.get<Int>(context).toFloat()
    }

    private fun showVolumeNotification(increasing: Boolean, volume: Float) {
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
        checkRadioContinuation()
    }

    private fun onSongMoved(from: Int, to: Int) {
        for (listener in queue_listeners) {
            listener.onSongMoved(from, to)
        }
        checkRadioContinuation()
    }

    private fun onSongEnded() {
        if (stop_after_current_song) {
            pause()
            stop_after_current_song = false
        }
    }

    private fun checkRadioContinuation() {
        if (!radio.active || radio.loading) {
            return
        }

        val remaining = song_count - current_song_index
        if (remaining < RADIO_MIN_LENGTH) {
            continueRadio()
        }
    }

    fun iterateSongs(action: (i: Int, song: Song) -> Unit) {
        for (i in 0 until song_count) {
            action(i, getSong(i)!!)
        }
    }
}
