package com.toasterofbread.spmp.youtubeapi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.AppUiString
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.artist.toArtistData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.playlist.toRemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.song.SongData
import com.toasterofbread.spmp.model.mediaitem.song.toSongData
import com.toasterofbread.spmp.model.mediaitem.toMediaItemData
import com.toasterofbread.spmp.model.settings.category.VideoFormatsEndpointType
import com.toasterofbread.spmp.model.settings.category.instantiate
import com.toasterofbread.spmp.model.settings.unpackSetData
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.getDataLanguage
import com.toasterofbread.spmp.resources.Language
import dev.toastbits.composekit.settings.PlatformSettingsListener
import dev.toastbits.ytmkt.endpoint.ArtistWithParamsRow
import dev.toastbits.ytmkt.endpoint.SearchResults
import dev.toastbits.ytmkt.endpoint.SongFeedLoadResult
import dev.toastbits.ytmkt.formats.VideoFormatsEndpoint
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiAuthenticationState
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMArtistWithParamsEndpoint
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMGetSongFeedEndpoint
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMLoadArtistEndpoint
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMLoadPlaylistEndpoint
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMLoadSongEndpoint
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMSearchEndpoint
import dev.toastbits.ytmkt.impl.youtubei.endpoint.YTMSongRelatedContentEndpoint
import dev.toastbits.ytmkt.model.ApiAuthenticationState
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import dev.toastbits.ytmkt.model.external.RelatedGroup
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.radio.BuiltInRadioContinuation
import dev.toastbits.ytmkt.radio.RadioContinuation
import dev.toastbits.ytmkt.uistrings.UiString
import kotlinx.coroutines.launch

internal class SpMpYoutubeiApi(
    val context: AppContext,
    api_url: String,
    data_language: Language
): YoutubeiApi(
    api_url = api_url,
    item_cache = SpMpItemCache(context.database)
) {
    private var _data_language: String = data_language.identifier
    override val data_language: String
        get() = _data_language

    private var _VideoFormats: VideoFormatsEndpoint = VideoFormatsEndpointType.DEFAULT.instantiate(this)
    override val VideoFormats: VideoFormatsEndpoint
        get() = _VideoFormats

    override var user_auth_state: YoutubeiAuthenticationState? by mutableStateOf(null)

    private val prefs_listener: PlatformSettingsListener =
        PlatformSettingsListener { key ->
            when (key) {
                context.settings.YoutubeAuth.YTM_AUTH.key -> context.coroutineScope.launch { user_auth_state = getCurrentUserAuthState() }
                context.settings.Interface.DATA_LOCALE.key -> context.coroutineScope.launch { _data_language = context.getDataLanguage().toTag() }
                context.settings.Streaming.VIDEO_FORMATS_METHOD.key -> context.coroutineScope.launch {
                    _VideoFormats = context.settings.Streaming.VIDEO_FORMATS_METHOD.get().instantiate(this@SpMpYoutubeiApi)
                }
            }
        }

    init {
        context.getPrefs().addListener(prefs_listener)

        context.coroutineScope.launch {
            user_auth_state = getCurrentUserAuthState()
        }
        context.coroutineScope.launch {
            _data_language = context.getDataLanguage().toTag()
        }
        context.coroutineScope.launch {
            _VideoFormats = context.settings.Streaming.VIDEO_FORMATS_METHOD.get().instantiate(this@SpMpYoutubeiApi)
        }
    }

    private suspend fun getCurrentUserAuthState(): SpMpYoutubeiAuthenticationState? =
        ApiAuthenticationState.unpackSetData(context.settings.YoutubeAuth.YTM_AUTH.get(), context)
            ?.let { data ->
                SpMpYoutubeiAuthenticationState(context.database, this, data.first, data.second)
            }

    // // -- User auth ---
    // override val YoutubeChannelCreationForm = YTMYoutubeChannelCreationFormEndpoint(this)
    // override val CreateYoutubeChannel = YTMCreateYoutubeChannelEndpoint(this)

    // --- Item loading ---
    inner class LoadSongDataEndpoint: YTMLoadSongEndpoint(this) {
        override suspend fun loadSong(song_id: String): Result<YtmSong> = runCatching {
            val song: YtmSong = super.loadSong(song_id).getOrThrow()
            song.toSongData().also { data ->
                data.loaded = true
                data.saveToDatabase(database, subitems_uncertain = true)
            }
            return@runCatching song
        }

        suspend fun loadSongData(song_id: String, save: Boolean = true): Result<SongData> = runCatching {
            val song: SongData = super.loadSong(song_id).getOrThrow().toSongData()
            song.loaded = true
            if (save) {
                song.saveToDatabase(database, subitems_uncertain = true)
            }
            return@runCatching song
        }
    }
    override val LoadSong: LoadSongDataEndpoint = LoadSongDataEndpoint()

    inner class LoadArtistDataEndpoint: YTMLoadArtistEndpoint(this) {
        override suspend fun loadArtist(artist_id: String): Result<YtmArtist> = runCatching {
            val artist: YtmArtist = super.loadArtist(artist_id).getOrThrow()
            artist.toArtistData().also { data ->
                data.loaded = true
                data.saveToDatabase(database, subitems_uncertain = true)
            }
            return@runCatching artist
        }

        suspend fun loadArtistData(artist_id: String, save: Boolean = true): Result<ArtistData> = runCatching {
            val artist: ArtistData = super.loadArtist(artist_id).getOrThrow().toArtistData()
            artist.loaded = true
            if (save) {
                artist.saveToDatabase(database, subitems_uncertain = true)
            }
            return@runCatching artist
        }
    }
    override val LoadArtist: LoadArtistDataEndpoint = LoadArtistDataEndpoint()

    inner class LoadPlaylistDataEndpoint: YTMLoadPlaylistEndpoint(this) {
        override suspend fun loadPlaylist(
            playlist_id: String,
            continuation: BuiltInRadioContinuation?,
            browse_params: String?,
            playlist_url: String?,
            use_non_music_api: Boolean
        ): Result<YtmPlaylist> = runCatching {
            val playlist: YtmPlaylist = super.loadPlaylist(playlist_id, continuation, browse_params, playlist_url, use_non_music_api).getOrThrow()
            playlist.toRemotePlaylistData().also { data ->
                data.loaded = true
                data.saveToDatabase(
                    database,
                    uncertain = data.playlist_type != PlaylistType.PLAYLIST,
                    subitems_uncertain = true
                )
            }
            return@runCatching playlist
        }

        suspend fun loadPlaylistData(
            playlist_id: String,
            continuation: BuiltInRadioContinuation? = null,
            browse_params: String? = null,
            playlist_url: String? = null,
            use_non_music_api: Boolean = false,
            save: Boolean = false
        ): Result<RemotePlaylistData> = runCatching {
            val playlist: RemotePlaylistData = super.loadPlaylist(playlist_id, continuation, browse_params, playlist_url, use_non_music_api).getOrThrow().toRemotePlaylistData()
            playlist.loaded = true
            if (save) {
                playlist.saveToDatabase(
                    database,
                    uncertain = playlist.playlist_type != PlaylistType.PLAYLIST,
                    subitems_uncertain = true
                )
            }
            return@runCatching playlist
        }
    }
    override val LoadPlaylist: LoadPlaylistDataEndpoint = LoadPlaylistDataEndpoint()

    // // --- Video formats ---
    // override val VideoFormats: VideoFormatsEndpoint = YoutubeiVideoFormatsEndpoint(this)

    // --- Feed ---
    override val SongFeed = object : YTMGetSongFeedEndpoint(this) {
        override suspend fun getSongFeed(
            min_rows: Int,
            params: String?,
            continuation: String?
        ): Result<SongFeedLoadResult> = runCatching {
            val load_result: SongFeedLoadResult = super.getSongFeed(min_rows, params, continuation).getOrThrow()

            performTransaction {
                for (layout in load_result.layouts) {
                    for (item in layout.items) {
                        item.toMediaItemData().saveToDatabase(database)
                    }
                }
            }

            return@runCatching load_result
        }

        override fun getMusicBrowseIdRowTitle(browse_id: String): UiString? =
            when (browse_id) {
                "FEmusic_listen_again" -> AppUiString("home_feed_listen_again")
                "FEmusic_mixed_for_you" -> AppUiString("home_feed_mixed_for_you")
                "FEmusic_new_releases_albums" -> AppUiString("home_feed_new_releases")
                "FEmusic_moods_and_genres" -> AppUiString("home_feed_moods_and_genres")
                "FEmusic_charts" -> AppUiString("home_feed_charts")
                else -> null
            }

        override fun getMusicBrowseIdRowType(browse_id: String): ItemLayoutType? =
            when (browse_id) {
                "FEmusic_listen_again" -> ItemLayoutType.GRID_ALT
                else -> null
            }
    }
    // override val GenericFeedViewMorePage = YTMGenericFeedViewMorePageEndpoint(this)
    // override val SongRadio = YTMSongRadioEndpoint(this)

    // --- Artists ---
    override val ArtistWithParams = object : YTMArtistWithParamsEndpoint(this) {
        override suspend fun loadArtistWithParams(
            browse_params: YoutubePage.BrowseParamsData
        ): Result<List<ArtistWithParamsRow>> = runCatching {
            val rows: List<ArtistWithParamsRow> = super.loadArtistWithParams(browse_params).getOrThrow()

            performTransaction {
                for (row in rows) {
                    for (item in row.items) {
                        item.toMediaItemData().saveToDatabase(database, subitems_uncertain = true)
                    }
                }
            }

            return@runCatching rows
        }
    }
    // override val ArtistRadio: ArtistRadioEndpoint = YTMArtistRadioEndpoint(this)
    // override val ArtistShuffle: ArtistShuffleEndpoint = YTMArtistShuffleEndpoint(this)

    // // --- Playlists ---
    // override val PlaylistContinuation = YTMPlaylistContinuationEndpoint(this)

    // // --- Search ---
    override val Search = object : YTMSearchEndpoint(this) {
        override suspend fun search(
            query: String,
            params: String?,
            non_music: Boolean
        ): Result<SearchResults> = runCatching {
            val results: SearchResults = super.search(query, params, non_music).getOrThrow()
            performTransaction {
                for (category in results.categories) {
                    for (item in category.first.items) {
                        item.toMediaItemData().saveToDatabase(database)
                    }
                }
            }
            return@runCatching results
        }
    }
    // override val SearchSuggestions = YTMSearchSuggestionsEndpoint(this)

    // // --- Radio builder ---
    // override val RadioBuilder = YTMRadioBuilderEndpoint(this)

    // // --- Song content ---
    override val SongRelatedContent = object : YTMSongRelatedContentEndpoint(this) {
        override suspend fun getSongRelated(
            song_id: String
        ): Result<List<RelatedGroup>> = runCatching {
            val related: List<RelatedGroup> = super.getSongRelated(song_id).getOrThrow()
            performTransaction {
                for (group in related) {
                    for (item in group.items ?: emptyList()) {
                        item.toMediaItemData().saveToDatabase(database)
                    }
                }
            }
            return@runCatching related
        }
    }
    // override val SongLyrics = YTMSongLyricsEndpoint(this)

    private val database: Database = context.database
    private suspend fun performTransaction(action: () -> Unit) {
        database.transaction {
            action()
        }
    }
}
