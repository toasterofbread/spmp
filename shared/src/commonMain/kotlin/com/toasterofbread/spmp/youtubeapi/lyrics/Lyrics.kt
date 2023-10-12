package com.toasterofbread.spmp.youtubeapi.lyrics

import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getStringTODO
import mediaitem.LyricsById

data class LyricsReference(val source_index: Int, val id: String) {
    fun isNone(): Boolean = source_index < 0

    companion object {
        val NONE: LyricsReference = LyricsReference(-1, "")
    }
}

fun LyricsById?.toLyricsReference(): LyricsReference? =
    if (this?.lyrics_source != null && lyrics_id != null) LyricsReference(lyrics_source.toInt(), lyrics_id)
    else null

sealed class LyricsSource(val source_index: Int) {
    data class SearchResult(
        var id: String,
        var name: String,
        var sync_type: SongLyrics.SyncType,
        var artist_name: String?,
        var album_name: String?
    )
    
    abstract fun getReadable(): String
    abstract fun getColour(): Color

    open fun supportsLyricsBySong(): Boolean = false
    open suspend fun getReferenceBySong(song: Song, context: PlatformContext): Result<LyricsReference?> { throw NotImplementedError() }

    open fun supportsLyricsBySearching(): Boolean = true
    abstract suspend fun getLyrics(lyrics_id: String, context: PlatformContext): Result<SongLyrics>
    abstract suspend fun searchForLyrics(title: String, artist_name: String? = null): Result<List<SearchResult>>

    fun referenceOfSource(id: String): LyricsReference =
        LyricsReference(source_index, id)

    companion object {
        private val lyrics_sources: List<(Int) -> LyricsSource> = listOf(
            { KugouLyricsSource(it) },
            { PetitLyricsSource(it) },
            { YoutubeMusicLyricsSource(it) }
        )
        val SOURCE_AMOUNT: Int get() = lyrics_sources.size

        fun fromIdx(source_idx: Int): LyricsSource {
            require(source_idx >= 0)

            val creator = lyrics_sources[source_idx]
            return creator.invoke(source_idx)
        }

        inline fun iterateByPriority(
            default: Int = Settings.KEY_LYRICS_DEFAULT_SOURCE.get(),
            action: (LyricsSource) -> Unit
        ) {
            for (i in 0 until SOURCE_AMOUNT) {
                val source = fromIdx(if (i == 0) default else if (i > default) i - 1 else i)
                action(source)
            }
        }

        suspend fun searchSongLyricsByPriority(
            song: Song,
            context: PlatformContext,
            default: Int = Settings.KEY_LYRICS_DEFAULT_SOURCE.get()
        ): Result<SongLyrics> {
            val db = context.database
            val (song_title, artist_title) = db.transactionWithResult {
                Pair(
                    song.getActiveTitle(db),
                    song.Artist.get(db)?.getActiveTitle(db)
                )
            }

            var fail_result: Result<SongLyrics>? = null
            iterateByPriority(default) { source ->
                var lyrics_reference: LyricsReference? = null

                if (source.supportsLyricsBySong()) {
                    lyrics_reference = source.getReferenceBySong(song, context).fold(
                        { it },
                        {
                            if (fail_result == null) {
                                fail_result = Result.failure(it)
                            }
                            return@iterateByPriority
                        }
                    )
                }

                if (lyrics_reference == null && source.supportsLyricsBySearching()) {
                    if (song_title == null) {
                        if (fail_result == null) {
                            fail_result = Result.failure(RuntimeException("Song has no title to search by"))
                        }
                        return@iterateByPriority
                    }

                    val result: SearchResult = source.searchForLyrics(song_title, artist_title).fold(
                        { results ->
                            if (results.isEmpty()) {
                                fail_result = null
                                null
                            }
                            else {
                                results.first()
                            }
                        },
                        {
                            if (fail_result == null) {
                                fail_result = Result.failure(it)
                            }
                            null
                        }
                    ) ?: return@iterateByPriority

                    lyrics_reference = LyricsReference(source.source_index, result.id)
                }

                if (lyrics_reference == null) {
                    throw NotImplementedError(source::class.toString())
                }

                val lyrics_result = SongLyricsLoader.loadByLyrics(lyrics_reference, context)
                if (lyrics_result.isSuccess) {
                    return lyrics_result
                }

                if (fail_result == null) {
                    fail_result = lyrics_result
                }
            }

            return fail_result ?: Result.failure(RuntimeException(getStringTODO("No lyrics found")))
        }
    }
}

suspend fun loadLyrics(reference: LyricsReference, context: PlatformContext): Result<SongLyrics> {
    require(!reference.isNone())

    val source = LyricsSource.fromIdx(reference.source_index)
    return source.getLyrics(reference.id, context)
}

internal fun parseStaticLyrics(lyrics_text: String): List<List<SongLyrics.Term>> {
    val tokeniser = createTokeniser()
    return lyrics_text.split('\n').map { line ->
        val terms: MutableList<SongLyrics.Term> = mutableListOf()
        val split = line.split(' ')

        if (split.any { it.isNotBlank() }) {
            for (term in split.withIndex()) {
                val text = SongLyrics.Term.Text(
                    if (term.index + 1 != split.size) term.value + ' '
                    else term.value
                )
                terms.add(SongLyrics.Term(listOf(text), -1))
            }
        }

        mergeAndFuriganiseTerms(tokeniser, terms)
    }
}
