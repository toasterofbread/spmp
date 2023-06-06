package com.spectre7.spmp.model

import androidx.compose.ui.graphics.Color
import com.spectre7.spmp.resources.getString
import com.spectre7.utils.lazyAssert
import com.spectre7.utils.toHiragana

data class SongLyrics(
    val id: Int,
    val source: Source,
    val sync_type: SyncType,
    val lines: List<List<Term>>
) {
    val synced: Boolean get() = sync_type != SyncType.NONE

    enum class Source {
        PETITLYRICS;

        val readable: String
            get() = when (this) {
                PETITLYRICS -> getString("lyrics_source_petitlyrics")
            }

        val colour: Color
            get() = when (this) {
                PETITLYRICS -> Color(0xFFBD0A0F)
            }
    }

    enum class SyncType {
        NONE,
        LINE_SYNC,
        WORD_SYNC;

        val readable: String
            get() = when (this) {
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
                require(text.isNotBlank())

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
            for (line in lines) {
                for (term in line) {
                    if (sync_type != SyncType.NONE && (term.start == null || term.end == null)) {
                        return@lazyAssert false
                    }
                }
            }
            return@lazyAssert true
        }
    }
}
