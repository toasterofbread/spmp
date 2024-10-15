package com.toasterofbread.spmp.youtubeapi.lyrics

import PlatformIO
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.lyrics_source_ytm

internal class YoutubeMusicLyricsSource(source_idx: Int): LyricsSource(source_idx) {
    @Composable
    override fun getReadable(): String = stringResource(Res.string.lyrics_source_ytm)
    override fun getColour(): Color = Color(0xFE0000)
    override fun getUrlOfId(id: String): String? = "https://music.youtube.com/watch?v=$id"

    override fun supportsLyricsBySong(): Boolean = true
    override fun supportsLyricsBySearching(): Boolean = false

    override suspend fun getReferenceBySong(song: Song, context: AppContext): Result<LyricsReference?> = withContext(Dispatchers.PlatformIO) {
        val browse_id = song.LyricsBrowseId.get(context.database)
        if (browse_id != null) {
            return@withContext Result.success(referenceOfSource(browse_id))
        }

        if (song.Loaded.get(context.database)) {
            return@withContext Result.success(null)
        }

        val loaded_item = MediaItemLoader.loadSong(song.getEmptyData(), context)
            .fold(
                { it },
                { return@withContext Result.failure(it) }
            )

        return@withContext Result.success(
            loaded_item.lyrics_browse_id?.let { browse_id ->
                referenceOfSource(browse_id)
            }
        )
    }

    override suspend fun getLyrics(lyrics_id: String, context: AppContext): Result<SongLyrics> = withContext(Dispatchers.PlatformIO) {
        val result = context.ytapi.SongLyrics.getSongLyrics(lyrics_id)
        return@withContext result.fold(
            { lyrics_text ->
                Result.success(
                    SongLyrics(
                        LyricsReference(source_index, lyrics_id),
                        SongLyrics.SyncType.NONE,
                        parseStaticLyrics(lyrics_text)
                    )
                )
            },
            { error ->
                Result.failure(error)
            }
        )
    }
}
