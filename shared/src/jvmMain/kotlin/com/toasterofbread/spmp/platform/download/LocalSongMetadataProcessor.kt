package com.toasterofbread.spmp.platform.download

import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import dev.toastbits.composekit.context.PlatformFile
import com.toasterofbread.spmp.platform.AppContext

expect val LocalSongMetadataProcessor: MetadataProcessor

interface MetadataProcessor {
    suspend fun addMetadataToLocalSong(song: Song, file: PlatformFile, file_extension: String, context: AppContext)
    suspend fun readLocalSongMetadata(file: PlatformFile, context: AppContext, match_id: String? = null, load_data: Boolean = true): SongData?
}
