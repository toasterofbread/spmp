package com.toasterofbread.spmp.youtubeapi.lyrics

import com.toasterofbread.spmp.model.lyrics.SongLyrics

fun interface LyricsFuriganaTokeniser {
    fun mergeAndFuriganiseTerms(terms: List<SongLyrics.Term>, romanise: Boolean): List<SongLyrics.Term>

    companion object {
        private var instance: LyricsFuriganaTokeniser? = null

        suspend fun getInstance(): LyricsFuriganaTokeniser? {
            if (instance == null) {
                instance = createFuriganaTokeniserImpl()
            }
            return instance
        }

    }
}

internal expect suspend fun createFuriganaTokeniserImpl(): LyricsFuriganaTokeniser?
