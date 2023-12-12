package com.toasterofbread.spmp.platform.download

import android.media.MediaMetadataRetriever
import android.net.Uri
import com.toasterofbread.composekit.platform.PlatformFile
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

                    return@withContext SongData(custom_metadata.song_id).apply {
                        if (!load_data) {
                            return@apply
                        }

                        title = metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        artist = getItemWithOrForTitle(custom_metadata.artist_id, metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)) { ArtistData(it) }
                        album = getItemWithOrForTitle(custom_metadata.album_id, metadata_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)) { RemotePlaylistData(it) }
                    }
                }
                catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                }
            }
    }
