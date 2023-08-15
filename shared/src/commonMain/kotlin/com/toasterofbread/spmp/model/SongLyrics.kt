package com.toasterofbread.spmp.model

import com.toasterofbread.spmp.api.lyrics.LyricsReference
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.utils.lazyAssert
import com.toasterofbread.utils.toHiragana

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

        fun getReadable(): String =
            when (this) {
                NONE -> getString("lyrics_sync_none")
                LINE_SYNC -> getString("lyrics_sync_line")
                WORD_SYNC -> getString("lyrics_sync_word")
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
                return values().toList().reversed()
            }
        }
    }

    data class Term(
        val subterms: List<Text>,
        var line_index: Int,
        val start: Long? = null,
        val end: Long? = null
    ) {
        var line_range: LongRange? = null
        var data: Any? = null

        data class Text(val text: String, var furi: String? = null) {
            init {
                require(text.isNotEmpty())

                if (furi != null) {
                    if (furi == "*") {
                        this.furi = null
                    }
                    else {
                        furi = furi!!.toHiragana()
                        if (furi == text.toHiragana()) {
                            furi = null
                        }
                    }
                }
            }
        }

        val range: LongRange
            get() = start!! .. end!!

    }

    init {
        lazyAssert {
            synchronized(lines) {
                for (line in lines) {
                    for (term in line) {
                        if (sync_type != SyncType.NONE && (term.start == null || term.end == null)) {
                            return@lazyAssert false
                        }
                    }
                }
            }
            return@lazyAssert true
        }
    }
}
