package com.toasterofbread.spmp.model.lyrics

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsReference
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_sync_line
import spmp.shared.generated.resources.lyrics_sync_none
import spmp.shared.generated.resources.lyrics_sync_word

data class SongLyrics(
    val reference: LyricsReference,
    val sync_type: SyncType,
    val lines: List<List<Term>>
) {
    val synced: Boolean get() = sync_type != SyncType.NONE
    val id: String get() = reference.id
    val source_idx: Int get() = reference.source_index

    enum class SyncType {
        NONE,
        LINE_SYNC,
        WORD_SYNC;

        @Composable
        fun getReadable(): String =
            when (this) {
                NONE -> stringResource(Res.string.lyrics_sync_none)
                LINE_SYNC -> stringResource(Res.string.lyrics_sync_line)
                WORD_SYNC -> stringResource(Res.string.lyrics_sync_word)
            }

        companion object {
            fun fromKey(key: String): SyncType {
                return when (key) {
                    "text" -> NONE
                    "line_sync" -> LINE_SYNC
                    "text_sync" -> WORD_SYNC
                    else -> throw NotImplementedError(key)
                }
            }

            fun byPriority(): List<SyncType> {
                return entries.toList().reversed()
            }
        }
    }

    data class Term(
        val subterms: List<Text>,
        var line_index: Int,
        val start: Long? = null,
        var end: Long? = null
    ) {
        var line_range: LongRange? = null
        var data: Any? = null

        data class Text(val text: String, var reading: String? = null)

        val range: LongRange
            get() = start!! .. end!!
    }

    // init {
    //     lazyAssert {
    //         synchronized(lines) {
    //             for (line in lines) {
    //                 for (term in line) {
    //                     if (sync_type != SyncType.NONE && (term.start == null || term.end == null)) {
    //                         return@lazyAssert false
    //                     }
    //                 }
    //             }
    //         }
    //         return@lazyAssert true
    //     }
    // }
}
