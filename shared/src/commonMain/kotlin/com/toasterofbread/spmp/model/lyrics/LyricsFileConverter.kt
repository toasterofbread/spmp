package com.toasterofbread.spmp.model.lyrics

import dev.toastbits.composekit.context.PlatformFile
import dev.toastbits.composekit.util.indexOfOrNull
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongRef
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.BufferedSink
import okio.use
import kotlin.time.Duration
import PlatformIO
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.app_name

// Reads and writes LRC files based on https://en.wikipedia.org/wiki/LRC_(file_format)
object LyricsFileConverter {
    fun getSongLyricsFileName(song: Song): String =
        "${song.id}.${getFileExtension()}"
    fun getFileExtension(): String = "lrc"

    private suspend fun BufferedSink.writeFileHeaders(lyrics: SongLyrics) {
        writeUtf8(
            buildString {
                appendLine("[src:${lyrics.reference.source_index}]")
                appendLine("[id:${lyrics.reference.id}]")

                appendLine("[re:${getString(Res.string.app_name)}]")
                appendLine("[ve:${ProjectBuildConfig.GIT_TAG ?: ProjectBuildConfig.GIT_COMMIT_HASH}]")
            }
        )
    }

    private fun timestampToString(time: Duration): String {
        val minutes: String = time.inWholeMinutes.toString().padStart(2, '0')
        val seconds: String = (time.inWholeSeconds % 60).toString().padStart(2, '0')
        val milliseconds: String = ((time.inWholeMilliseconds % 1000) / 100).toString().padStart(2, '0')
        return "$minutes:$seconds.$milliseconds"
    }

    private fun stringToTimestampMs(string: String): Long {
        val minutes_end: Int? = string.indexOfOrNull(':')
        val minutes: Int = minutes_end?.let {
            string.substring(0, it).toIntOrNull()
        } ?: 0

        val seconds: Float = string.substring((minutes_end ?: -1) + 1).toFloatOrNull() ?: 0f
        return (((minutes * 60) + seconds) * 1000).toLong()
    }

    suspend fun SongLyrics.saveToFile(
        file: PlatformFile,
        context: AppContext
    ): Result<Unit> = withContext(Dispatchers.PlatformIO) {
        val temp_file: PlatformFile = file.getSibling("${file.name}.tmp")
        check(temp_file.createFile()) { temp_file.toString() }

        return@withContext runCatching {
            temp_file.outputStream().buffer().use { stream ->
                stream.writeFileHeaders(this@saveToFile)

                stream.writeUtf8(
                    buildString {
                        for (line in lines) {
                            var first = true
                            for (term in line) {
                                if (first) {
                                    term.line_range?.start?.also { line_start ->
                                        val duration: Duration = with (Duration) { line_start.milliseconds }
                                        append("[${timestampToString(duration)}]")
                                    }
                                    first = false
                                }

                                term.start?.also { term_start ->
                                    if (term_start == term.line_range?.start) {
                                        return@also
                                    }
                                    val duration: Duration = with (Duration) { term_start.milliseconds }
                                    append("<${timestampToString(duration)}>")
                                }

                                for (subterm in term.subterms) {
                                    append(subterm.text)
                                }
                            }

                            append('\n')
                        }
                    }
                )

                stream.flush()
            }

            check(file.delete()) { "Deleting existing file failed ($file)" }
            check(temp_file.renameTo(file.name).is_file) { "Renaming temporary file failed ($temp_file -> $file)" }
        }
    }

    suspend fun loadFromFile(file: PlatformFile, context: AppContext): Pair<SongRef, SongLyrics>? = withContext(Dispatchers.PlatformIO) {
        val required_suffix: String = ".${getFileExtension()}"
        if (!file.name.endsWith(required_suffix)) {
            return@withContext null
        }

        val song = SongRef(file.name.dropLast(required_suffix.length))
        val lines: MutableList<List<SongLyrics.Term>> = mutableListOf()

        var lyrics_source: Int? = null
        var lyrics_id: String? = null

        file.inputStream().buffer().use { stream ->
            while (true) {
                val line: String = stream.readUtf8Line() ?: break
                if (line.isBlank()) {
                    continue
                }

                val line_text: String
                val line_timestamp: Long?

                // Parse metadata
                if (line.first() == '[') {
                    val metadata_end: Int = line.indexOf(']', 2)
                    if (metadata_end == -1) {
                        line_text = line
                        line_timestamp = null
                    }
                    else {
                        line_text = line.substring(metadata_end + 1).trimStart()

                        val metadata: String = line.substring(1, metadata_end)

                        // Timestamp
                        if (metadata.first().isDigit()) {
                            line_timestamp =
                                try {
                                    stringToTimestampMs(metadata)
                                }
                                catch (_: Throwable) {
                                    null
                                }
                        }
                        else {
                            line_timestamp = null

                            val split = metadata.split(':', limit = 2)
                            if (split.size > 1) {
                                when (split[0]) {
                                    "src" -> {
                                        if (lyrics_source == null) {
                                            lyrics_source = split[1].toIntOrNull()
                                        }
                                    }
                                    "id" -> {
                                        if (lyrics_id == null && split[1].isNotBlank()) {
                                            lyrics_id = split[1]
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    line_text = line
                    line_timestamp = null
                }

                val terms: MutableList<Pair<String, Long?>> = mutableListOf()

                val current_term = StringBuilder()
                var parsing_timestamp: Boolean = false
                var current_term_timestamp: Long? = null

                for (c in line_text) {
                    if (c == '<') {
                        if (current_term.isNotBlank()) {
                            terms.add(Pair(current_term.toString(), current_term_timestamp))
                        }
                        current_term.clear()
                        current_term_timestamp = null
                        parsing_timestamp = true
                    }
                    else if (c == '>' && parsing_timestamp) {
                        current_term_timestamp =
                            try {
                                stringToTimestampMs(current_term.toString())
                            }
                            catch (_: Throwable) {
                                null
                            }
                        current_term.clear()
                        parsing_timestamp = false
                    }
                    else {
                        current_term.append(c)
                    }
                }

                if (!parsing_timestamp && current_term.isNotBlank()) {
                    terms.add(Pair(current_term.toString(), current_term_timestamp))
                }

                if (terms.isEmpty()) {
                    continue
                }

                lines.add(
                    terms.map { term_data ->
                        SongLyrics.Term(
                            listOf(SongLyrics.Term.Text(term_data.first)),
                            -1,
                            start = term_data.second ?: line_timestamp,
                            end = null
                        )
                    }
                )
            }
        }

        checkNotNull(lyrics_source) { "Source not found" }
        checkNotNull(lyrics_id) { "ID not found" }

        var previous_time: Long? = null

        for ((i, line) in lines.asReversed().withIndex()) {
            val line_start = line.first().start ?: 0
            val line_end = previous_time ?: Long.MAX_VALUE

            for (term in line.asReversed()) {
                term.line_index = lines.size - i - 1
                term.line_range = line_start .. line_end

                term.end = previous_time ?: Long.MAX_VALUE
                previous_time = term.start
            }
        }

        return@withContext Pair(
            song,
            SongLyrics(
                LyricsReference(lyrics_source!!, lyrics_id!!, file),
                SongLyrics.SyncType.WORD_SYNC,
                lines
            )
        )
    }
}
