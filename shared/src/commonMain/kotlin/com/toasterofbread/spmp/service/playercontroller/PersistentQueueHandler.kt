package com.toasterofbread.spmp.service.playercontroller

import SpMp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException

private const val PERSISTENT_QUEUE_FILENAME: String = "persistent_queue"

private data class PersistentQueueMetadata(val song_index: Int, val position_ms: Long) {
    fun serialise(): String = "$song_index,$position_ms"
    companion object {
        fun deserialise(data: String): PersistentQueueMetadata {
            val split = data.split(",")
            return PersistentQueueMetadata(split[0].toInt(), split[1].toLong())
        }
    }
}

private suspend fun getSavedQueue(context: PlatformContext): Pair<List<SongData>, PersistentQueueMetadata> = withContext(Dispatchers.IO) {
    val reader: BufferedReader = context.openFileInput(PERSISTENT_QUEUE_FILENAME).bufferedReader()
    reader.use {
        val songs: MutableList<SongData> = mutableListOf()
        val metadata: PersistentQueueMetadata = PersistentQueueMetadata.deserialise(
            reader.readLine() ?: throw IOException("Empty file")
        )

        var line: String? = reader.readLine()
        while (line != null) {
            val song = SongData(line)
            songs.add(song)
            line = reader.readLine()
        }

        return@withContext Pair(songs, metadata)
    }
}

internal class PersistentQueueHandler(val player: PlayerServicePlayer, val context: PlatformContext) {
    private var persistent_queue_loaded: Boolean = false
    private val queue_lock = Mutex()

    private fun getPersistentQueueMetadata(): PersistentQueueMetadata =
        PersistentQueueMetadata(player.current_song_index, player.current_position_ms)

    suspend fun savePersistentQueue() {
        if (!persistent_queue_loaded || !Settings.KEY_PERSISTENT_QUEUE.get<Boolean>(context)) {
            return
        }

        val songs: MutableList<Song> = mutableListOf()
        val metadata: PersistentQueueMetadata

        withContext(Dispatchers.Main) {
            for (i in 0 until player.song_count) {
                val song = player.getSong(i)
                if (song != null) {
                    songs.add(song)
                }
            }
            metadata = getPersistentQueueMetadata()
        }

        withContext(Dispatchers.IO) {
            queue_lock.withLock {
                context.openFileOutput(PERSISTENT_QUEUE_FILENAME).bufferedWriter().use { writer ->
                    writer.write(metadata.serialise())
                    writer.newLine()

                    if (songs.isNotEmpty()) {
                        for (song in songs) {
                            writer.write(song.id)
                            writer.newLine()
                        }

                        SpMp.Log.info("savePersistentQueue: Saved ${songs.size} songs with data $metadata")
                    }
                }

                persistent_queue_loaded = true
            }
        }
    }

    suspend fun loadPersistentQueue() {
        if (player.song_count > 0) {
            SpMp.Log.info("loadPersistentQueue: Skipping, queue already populated")
            persistent_queue_loaded = true
            return
        }

        withContext(Dispatchers.IO) {
            if (!Settings.KEY_PERSISTENT_QUEUE.get<Boolean>(context)) {
                SpMp.Log.info("loadPersistentQueue: Skipping, feature disabled")
                context.deleteFile(PERSISTENT_QUEUE_FILENAME)
                return@withContext
            }

            if (!queue_lock.tryLock()) {
                SpMp.Log.info("loadPersistentQueue: Skipping, lock already acquired")
                return@withContext
            }

            val songs: List<Song>
            val metadata: PersistentQueueMetadata

            try {
                if (persistent_queue_loaded) {
                    SpMp.Log.info("loadPersistentQueue: Skipping, queue already loaded")
                    return@withContext
                }

                val queue = getSavedQueue(context)
                songs = queue.first
                metadata = queue.second

                if (songs.isEmpty()) {
                    SpMp.Log.info("loadPersistentQueue: Saved queue is empty")
                    return@withContext
                }

                val request_limit = Semaphore(10)
                val jobs: MutableList<Job> = mutableListOf()

                for (song in songs) {
                    if (!song.Loaded.get(context.database)) {
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
            catch (_: FileNotFoundException) {
                SpMp.Log.info("loadPersistentQueue: No file found")
                persistent_queue_loaded = true
                return@withContext
            }
            catch (e: Throwable) {
                SpMp.Log.info("loadPersistentQueue: Failed with $e")
                throw RuntimeException(e)
            }
            finally {
                queue_lock.unlock()
                persistent_queue_loaded = true
            }

            withContext(Dispatchers.Main) {
                SpMp.Log.info("loadPersistentQueue: Adding ${songs.size} songs to $metadata")

                player.apply {
                    if (player.song_count == 0) {
                        clearQueue(save = false)
                        addMultipleToQueue(songs, 0)
                        player.seekToSong(metadata.song_index)
                        player.seekTo(metadata.position_ms)
                        player.pause()
                    }
                }
            }
        }
    }

    companion object {
        suspend fun isPopulatedQueueSaved(context: PlatformContext): Boolean {
            try {
                val queue = getSavedQueue(context)
                return queue.first.isNotEmpty()
            }
            catch (_: Throwable) {
                return false
            }
        }
    }
}
