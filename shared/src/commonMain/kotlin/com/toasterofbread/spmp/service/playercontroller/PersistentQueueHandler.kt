package com.toasterofbread.spmp.service.playercontroller

import SpMp
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.PlatformContext
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

internal class PersistentQueueHandler(val player: PlayerController, val context: PlatformContext) {
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

                        SpMp.Log.info("savePersistentQueue saved ${songs.size} songs with data $metadata")
                    }
                }

                persistent_queue_loaded = true
            }
        }
    }

    suspend fun loadPersistentQueue() {
        if (player.song_count != 0) {
            persistent_queue_loaded = true
            return
        }

        withContext(Dispatchers.IO) {
            if (!Settings.KEY_PERSISTENT_QUEUE.get<Boolean>(context)) {
                context.deleteFile(PERSISTENT_QUEUE_FILENAME)
                return@withContext
            }

            if (!queue_lock.tryLock()) {
                persistent_queue_loaded = true
                return@withContext
            }

            val songs: MutableList<Song>
            val metadata: PersistentQueueMetadata

            try {
                if (persistent_queue_loaded) {
                    return@withContext
                }

                val reader: BufferedReader
                try {
                    reader = context.openFileInput(PERSISTENT_QUEUE_FILENAME).bufferedReader()
                } catch (_: FileNotFoundException) {
                    SpMp.Log.info("loadPersistentQueue no file found")
                    persistent_queue_loaded = true
                    return@withContext
                }

                coroutineContext.job.invokeOnCompletion {
                    reader.close()
                }

                metadata = PersistentQueueMetadata.deserialise(reader.readLine() ?: return@withContext)

                songs = mutableListOf()
                val request_limit = Semaphore(10)
                val jobs: MutableList<Job> = mutableListOf()

                var line: String? = reader.readLine()

                while (line != null) {
                    val song = SongData(line)
                    songs.add(song)

                    if (!song.Loaded.get(context.database)) {
                        jobs.add(
                            launch {
                                request_limit.withPermit {
                                    MediaItemLoader.loadSong(song, context)
                                }
                            }
                        )
                    }

                    line = reader.readLine()
                }

                if (songs.isEmpty()) {
                    return@withContext
                }

                jobs.joinAll()

            } finally {
                queue_lock.unlock()
                persistent_queue_loaded = true
            }

            withContext(Dispatchers.Main) {
                SpMp.Log.info("loadPersistentQueue adding ${songs.size} songs to $metadata")

                player.apply {
                    @Suppress("KotlinConstantConditions")
                    if (song_count == 0) {
                        clearQueue(save = false)
                        addMultipleToQueue(songs, 0)
                        seekToSong(metadata.song_index)
                        seekTo(metadata.position_ms)
                    }
                }
            }
        }
    }
}
