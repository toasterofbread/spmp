package com.toasterofbread.spmp.youtubeapi.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.db.mediaitem.LyricsById
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getStringTODO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class LyricsReference(val source_index: Int, val id: String, val local_file: PlatformFile? = null) {
    fun isNone(): Boolean = source_index < 0
    fun getUrl(): String? = LyricsSource.fromIdx(source_index).getUrlOfId(id)

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

    @Composable
    abstract fun getReadable(): String
    abstract fun getColour(): Color
    abstract fun getUrlOfId(id: String): String?

    abstract suspend fun getLyrics(lyrics_id: String, context: AppContext): Result<SongLyrics>

    open fun supportsLyricsBySong(): Boolean = false
    open suspend fun getReferenceBySong(song: Song, context: AppContext): Result<LyricsReference?> { throw NotImplementedError() }

    open fun supportsLyricsBySearching(): Boolean = true
    open suspend fun searchForLyrics(
        title: String,
        artist_name: String?,
        album_name: String?,
        duration: Duration?
    ): Result<List<SearchResult>> { throw NotImplementedError() }

    fun referenceOfSource(id: String): LyricsReference =
        LyricsReference(source_index, id)

    companion object {
        private val lyrics_sources: List<(Int) -> LyricsSource> = listOf(
            { KugouLyricsSource(it) },
            { PetitLyricsSource(it) },
	    { LrclibLyricsSource(it) },
            { YoutubeMusicLyricsSource(it) }
        )
        val SOURCE_AMOUNT: Int get() = lyrics_sources.size

        fun fromIdx(source_idx: Int): LyricsSource {
            require(source_idx >= 0)

            val creator = lyrics_sources[source_idx]
            return creator.invoke(source_idx)
        }

        inline fun iterateByPriority(
            default: Int,
            action: (LyricsSource) -> Unit
        ) {
            for (i in 0 until SOURCE_AMOUNT) {
                val source = fromIdx(if (i == 0) default else if (i > default) i - 1 else i)
                action(source)
            }
        }

        suspend fun searchSongLyricsByPriority(
            song: Song,
            context: AppContext,
            default: Int? = null
        ): Result<SongLyrics> = runCatching {
            val db: Database = context.database
            val song_title: String? = song.getActiveTitle(db)
            val artist_title: String? = song.Artists.get(db)?.firstOrNull()?.getActiveTitle(db)
            val album_title: String? = song.Album.get(db)?.getActiveTitle(db)
            val duration_ms: Long? = song.Duration.get(db)

            var fail_exception: Throwable? = null
            iterateByPriority(default ?: context.settings.Lyrics.DEFAULT_SOURCE.get()) { source ->
                var lyrics_reference: LyricsReference? = null

                if (source.supportsLyricsBySong()) {
                    lyrics_reference = source.getReferenceBySong(song, context).fold(
                        { it },
                        {
                            if (fail_exception == null) {
                                fail_exception = it
                            }
                            return@iterateByPriority
                        }
                    )
                }

                if (lyrics_reference == null && source.supportsLyricsBySearching()) {
                    if (song_title == null) {
                        if (fail_exception == null) {
                            fail_exception = RuntimeException("Song has no title to search by")
                        }
                        return@iterateByPriority
                    }

                    val result: SearchResult = source.searchForLyrics(
                        song_title,
                        artist_title,
                        album_title,
                        duration_ms?.milliseconds
                    ).fold(
                        { results ->
                            if (results.isEmpty()) {
                                fail_exception = null
                                null
                            }
                            else {
                                results.first()
                            }
                        },
                        {
                            if (fail_exception == null) {
                                fail_exception = it
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
                lyrics_result.fold(
                    { return@runCatching it },
                    { fail_exception = it }
                )
            }

            throw fail_exception ?: RuntimeException(getStringTODO("No lyrics found"))
        }
    }
}

suspend fun loadLyrics(reference: LyricsReference, context: AppContext): Result<SongLyrics> {
    require(!reference.isNone())

    val source: LyricsSource = LyricsSource.fromIdx(reference.source_index)
    return source.getLyrics(reference.id, context)
}

internal fun parseStaticLyrics(lyrics_text: String): List<List<SongLyrics.Term>> =
    lyrics_text.split('\n').map { line ->
        val split: List<String> = line.split(' ')
        if (split.all { it.isBlank() }) {
            return@map emptyList()
        }

        return@map split.mapIndexed { index, term ->
            val text: SongLyrics.Term.Text =
                SongLyrics.Term.Text(
                    if (index + 1 != split.size) term + ' '
                    else term
                )
            return@mapIndexed SongLyrics.Term(listOf(text), -1)
        }
    }
