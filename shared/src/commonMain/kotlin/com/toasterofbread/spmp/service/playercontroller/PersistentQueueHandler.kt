package com.toasterofbread.spmp.service.playercontroller

import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import dev.toastbits.composekit.platform.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import PlatformIO
import com.toasterofbread.spmp.db.persistentqueue.PersistentQueueMetadata
import dev.toastbits.composekit.platform.lazyAssert

internal class PersistentQueueHandler(val state: PlayerServicePlayer, val context: AppContext) {
    private var persistent_queue_loaded: Boolean = false
    private val queue_lock = Mutex()

    private fun getPersistentQueueMetadata(): PersistentQueueMetadata =
        PersistentQueueMetadata(0, state.current_song_index.toLong(), state.current_position_ms)

    suspend fun savePersistentQueue() {
        if (!persistent_queue_loaded || !context.settings.system.PERSISTENT_QUEUE.get() || ProjectBuildConfig.DISABLE_PERSISTENT_QUEUE == true) {
            return
        }

        val songs: MutableList<Song> = mutableListOf()
        val metadata: PersistentQueueMetadata

        withContext(Dispatchers.Main) {
            for (i in 0 until state.song_count) {
                val song: Song? = state.getSong(i)
                if (song != null) {
                    songs.add(song)
                }
            }
            metadata = getPersistentQueueMetadata()
        }

        withContext(Dispatchers.PlatformIO) {
            context.database.transaction {
                context.database.persistentQueueMetadataQueries.set(
                    id = metadata.id,
                    queue_index = metadata.queue_index,
                    playback_position_ms = metadata.playback_position_ms
                )

                context.database.persistentQueueItemQueries.clear()
                for ((index, song) in songs.withIndex()) {
                    context.database.persistentQueueItemQueries.insert(index.toLong(), song.id)
                }
            }
        }

        println("savePersistentQueue: Saved ${songs.size} songs with data $metadata")
        persistent_queue_loaded = true
    }

    suspend fun loadPersistentQueue() {
        if (Platform.DESKTOP.isCurrent()) {
            // TODO
            return
        }

        if (ProjectBuildConfig.DISABLE_PERSISTENT_QUEUE == true) {
            return
        }

        if (state.song_count > 0) {
            println("loadPersistentQueue: Skipping, queue already populated")
            persistent_queue_loaded = true
            return
        }

        withContext(Dispatchers.PlatformIO) {
            if (!context.settings.system.PERSISTENT_QUEUE.get()) {
                println("loadPersistentQueue: Skipping, feature disabled")

                return@withContext
            }

            if (!queue_lock.tryLock()) {
                println("loadPersistentQueue: Skipping, lock already acquired")
                return@withContext
            }

            val songs: List<Song>
            val metadata: PersistentQueueMetadata?

            try {
                if (persistent_queue_loaded) {
                    println("loadPersistentQueue: Skipping, queue already loaded")
                    return@withContext
                }

                val queue: Pair<List<SongData>, PersistentQueueMetadata?> = getSavedQueue(context)
                songs = queue.first
                metadata = queue.second

                if (songs.isEmpty()) {
                    println("loadPersistentQueue: Saved queue is empty")
                    return@withContext
                }

                val request_limit = Semaphore(10)
                val jobs: MutableList<Job> = mutableListOf()

                for (song in songs) {
                    if (song.Title.get(context.database) == null) {
                        jobs.add(
                            launch {
                                request_limit.withPermit {
                                    MediaItemLoader.loadSong(song, context)
                                }
                            }
                        )
                    }
                }

                jobs.joinAll()
            }
            catch (e: Throwable) {
                println("loadPersistentQueue: Failed with $e")
                persistent_queue_loaded = true
                return@withContext
            }
            finally {
                queue_lock.unlock()
                persistent_queue_loaded = true
            }

            withContext(Dispatchers.Main) {
                println("loadPersistentQueue: Adding ${songs.size} songs to $metadata")

                state.apply {
                    if (song_count == 0) {
                        clearQueue(save = false)
                        addMultipleToQueue(songs, 0)

                        if (metadata != null) {
                            seekToSong(metadata.queue_index.toInt())
                            seekTo(metadata.playback_position_ms)
                            pause()
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun isPopulatedQueueSaved(context: AppContext): Boolean {
            try {
                val queue: Pair<List<SongData>, PersistentQueueMetadata?> = getSavedQueue(context)
                return queue.second != null
            }
            catch (_: Throwable) {
                return false
            }
        }
    }
}

private suspend fun getSavedQueue(context: AppContext): Pair<List<SongData>, PersistentQueueMetadata?> = withContext(Dispatchers.PlatformIO) {
    val metadata: PersistentQueueMetadata? = context.database.persistentQueueMetadataQueries.get().executeAsOneOrNull()

    val items: List<SongData> =
        context.database.persistentQueueItemQueries
            .get()
            .executeAsList()
            .also {
                lazyAssert { it.sortedBy { item -> item.item_index } == it }
            }
            .map { item ->
                SongData(item.id)
            }

    return@withContext Pair(items, metadata)
}

private suspend fun clearSavedQueue(context: AppContext) = withContext(Dispatchers.PlatformIO) {
    context.database.transaction {
        context.database.persistentQueueMetadataQueries.clear()
        context.database.persistentQueueItemQueries.clear()
    }
}
