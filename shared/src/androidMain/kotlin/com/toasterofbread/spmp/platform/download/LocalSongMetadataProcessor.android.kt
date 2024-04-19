package com.toasterofbread.spmp.platform.download

import android.media.MediaMetadataRetriever
import android.net.Uri
import dev.toastbits.composekit.platform.PlatformFile
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jaudiotagger.tag.FieldKey

actual val LocalSongMetadataProcessor: MetadataProcessor =
    object : MetadataProcessor {
        override suspend fun addMetadataToLocalSong(song: Song, file: PlatformFile, file_extension: String, context: AppContext) =
            JAudioTaggerMetadataProcessor.addMetadataToLocalSong(song, file, file_extension, context)

        private val custom_metadata_key: Int get() =
            when (JAudioTaggerMetadataProcessor.CUSTOM_METADATA_KEY) {
                FieldKey.ALBUM_ARTIST -> MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
                else -> throw NotImplementedError(JAudioTaggerMetadataProcessor.CUSTOM_METADATA_KEY.name)
            }

        override suspend fun readLocalSongMetadata(file: PlatformFile, context: AppContext, match_id: String?, load_data: Boolean): SongData? =
            withContext(Dispatchers.IO) {
                try {
                    val metadata_retriever: MediaMetadataRetriever = MediaMetadataRetriever()
                    metadata_retriever.setDataSource(context.ctx, Uri.parse(file.uri))

                    val custom_metadata: JAudioTaggerMetadataProcessor.CustomMetadata? =
                        try {
                            Json.decodeFromString(metadata_retriever.extractMetadata(custom_metadata_key)!!)
                        }
                        catch (_: Throwable) { null }

                    if (custom_metadata?.song_id == null || (match_id != null && custom_metadata.song_id != match_id)) {
                        return@withContext null
                    }

                    val song: SongData = SongData(custom_metadata.song_id)
                    if (load_data) {
                        song.name = metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

                        val artist_ids: List<String>? = custom_metadata.artist_ids ?: custom_metadata.artist_id?.let { listOf(it) }
                        song.artists = artist_ids?.map { ArtistData(it) } ?: song.getItemWithOrForTitle(null, metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)) { ArtistData(it) }?.let { listOf(it) }

                        song.album = song.getItemWithOrForTitle(custom_metadata.album_id, metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)) { RemotePlaylistData(it) }
                    }

                    return@withContext song
                }
                catch (e: Throwable) {
                    val error: Throwable = RuntimeException("Reading metadata failed for $file ${file.uri}", e)
                    error.printStackTrace()
                    throw error
                }
            }
    }
