package com.spectre7.spmp

import SpMp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.*
import com.spectre7.spmp.api.*
import com.spectre7.spmp.model.*
import com.spectre7.spmp.model.mediaitem.Artist
import com.spectre7.spmp.model.mediaitem.MediaItem
import com.spectre7.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.platform.*
import com.spectre7.spmp.resources.getString
import com.spectre7.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.*
import java.util.*
import kotlin.random.Random
import kotlin.random.nextInt

// Radio continuation will be added if the amount of remaining songs (including current) falls below this
private const val RADIO_MIN_LENGTH: Int = 10
private const val UPDATE_INTERVAL: Long = 5000 // ms
//private const val VOL_NOTIF_SHOW_DURATION: Long = 1000
private const val SONG_MARK_WATCHED_POSITION = 1000 // ms

private const val DISCORD_APPLICATION_ID = "1081929293979992134"
private const val DISCORD_ASSET_ICON_PRIMARY = "1103702923852132372"

@Suppress("OPT_IN_USAGE")
class PlayerService : MediaPlayerService() {

    var stop_after_current_song: Boolean by mutableStateOf(false)
    var active_queue_index: Int by mutableStateOf(0)
    private var song_marked_as_watched: Boolean = false
    private val coroutine_scope = CoroutineScope(Job())

    private var discord_rpc: DiscordStatus? = null
    private var discord_status_update_job: Job? = null

    fun updateActiveQueueIndex(delta: Int = 0) {
        if (delta != 0) {
            active_queue_index = (active_queue_index + delta).coerceIn(current_song_index, song_count - 1)
        }
        else if (active_queue_index >= song_count) {
            active_queue_index = current_song_index
        }
    }

    fun playSong(song: Song, start_radio: Boolean = true, shuffle: Boolean = false) {
        require(start_radio || !shuffle)
        
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

        startRadioAtIndex(
            1, 
            song, 
            0, 
            skip_first = true, 
            shuffle = shuffle
        )
    }

    fun startRadioAtIndex(index: Int, item: MediaItem? = null, item_index: Int? = null, skip_first: Boolean = false, shuffle: Boolean = false) {
        require(item_index == null || item != null)

        synchronized(radio) {
            clearQueue(from = index, keep_current = false, save = false)

            val final_item = item ?: getSong(index)!!
            val final_index = if (item != null) item_index else index

            radio.playMediaItem(final_item, final_index, shuffle)
            radio.loadContinuation { result ->
                coroutine_scope.launch(Dispatchers.Main) {
                    if (result.isFailure) {
                        SpMp.error_manager.onError("startRadioAtIndex", result.exceptionOrNull()!!)
                        savePersistentQueue()
                    }
                    else {
                        addMultipleToQueue(result.getOrThrowHere(), index, skip_first, skip_existing = true)

                        if (final_item !is Song) {
                            final_item.editRegistry {
                                it.incrementPlayCount()
                            }
                        }
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
                        addMultipleToQueue(result.getOrThrowHere(), song_count, false, skip_existing = true)
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

    fun shuffleQueue(start: Int = -1, end: Int = song_count - 1) {
        val range: IntRange =
            if (start < 0) {
                current_song_index + 1 .. end
            }
            else if (song_count - start <= 1) {
                return
            }
            else {
                start .. end
            }
        shuffleQueue(range)
    }

    fun shuffleQueueAndIndices(indices: List<Int>) {
        for (i in indices.withIndex()) {
            val swap_index = Random.nextInt(indices.size)
            swapQueuePositions(i.value, indices[swap_index], false)
//            indices.swap(i.index, swap_index)
        }
        savePersistentQueue()
    }

    fun shuffleQueue(range: IntRange) {
        for (i in range) {
            val swap = Random.nextInt(range)
            swapQueuePositions(i, swap, false)
        }
        savePersistentQueue()
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

        if (start_radio) {
            clearQueue(added_index + 1, save = false)

            synchronized(radio) {
                radio.playMediaItem(song, added_index)
                radio.loadContinuation { result ->
                    if (result.isFailure) {
                        SpMp.error_manager.onError("addToQueue", result.exceptionOrNull()!!)
                        if (save) {
                            savePersistentQueue()
                        }
                    }
                    else {
                        context.mainThread {
                            addMultipleToQueue(result.getOrThrowHere(), added_index + 1, save = save, skip_existing = true)
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

    fun addMultipleToQueue(songs: List<Song>, index: Int = 0, skip_first: Boolean = false, save: Boolean = true, is_active_queue: Boolean = false, skip_existing: Boolean = false) {
        
        val to_add: List<Song> = 
            if (!skip_existing) {
                songs
            }
            else {
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
        for (song in to_add.withIndex()) {
            if (skip_first && song.index == 0) {
                continue
            }

            val item_index = index + song.index + index_offset
            addSong(song.value, item_index)
        }

        if (is_active_queue) {
            active_queue_index = index + to_add.size - 1 + index_offset
        }

        if (save) {
            savePersistentQueue()
        }
    }

    fun removeFromQueue(index: Int, save: Boolean = true): Song {
        val song = getSong(index)!!
        removeSong(index)

        if (save) {
            savePersistentQueue()
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

    val radio_loading: Boolean get() = radio.loading
    val radio_item: MediaItem? get() = radio.state.item?.first
    val radio_item_index: Int? get() = radio.state.item?.second
    val radio_filters: List<List<RadioModifier>>? get() = radio.state.filters
    var radio_current_filter: Int?
        get() = radio.state.current_filter
        set(value) { radio.setFilter(value) }

    // --- Internal ---

    private val radio = RadioInstance()
    private fun onRadioFiltersChanged(filters: List<RadioModifier>?) {
        val item = radio.state.item
        val add_index = maxOf(item?.second ?: -1, current_song_index) + 1

        radio.cancelJob()
        radio.loadContinuation({
            runBlocking {
                playerLaunch {
                    clearQueue(add_index, cancel_radio = false, save = false)
                }
            }
        }) { result ->
            result.fold(
                { songs ->
                    runBlocking {
                        playerLaunch {
                            addMultipleToQueue(songs, add_index, skip_existing = true)
                        }
                    }
                },
                { error ->
                    SpMp.error_manager.onError("onRadioFiltersChanged", error)
                }
            )
        }
    }

    override fun onSongMoved(from: Int, to: Int) {
        radio.onSongMoved(from, to)
    }

    private val prefs_listener = object : ProjectPreferences.Listener {
        override fun onChanged(prefs: ProjectPreferences, key: String) {
            when (key) {
                Settings.KEY_DISCORD_ACCOUNT_TOKEN.name -> {
                    onDiscordAccountTokenChanged()
                }
//                Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name -> {
//                    vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)
//                }
            }
        }
    }

    private var tracking_song_index = 0
    private val player_listener = object : Listener() {
        var current_song: Song? = null
        val song_title_listener: (String?) -> Unit = {
            updateDiscordStatus(current_song)
        }
        val song_artist_listener: (Artist?) -> Unit = {
            updateDiscordStatus(current_song)
        }
        // TODO | Thumb listener for song and artist

        override fun onSongTransition(song: Song?) {
            current_song?.apply {
                title_listeners.remove(song_title_listener)
                artist_listeners.remove(song_artist_listener)
            }
            current_song = song
            current_song?.apply {
                title_listeners.add(song_title_listener)
                artist_listeners.add(song_artist_listener)
            }

            savePersistentQueue()
            if (current_song_index == tracking_song_index + 1) {
                onSongEnded()
            }
            tracking_song_index = current_song_index
            song_marked_as_watched = false

            checkRadioContinuation()
            updateDiscordStatus(song)
        }
        override fun onStateChanged(state: MediaPlayerState) {
            if (state == MediaPlayerState.ENDED) {
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
    private val queue_lock = Mutex()
    private var persistent_queue_loaded: Boolean = false
    private var update_timer: Timer? = null

    private fun onDiscordAccountTokenChanged() {
        discord_rpc?.close()

        val account_token = Settings.KEY_DISCORD_ACCOUNT_TOKEN.get<String>(context.getPrefs())
        if (!DiscordStatus.isSupported() || (account_token.isBlank() && DiscordStatus.isAccountTokenRequired())) {
            discord_rpc = null
            return
        }

        discord_rpc = DiscordStatus(
            bot_token = ProjectBuildConfig.DISCORD_BOT_TOKEN,
            custom_images_channel_category_id = ProjectBuildConfig.DISCORD_CUSTOM_IMAGES_CHANNEL_CATEGORY,
            custom_images_channel_name_prefix = ProjectBuildConfig.DISCORD_CUSTOM_IMAGES_CHANNEL_NAME_PREFIX,
            account_token = account_token
        )

        updateDiscordStatus(null)
    }

    private fun updateDiscordStatus(song: Song?) {
        discord_status_update_job?.cancel()
        discord_rpc?.apply {
            val song = song ?: getSong()
            if (song?.title == null) {
                close()
                return
            }

            check(song.artist?.title != null)

            discord_status_update_job = Api.scope.launch {
                fun formatText(text: String): String {
                    return text
                        .replace("\$artist", song.artist!!.title!!)
                        .replace("\$song", song.title!!)
                }

                val name = formatText(Settings.KEY_DISCORD_STATUS_NAME.get())
                val text_a = formatText(Settings.KEY_DISCORD_STATUS_TEXT_A.get())
                val text_b = formatText(Settings.KEY_DISCORD_STATUS_TEXT_B.get())
                val text_c = formatText(Settings.KEY_DISCORD_STATUS_TEXT_C.get())

                val large_image: String?
                val small_image: String?

                try {
                    large_image = getCustomImage(song.id) { song.loadThumbnail(MediaItemThumbnailProvider.Quality.LOW)!! }.getOrThrow()
                    small_image = song.artist?.let { artist ->
                        getCustomImage(artist.id) {
                            artist.loadThumbnail(MediaItemThumbnailProvider.Quality.LOW)
                        }.getOrThrow()
                    }
                }
                catch (e: IOException) {
                    return@launch
                }

                val buttons = mutableListOf<Pair<String, String>>().apply {
                    if (Settings.KEY_DISCORD_SHOW_BUTTON_SONG.get()) {
                        add(Settings.KEY_DISCORD_BUTTON_SONG_TEXT.get<String>() to song.url)
                    }
                    if (Settings.KEY_DISCORD_SHOW_BUTTON_PROJECT.get()) {
                        add(Settings.KEY_DISCORD_BUTTON_PROJECT_TEXT.get<String>() to getString("project_url"))
                    }
                }

                setActivity(
                    name = name,
                    type = DiscordStatus.Type.LISTENING,
                    details = text_a.ifEmpty { null },
                    state = text_b.ifEmpty { null },
                    buttons = buttons.ifEmpty { null },
                    large_image = large_image,
                    large_text = text_c.ifEmpty { null },
                    small_image = small_image,
                    small_text = if (small_image != null) song.artist?.title else null,
                    application_id = DISCORD_APPLICATION_ID
                )
            }
        }
    }

    fun savePersistentQueue() {
        if (Platform.is_desktop || !persistent_queue_loaded) {
            return
        }

        if (!queue_lock.tryLock()) {
            return
        }

        val writer = context.openFileOutput("persistent_queue").bufferedWriter()
        writer.write("${current_song_index},${current_position_ms}")
        writer.newLine()

        iterateSongs { _, song: Song ->
            writer.write(song.id)
            writer.newLine()
        }

        SpMp.Log.info("savePersistentQueue saved $song_count songs")

        writer.close()

        queue_lock.unlock()
    }

    suspend fun loadPersistentQueue(apply: Boolean = true) = withContext(Dispatchers.IO) {
        if (Platform.is_desktop) {
            return@withContext
        }

        if (!apply) {
            persistent_queue_loaded = true
            return@withContext
        }

        queue_lock.withLock {
            val reader: BufferedReader
            try {
                reader = context.openFileInput("persistent_queue").bufferedReader()
            }
            catch (_: FileNotFoundException) {
                SpMp.Log.info("loadPersistentQueue no file found")
                persistent_queue_loaded = true
                return@withContext
            }

            coroutineContext.job.invokeOnCompletion {
                reader.close()
            }

            val pos_data = reader.readLine().split(',')

            val songs: MutableList<Song?> = mutableListOf()
            val request_limit = Semaphore(10)
            val jobs: MutableList<Job> = mutableListOf()

            var i = 0
            var line = reader.readLine()
            while (line != null) {
                val song = Song.fromId(line)
                val index = i++
                line = reader.readLine()

                if (song.title != null) {
                    songs.add(song)
                    continue
                }

                songs.add(null)

                jobs.add(
                    launch {
                        request_limit.withPermit {
                            song.getTitle().getOrReport("loadPersistentQueue") ?: return@launch
                            song.getArtist().getOrReport("loadPersistentQueue")
                            song.getThumbnailProvider()

                            synchronized(songs) {
                                songs[index] = song
                            }
                        }
                    }
                )
            }

            jobs.joinAll()

            val to_add = songs.filterIsInstance<Song>()
            if (to_add.isEmpty()) {
                return@withContext
            }

            SpMp.Log.info("loadPersistentQueue adding ${songs.size} songs to $pos_data")

            withContext(Dispatchers.Main) {
                clearQueue(save = false)
                addMultipleToQueue(to_add, 0)
                seekToSong(pos_data[0].toInt())
                seekTo(pos_data[1].toLong())
                persistent_queue_loaded = true
                check(queue_lock.isLocked)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        addListener(player_listener)

        val prefs = context.getPrefs()
        prefs.addListener(prefs_listener)

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

        if (update_timer == null) {
            update_timer = createUpdateTimer()
        }
        radio.filter_changed_listeners.add(this::onRadioFiltersChanged)

        onDiscordAccountTokenChanged()
    }

    override fun onDestroy() {
        removeListener(player_listener)
        coroutine_scope.cancel()

        radio.filter_changed_listeners.remove(this::onRadioFiltersChanged)

        update_timer?.cancel()
        update_timer = null

        discord_rpc?.close()

//        if (vol_notif.isShown) {
//            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(vol_notif)
//        }
//        ProjectContext(this).getPrefs().removeListener(prefs_listener)

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
                                it.incrementPlayCount()
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
