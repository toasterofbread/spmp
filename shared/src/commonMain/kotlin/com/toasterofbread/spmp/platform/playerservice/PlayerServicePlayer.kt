package com.toasterofbread.spmp.platform.playerservice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.db.incrementPlayCount
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.MediaPlayerRepeatMode
import com.toasterofbread.spmp.platform.MediaPlayerState
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.service.playercontroller.DiscordStatusHandler
import com.toasterofbread.spmp.service.playercontroller.PersistentQueueHandler
import com.toasterofbread.spmp.service.playercontroller.RadioHandler
import com.toasterofbread.spmp.youtubeapi.radio.RadioInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.random.nextInt

class PlayerServicePlayer(private val service: PlatformPlayerService) {
    internal val radio: RadioHandler = object : RadioHandler(this, service.context) {
        override fun onInstanceStateChanged(state: RadioInstance.RadioState) {
            TODO()
        }
    }
    private lateinit var persistent_queue: PersistentQueueHandler
    private lateinit var discord_status: DiscordStatusHandler
    private val context: PlatformContext get() = service.context

    var active_queue_index: Int by mutableIntStateOf(0)
//
//    val radio_loading: Boolean get() = radio.loading
//    val radio_item: MediaItem? get() = radio.item?.first?.let { getMediaItemFromUid(it) }
//    val radio_item_index: Int? get() = radio.item?.second
//    val radio_filters: List<List<RadioBuilderModifier>>? get() = radio.filters
//    var radio_current_filter: Int?
//        get() = radio.current_filter
//        set(value) {
//            TODO()
////            radio.setRadioFilter(value)
//        }

    private val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val undo_handler: UndoHandler = UndoHandler(this, service)

    val undo_count: Int get() = undo_handler.undo_count
    val redo_count: Int get() = undo_handler.redo_count

    fun redo() = undo_handler.redo()
    fun redoAll() = undo_handler.redoAll()
    fun undo() = undo_handler.undo()
    fun undoAll() = undo_handler.undoAll()

    fun updateActiveQueueIndex(delta: Int = 0) {
        if (delta != 0) {
            active_queue_index = (active_queue_index + delta).coerceIn(service.current_song_index, service.song_count - 1)
        }
        else if (active_queue_index >= service.song_count) {
            active_queue_index = service.current_song_index
        }
    }

    fun playSong(song: Song, start_radio: Boolean = true, shuffle: Boolean = false, at_index: Int = 0) {
        require(start_radio || !shuffle)
        require(at_index >= 0)

        undo_handler.undoableAction {
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

        undo_handler.customUndoableAction { furtherAction ->
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
            TODO()
//            radio.instance.cancelRadio()
        }

        undo_handler.undoableAction {
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

    fun shuffleQueue(start: Int = -1, end: Int = song_count - 1) {
        val range: IntRange =
            if (start < 0) {
                current_song_index + 1..end
            } 
            else if (song_count - start <= 1) {
                return
            }
            else {
                start..end
            }
        shuffleQueue(range)
    }

    fun shuffleQueue(range: IntRange) {
        undo_handler.undoableAction {
            for (i in range) {
                val swap = Random.nextInt(range)
                swapQueuePositions(i, swap, false)
            }
        }
        savePersistentQueue()
    }

    fun shuffleQueueIndices(indices: List<Int>) {
        undo_handler.undoableAction {
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

        undo_handler.undoableAction {
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
            add_to_index = song_count - 1
        }
        else {
            add_to_index = if (index < song_count) index else song_count - 1
        }

        if (is_active_queue) {
            active_queue_index = add_to_index
        }

        undo_handler.customUndoableAction { furtherAction ->
            performAction(UndoHandler.AddAction(song, add_to_index))
            if (start_radio) {
                clearQueue(add_to_index + 1, save = false, cancel_radio = false)

                synchronized(radio) {
                    TODO()
//                    return@customUndoableAction radio.getRadioChangeUndoRedo(
//                        radio.instance.playMediaItem(song, add_to_index),
//                        add_to_index + 1,
//                        save = save,
//                        furtherAction = furtherAction
//                    )
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
        skip_existing: Boolean = false
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

        undo_handler.undoableAction {
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

    fun removeFromQueue(index: Int, save: Boolean = true): Song {
        val song = getSong(index)!!

        undo_handler.undoableAction {
            performAction(UndoHandler.RemoveAction(index))
        }

        if (save) {
            savePersistentQueue()
        }
        return song
    }

    fun seekBy(delta_ms: Long) {
        seekTo(current_position_ms + delta_ms)
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

    // --- UndoHandler ---

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

    val radio_state: RadioInstance.RadioState get() = service.radio_state

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
    
    fun savePersistentQueue() = service.savePersistentQueue()
}
