package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.platform.AppContext

internal suspend fun getTermRangeOfTime(context: AppContext, lyrics: SongLyrics, time: Long): Pair<IntRange?, Long> {
    require(lyrics.synced)

    val enable_word_sync: Boolean = context.settings.Lyrics.ENABLE_WORD_SYNC.get()

    var start: Int? = null
    var end: Int? = null
    var last_before: Int? = null
    var next: Long = Long.MAX_VALUE

    for (line in lyrics.lines.withIndex()) {
        for (term in line.value.withIndex()) {
            val range: LongRange =
                if (lyrics.sync_type == SongLyrics.SyncType.WORD_SYNC && !enable_word_sync) {
                    term.value.line_range ?: term.value.range
                }
                else {
                    term.value.range
                }

            if (range.contains(time)) {
                end = line.index
                if (start == null) {
                    start = end
                }
            }
            else if (start != null) {
                if (term.value.start!! > time) {
                    next = term.value.start!! - time
                }
                break
            }
            else if (time > range.last) {
                last_before = line.index
            }
        }
    }

    if (start != null) {
        return Pair(start .. end!!, next)
    }
    else if (last_before != null) {
        return Pair(last_before .. last_before, next)
    }

    return Pair(null, next)
}
