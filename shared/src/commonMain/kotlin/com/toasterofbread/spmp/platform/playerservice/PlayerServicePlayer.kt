package com.toasterofbread.spmp.platform.playerservice

import PlatformIO
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.incrementPlayCount
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.radio.RadioInstance
import com.toasterofbread.spmp.model.radio.RadioState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.service.playercontroller.DiscordStatusHandler
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import dev.toastbits.composekit.util.platform.Platform
import dev.toastbits.composekit.settings.PlatformSettingsListener
import dev.toastbits.spms.socketapi.shared.SpMsPlayerRepeatMode
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val UPDATE_INTERVAL: Duration = with (Duration) { 30.seconds }
//private const val VOL_NOTIF_SHOW_DURATION: Long = 1000
private val SONG_MARK_WATCHED_POSITION: Duration = with (Duration) { 1.seconds }

@Suppress("LeakingThis")
abstract class PlayerServicePlayer(internal val service: PlayerService) {
    private val context: AppContext get() = service.context
    private val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    internal open val radio: RadioHandler = RadioHandler(this, context)
    private val persistent_queue: PersistentQueueHandler = PersistentQueueHandler(this, context)
    private val discord_status: DiscordStatusHandler = DiscordStatusHandler(this, context)
    private val undo_handler: UndoHandler = UndoHandler(this, service)
    private var update_timer: Job? = null

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

    private val prefs_listener =
        PlatformSettingsListener { key ->
            when (key) {
//                Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.name -> {
//                    vol_notif_enabled = Settings.KEY_ACC_VOL_INTERCEPT_NOTIFICATION.get(preferences = prefs)
//                }
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
                    songQueries.artistsById(current.id).removeListener(song_metadata_listener)
                }
                current_song = song
                current_song?.also { current ->
                    mediaItemQueries.titleById(current.id).addListener(song_metadata_listener)
                    songQueries.artistsById(current.id).addListener(song_metadata_listener)
                }
            }

            coroutine_scope.launch {
                persistent_queue.savePersistentQueue()
            }

            if (current_item_index == tracking_song_index + 1) {
                onSongEnded()
            }
            tracking_song_index = current_item_index
            song_marked_as_watched = false

            radio.checkAutoRadioContinuation()
            discord_status.updateDiscordStatus(song)

            coroutine_scope.launch {
                sendStatusWebhook(song).onFailure {
                    RuntimeException("Sending status webhook failed", it).printStackTrace()
                }
            }

            if (manual) {
                play()
            }
        }

        private suspend fun sendStatusWebhook(song: Song?): Result<Unit> = runCatching {
            val webhook_url: String = context.settings.Misc.STATUS_WEBHOOK_URL.get()
            if (webhook_url.isBlank()) {
                return@runCatching
            }

            val payload: MutableMap<String, JsonElement>

            val user_payload: String = context.settings.Misc.STATUS_WEBHOOK_PAYLOAD.get()
            if (!user_payload.isBlank()) {
                payload =
                    try {
                        Json.decodeFromString(user_payload)
                    }
                    catch (e: Throwable) {
                        e.printStackTrace()
                        mutableMapOf()
                    }
            }
            else {
                payload = mutableMapOf()
            }

            payload["youtube_video_id"] = JsonPrimitive(song?.id)

            val response: HttpResponse =
                HttpClient().post(webhook_url) {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(payload))
                }

            if (response.status.value !in 200 .. 299) {
                throw RuntimeException("${response.status.value}: ${response.bodyAsText()}")
            }
        }

        override fun onSongMoved(from: Int, to: Int) {
            radio.instance.onQueueSongMoved(from, to)
            radio.checkAutoRadioContinuation()
        }

        override fun onSongRemoved(index: Int, song: Song) {
            radio.instance.onQueueSongRemoved(index, song)
            radio.checkAutoRadioContinuation()
        }

        override fun onStateChanged(state: SpMsPlayerState) {
            if (state == SpMsPlayerState.ENDED) {
                onSongEnded()
            }
        }

        override fun onPlayingChanged(is_playing: Boolean) {
            discord_status.updateDiscordStatus(null)
        }

        override fun onDurationChanged(duration_ms: Long) {
            discord_status.updateDiscordStatus(null)
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
        if (ProjectBuildConfig.MUTE_PLAYER == true && !Platform.DESKTOP.isCurrent()) {
            service.setVolume(0.0)
        }

        service.addListener(player_listener)
        context.getPrefs().addListener(prefs_listener)

        if (update_timer == null) {
            update_timer = createUpdateTimer()
        }

        coroutine_scope.launch {
            persistent_queue.loadPersistentQueue()
        }
    }

    fun release() {
        coroutine_scope.cancel()
        update_timer?.cancel()
        update_timer = null

        service.removeListener(player_listener)
        context.getPrefs().removeListener(prefs_listener)
        discord_status.release()
    }

    fun updateActiveQueueIndex(delta: Int = 0, to_end: Boolean = false) {
        if (delta != 0) {
            if (to_end) {
                setActiveQueueIndex(if (delta > 0) Int.MAX_VALUE else Int.MIN_VALUE)
            }
            else {
                setActiveQueueIndex(active_queue_index + delta)
            }
        }
        else if (active_queue_index >= service.item_count) {
            active_queue_index = service.current_item_index
        }
    }

    fun setActiveQueueIndex(value: Int) {
        active_queue_index = value.coerceAtLeast(service.current_item_index).coerceAtMost(service.item_count - 1)
    }

    fun cancelSession() {
        pause()
        clearQueue()
        session_started = false
    }

    fun playSong(song: Song, start_radio: Boolean = true, shuffle: Boolean = false, at_index: Int = 0) {
        require(start_radio || !shuffle)
        require(at_index >= 0)

        undo_handler.undoableAction(item_count > 0) {
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
        source: RadioState.RadioStateSource,
        shuffle: Boolean = false,
        clear_queue: Boolean = true,
        item_queue_index: Int? = null,
        onSuccessfulLoad: (RadioInstance.LoadResult) -> Unit = {}
    ) {
        synchronized(radio) {
            coroutine_scope.launch {
                undo_handler.customUndoableAction { furtherAction ->
                    if (clear_queue) {
                        clearQueue(from = index, keep_current = false, save = false, cancel_radio = false)
                    }

                    return@customUndoableAction radio.setUndoableRadioState(
                        RadioState(
                            source = source,
                            item_queue_index = item_queue_index,
                            shuffle = shuffle
                        ),
                        furtherAction = { action: PlayerServicePlayer.() -> UndoRedoAction? ->
                            furtherAction { action() }
                        },
                        onSuccessfulLoad = onSuccessfulLoad,
                        insertion_index = index,
                        clear_after = true
                    )
                }
            }
        }
    }

    fun startRadioAtIndex(
        index: Int,
        item: MediaItem? = null,
        item_index: Int? = null,
        skip_first: Boolean = false,
        shuffle: Boolean = false,
        onSuccessfulLoad: (RadioInstance.LoadResult) -> Unit = {}
    ) {
        require(item_index == null || item != null)

        synchronized(radio) {
            val final_item: MediaItem = item ?: getSong(index)!!
            val final_index: Int? = if (item != null) item_index else index

            coroutine_scope.launch {
                if (final_item !is Song) {
                    final_item.incrementPlayCount(context)
                }

                val playlist_data: RemotePlaylistData? =
                    (final_item as? RemotePlaylist)?.loadData(context)?.getOrNull()

                startRadioAtIndex(
                    index = index,
                    source = RadioState.RadioStateSource.ItemUid(final_item.getUid()),
                    shuffle = shuffle,
                    clear_queue = playlist_data == null || playlist_data.continuation != null,
                    item_queue_index = final_index,
                    onSuccessfulLoad = onSuccessfulLoad
                )
            }
        }
    }

    fun clearQueue(from: Int = 0, keep_current: Boolean = false, save: Boolean = true, cancel_radio: Boolean = true) {
        if (cancel_radio) {
            radio.instance.cancelRadio()
        }

        undo_handler.undoableAction(null) {
            for (i in item_count - 1 downTo from) {
                if (keep_current && i == current_item_index) {
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

        val shuffle_end = if (end < 0) item_count -1 else end
        val range: IntRange =
            if (item_count - start <= 1) {
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

        assert(a in 0 until item_count)
        assert(b in 0 until item_count)

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
            add_to_index = (item_count - 1).coerceAtLeast(0)
        }
        else {
            add_to_index = if (index < item_count) index else (item_count - 1).coerceAtLeast(0)
        }

        if (is_active_queue) {
            active_queue_index = add_to_index
        }

        undo_handler.customUndoableAction(null) { furtherAction ->
            performAction(UndoHandler.AddAction(song, add_to_index))
            if (start_radio) {
                clearQueue(add_to_index + 1, save = false, cancel_radio = false)

                synchronized(radio) {
                    return@customUndoableAction radio.setUndoableRadioState(
                        RadioState(
                            source = RadioState.RadioStateSource.ItemUid(song.getUid()),
                            item_queue_index = add_to_index
                        ),
                        furtherAction = { action ->
                            furtherAction { action() }
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
        clear: Boolean = false,
        clear_after: Boolean = false
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
            else if (clear_after) {
                clearQueue(save = false, from = index, cancel_radio = false)
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
        if (duration_ms < 0) {
            return
        }

        seekTo((current_position_ms + delta_ms).coerceIn(0, duration_ms))
    }

    inline fun iterateSongs(action: (i: Int, song: Song) -> Unit) {
        for (i in 0 until item_count) {
            action(i, getSong(i)!!)
        }
    }

    private fun createUpdateTimer(): Job =
        coroutine_scope.launch {
            while (true) {
                savePersistentQueue()

                if (
                    !song_marked_as_watched
                    && is_playing
                    && current_position_ms.milliseconds >= SONG_MARK_WATCHED_POSITION
                ) {
                    song_marked_as_watched = true

                    val song: Song = getSong() ?: continue

                    withContext(Dispatchers.PlatformIO) {
                        song.incrementPlayCount(context)

                        val mark_endpoint = context.ytapi.user_auth_state?.MarkSongAsWatched
                        if (mark_endpoint?.isImplemented() == true && context.settings.Misc.ADD_SONGS_TO_HISTORY.get()) {
                            val result = mark_endpoint.markSongAsWatched(song.id)
                            result.onFailure {
                                context.sendNotification(it)
                            }
                        }
                    }
                }

                delay(UPDATE_INTERVAL)
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

    val state: SpMsPlayerState get() = service.state
    val is_playing: Boolean get() = service.is_playing
    val item_count: Int get() = service.item_count
    val current_item_index: Int get() = service.current_item_index
    val current_position_ms: Long get() = service.current_position_ms
    val duration_ms: Long get() = service.duration_ms

    val radio_instance: RadioInstance get() = radio.instance

    val repeat_mode: SpMsPlayerRepeatMode
        get() = service.repeat_mode

    val volume: Float
        get() = service.volume

    fun play() = service.play()
    fun pause() = service.pause()
    fun playPause() = service.playPause()

    fun seekTo(position_ms: Long) = service.seekToTime(position_ms)
    fun seekToSong(index: Int) = service.seekToItem(index)
    fun seekToNext() = service.seekToNext()
    fun seekToPrevious(repeat_threshold: Duration? = null) = service.seekToPrevious(repeat_threshold)
    fun undoSeek() = service.undoSeek()

    fun getSong(): Song? = service.getSong()
    fun getSong(index: Int): Song? = service.getSong(index)

    fun savePersistentQueue() {
        coroutine_scope.launch {
            persistent_queue.savePersistentQueue()
        }
    }
}
