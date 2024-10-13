package com.toasterofbread.spmp.platform.download

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
import wseemann.media.FFmpegMediaMetadataRetriever

actual val LocalSongMetadataProcessor: MetadataProcessor =
    object : MetadataProcessor {
        override suspend fun addMetadataToLocalSong(song: Song, file: PlatformFile, file_extension: String, context: AppContext) =
            JAudioTaggerMetadataProcessor.addMetadataToLocalSong(song, file, file_extension, context)

        private val custom_metadata_keys: List<String> =
            JAudioTaggerMetadataProcessor.CUSTOM_METADATA_KEYS.map { key ->
                when (key) {
                    FieldKey.ALBUM_ARTIST -> FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM_ARTIST
                    FieldKey.COMMENT -> FFmpegMediaMetadataRetriever.METADATA_KEY_COMMENT
                    else -> throw NotImplementedError(key.name)
                }
            }

        override suspend fun readLocalSongMetadata(file: PlatformFile, context: AppContext, match_id: String?, load_data: Boolean): SongData? =
            withContext(Dispatchers.IO) {
                try {
                    val metadata_retriever: FFmpegMediaMetadataRetriever = FFmpegMediaMetadataRetriever()
                    metadata_retriever.setDataSource(context.ctx, Uri.parse(file.uri))

                    val custom_metadata: JAudioTaggerMetadataProcessor.CustomMetadata? =
                        custom_metadata_keys.firstNotNullOfOrNull { key ->
                            try {
                                Json.decodeFromString(metadata_retriever.extractMetadata(key)!!)
                            }
                            catch (_: Throwable) { null }
                        }

                    if (custom_metadata?.song_id == null || (match_id != null && custom_metadata.song_id != match_id)) {
                        return@withContext null
                    }

                    val song: SongData = SongData(custom_metadata.song_id)
                    if (load_data) {
                        song.name = metadata_retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE)

                        val artist_ids: List<String>? = custom_metadata.artist_ids ?: custom_metadata.artist_id?.let { listOf(it) }
                        song.artists = artist_ids?.map { ArtistData(it) } ?: song.getItemWithOrForTitle(null, metadata_retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST)) { ArtistData(it) }?.let { listOf(it) }

                        song.album = song.getItemWithOrForTitle(custom_metadata.album_id, metadata_retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM)) { RemotePlaylistData(it) }
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
