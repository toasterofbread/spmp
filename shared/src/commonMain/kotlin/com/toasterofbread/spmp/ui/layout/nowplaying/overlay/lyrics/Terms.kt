@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.LyricsSettings
import com.toasterofbread.spmp.ui.component.AnnotatedReadingTerm
import com.toasterofbread.spmp.ui.component.ReadingTextData

internal fun List<ReadingTextData>.getTermRangeOfTime(lyrics: SongLyrics, time: Long): Pair<IntRange?, Long> {
    require(lyrics.synced)

    var start = -1
    var end = -1
    var next = Long.MAX_VALUE
    var last_before: Int? = null

    for (item in withIndex()) {
        val term = (item.value.data ?: continue) as SongLyrics.Term

        val range =
            if (lyrics.sync_type == SongLyrics.SyncType.WORD_SYNC && !Settings.get<Boolean>(LyricsSettings.Key.ENABLE_WORD_SYNC)) {
                term.line_range ?: term.range
            }
            else {
                term.range
            }

        if (range.contains(time)) {
            if (start == -1) {
                start = item.index
            }
            end = item.index
        }
        else if (start != -1) {
            if (term.start!! > time) {
                next = term.start - time
            }
            break
        }
        else if (time > range.last) {
            last_before = item.index
        }
    }

    if (start != -1) {
        return Pair(start .. end, next)
    }
    else if (last_before != null) {
        for (i in last_before - 1 downTo 0) {
            if (get(i).text.contains('\n')) {
                return Pair(i + 1 .. last_before, next)
            }
        }
    }

    return Pair(null, next)
}

internal fun SongLyrics.getReadingTerms(): MutableList<ReadingTextData> =
    mutableListOf<ReadingTextData>().apply {
        synchronized(lines) {
            for (line in lines) {
                if (line.isEmpty()) {
                    add(ReadingTextData("\n"))
                    continue
                }

                for (term in line.withIndex()) {
                    for (subterm in term.value.subterms.withIndex()) {
                        if (subterm.index + 1 == term.value.subterms.size && term.index + 1 == line.size) {
                            add(ReadingTextData(subterm.value.text + "\n", subterm.value.reading, term.value))
                        }
                        else {
                            add(ReadingTextData(subterm.value.text, subterm.value.reading, term.value))
                        }
                    }
                }
            }
        }
    }

internal fun List<AnnotatedReadingTerm>.getLineIndexOfTerm(term_index: Int): Int {
    var term_count = 0
    for (line in withIndex()) {
        val line_terms = line.value.annotated_string.annotations?.size ?: 0
        if (term_index < term_count + line_terms) {
            return line.index
        }
        term_count += line_terms
    }
    throw IndexOutOfBoundsException(term_index)
}
