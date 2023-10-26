package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.incrementPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.toastercomposetools.platform.PlatformPreferences
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.service.playercontroller.DiscordStatusHandler
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random
import kotlin.random.nextInt

private const val UPDATE_INTERVAL: Long = 30000 // ms
//private const val VOL_NOTIF_SHOW_DURATION: Long = 1000
private const val SONG_MARK_WATCHED_POSITION = 1000 // ms

@Suppress("LeakingThis")
abstract class PlayerServicePlayer(private val service: PlatformPlayerService) {
    private val context: AppContext get() = service.context
    private val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    internal val radio: RadioHandler = RadioHandler(this, context)
    private val persistent_queue: PersistentQueueHandler = PersistentQueueHandler(this, context)
    private val discord_status: DiscordStatusHandler = DiscordStatusHandler(this, context)
    private val undo_handler: UndoHandler = UndoHandler(this, service)
    private var update_timer: Timer? = null

    private var tracking_song_index = 0
    private var song_marked_as_watched: Boolean = false

    val undo_count: Int get() = undo_handler.undo_count
    val redo_count: Int get() = undo_handler.redo_count

    var stop_after_current_song: Boolean by mutableStateOf(false)
    var session_started: Boolean by mutableStateOf(false)
    var active_queue_index: Int by mutableIntStateOf(0)

    fun redo() = undo_handler.redo()
    fun redoAll() = undo_handler.redoAll()
    fun undo() = undo_handler.undo()
    fun undoAll() = undo_handler.undoAll()

    abstract fun onUndoStateChanged()

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

    private val player_listener = object : PlayerListener() {
        var current_song: Song? = null

        val song_metadata_listener = Query.Listener {
            discord_status.updateDiscordStatus(current_song)
        }

        override fun onSongTransition(song: Song?, manual: Boolean) {
            if (manual && stop_after_current_song) {
                stop_after_current_song = false
            }

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

            play()
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

        override fun onSongAdded(index: Int, song: Song) {}
    }

    private fun onSongEnded() {
        if (stop_after_current_song) {
            pause()
            stop_after_current_song = false
        }
    }

    init {
        if (ProjectBuildConfig.MUTE_PLAYER == true) {
            service.volume = 0f
        }

        service.addListener(player_listener)
        context.getPrefs().addListener(prefs_listener)
        discord_status.onDiscordAccountTokenChanged()

        if (update_timer == null) {
            update_timer = createUpdateTimer()
        }

        coroutine_scope.launch {
            persistent_queue.loadPersistentQueue()
        }
    }

    fun release() {
        update_timer?.cancel()
        update_timer = null

        service.removeListener(player_listener)
        context.getPrefs().removeListener(prefs_listener)
        discord_status.release()
    }

    fun updateActiveQueueIndex(delta: Int = 0) {
        if (delta != 0) {
            setActiveQueueIndex(active_queue_index + delta)
        }
        else if (active_queue_index >= service.song_count) {
            active_queue_index = service.current_song_index
        }
    }

    fun setActiveQueueIndex(value: Int) {
        active_queue_index = value.coerceIn(service.current_song_index, service.song_count - 1)
    }

    fun cancelSession() {
        pause()
        clearQueue()
        session_started = false
    }

    fun playSong(song: Song, start_radio: Boolean = true, shuffle: Boolean = false, at_index: Int = 0) {
        require(start_radio || !shuffle)
        require(at_index >= 0)

        undo_handler.undoableAction(song_count > 0) {
            if (at_index == 0 && song.id == service.getSong()?.id && start_radio) {
                clearQueue(keep_current = true, save = false)
            }
            else {
                clearQueue(at_index, keep_current = false, save = false, cancel_radio = !start_radio)
                addToQueue(song, at_index)

                if (!start_radio) {
                    return@undoableAction
                }
            }

            startRadioAtIndex(
                at_index + 1,
                song,
                at_index,
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

        undo_handler.customUndoableAction(null) { furtherAction ->
            synchronized(radio) {
                clearQueue(from = index, keep_current = false, save = false, cancel_radio = false)

                val final_item = item ?: getSong(index)!!
                val final_index = if (item != null) item_index else index

                if (final_item !is Song) {
                    coroutine_scope.launch {
                        final_item.incrementPlayCount(context)
                    }
                }

                return@customUndoableAction radio.getRadioChangeUndoRedo(
                    radio.instance.playMediaItem(final_item, final_index, shuffle),
                    index,
                    furtherAction = { a: PlayerServicePlayer.() -> UndoRedoAction? ->
                        furtherAction {
                            a()
                        }
                    },
                    onLoad = onLoad
                )
            }
        }
    }

    fun continueRadio(is_retry: Boolean = false) {
        synchronized(radio) {
            if (radio.instance.loading) {
                return
            }

            radio.instance.loadContinuation(
                context,
                can_retry = true,
                is_retry = is_retry
            ) { result, _ ->
                if (is_retry) {
                    return@loadContinuation
                }

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

        undo_handler.undoableAction(null) {
            for (i in song_count - 1 downTo from) {
                if (keep_current && i == current_song_index) {
                    continue
                }
                removeFromQueue(i, save = false)
            }
        }

        if (save) {
            savePersistentQueue()
        }

        updateActiveQueueIndex()
    }

    fun shuffleQueue(start: Int = 0, end: Int = -1) {
        require(start >= 0)

        val shuffle_end = if (end < 0) song_count -1 else end
        val range: IntRange =
            if (song_count - start <= 1) {
                return
            }
            else {
                start..shuffle_end
            }
        shuffleQueue(range)
    }

    private fun shuffleQueue(range: IntRange) {
        undo_handler.undoableAction(null) {
            for (i in range) {
                val swap = Random.nextInt(range)
                swapQueuePositions(i, swap, false)
            }
        }
        savePersistentQueue()
    }

    fun shuffleQueueIndices(indices: List<Int>) {
        undo_handler.undoableAction(null) {
            for (i in indices.withIndex()) {
                val swap_index = Random.nextInt(indices.size)
                swapQueuePositions(i.value, indices[swap_index], false)
            }
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

        undo_handler.undoableAction(null) {
            performAction(UndoHandler.MoveAction(a, b))
            performAction(UndoHandler.MoveAction(offset_b, a))
        }

        if (save) {
            savePersistentQueue()
        }
    }

    fun addToQueue(song: Song, index: Int? = null, is_active_queue: Boolean = false, start_radio: Boolean = false, save: Boolean = true): Int {
        val add_to_index: Int
        if (index == null) {
            add_to_index = (song_count - 1).coerceAtLeast(0)
        }
        else {
            add_to_index = if (index < song_count) index else (song_count - 1).coerceAtLeast(0)
        }

        if (is_active_queue) {
            active_queue_index = add_to_index
        }

        undo_handler.customUndoableAction(null) { furtherAction ->
            performAction(UndoHandler.AddAction(song, add_to_index))
            if (start_radio) {
                clearQueue(add_to_index + 1, save = false, cancel_radio = false)

                synchronized(radio) {
                    return@customUndoableAction radio.getRadioChangeUndoRedo(
                        radio.instance.playMediaItem(song, add_to_index),
                        add_to_index + 1,
                        save = save,
                        furtherAction = { a ->
                            furtherAction {
                                a()
                            }
                        }
                    )
                }
            }
            else if (save) {
                savePersistentQueue()
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
        skip_existing: Boolean = false,
        clear: Boolean = false
    ) {
        val to_add: List<Song> =
            if (!skip_existing) {
                songs
            }
            else {
                songs.toMutableList().apply {
                    iterateSongs { _, song ->
                        removeAll { it.id == song.id }
                    }
                }
            }

        if (to_add.isEmpty()) {
            return
        }

        val index_offset = if (skip_first) -1 else 0

        undo_handler.undoableAction(null) {
            if (clear) {
                clearQueue(save = false)
            }

            for (song in to_add.withIndex()) {
                if (skip_first && song.index == 0) {
                    continue
                }

                val item_index = index + song.index + index_offset
                performAction(UndoHandler.AddAction(song.value, item_index))
            }
        }

        if (is_active_queue) {
            active_queue_index = index + to_add.size - 1 + index_offset
        }

        if (save) {
            savePersistentQueue()
        }
    }

    fun moveSong(from: Int, to: Int) {
        undo_handler.undoableAction(null) {
            performAction(UndoHandler.MoveAction(from, to))
        }
    }

    fun removeFromQueue(index: Int, save: Boolean = true): Song {
        val song = getSong(index)!!

        undo_handler.performAction(UndoHandler.RemoveAction(index))

        if (save) {
            savePersistentQueue()
        }
        return song
    }

    fun seekBy(delta_ms: Long) {
        seekTo(current_position_ms + delta_ms)
    }

    inline fun iterateSongs(action: (i: Int, song: Song) -> Unit) {
        for (i in 0 until song_count) {
            action(i, getSong(i)!!)
        }
    }

    private fun createUpdateTimer(): Timer {
        return Timer().apply {
            scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        coroutine_scope.launch(Dispatchers.Main) {
                            savePersistentQueue()
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
                                    result.onFailure {
                                        context.sendNotification(it)
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

    // --- UndoHandler ---

    fun undoableAction(action: PlayerServicePlayer.(furtherAction: (PlayerServicePlayer.() -> Unit) -> Unit) -> Unit) {
        undo_handler.undoableAction { a ->
            action { b ->
                a {
                    b()
                }
            }
        }
    }

    fun customUndoableAction(action: PlayerServicePlayer.(furtherAction: (PlayerServicePlayer.() -> UndoRedoAction?) -> Unit) -> UndoRedoAction?) =
        undo_handler.customUndoableAction { a: (UndoHandler.() -> UndoRedoAction?) -> Unit ->
            action(player) { b: PlayerServicePlayer.() -> UndoRedoAction? ->
                a {
                    b()
                }
            }
        }

    // --- Service ---

    val state: MediaPlayerState get() = service.state
    val is_playing: Boolean get() = service.is_playing
    val song_count: Int get() = service.song_count
    val current_song_index: Int get() = service.current_song_index
    val current_position_ms: Long get() = service.current_position_ms
    val duration_ms: Long get() = service.duration_ms
    val has_focus: Boolean get() = service.has_focus

    val radio_state: RadioInstance.RadioState get() = radio.instance.state

    var repeat_mode: MediaPlayerRepeatMode
        get() = service.repeat_mode
        set(value) {
            service.repeat_mode = value
        }
    var volume: Float
        get() = service.volume
        set(value) {
            service.volume = value
        }

    fun play() = service.play()
    fun pause() = service.pause()
    fun playPause() = service.playPause()

    fun seekTo(position_ms: Long) = service.seekTo(position_ms)
    fun seekToSong(index: Int) = service.seekToSong(index)
    fun seekToNext() = service.seekToNext()
    fun seekToPrevious() = service.seekToPrevious()

    fun getSong(): Song? = service.getSong()
    fun getSong(index: Int): Song? = service.getSong(index)
    
    fun savePersistentQueue() {
        coroutine_scope.launch {
            persistent_queue.savePersistentQueue()
        }
    }
}
