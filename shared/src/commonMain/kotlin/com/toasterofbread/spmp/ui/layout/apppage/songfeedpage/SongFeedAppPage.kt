package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.utils.common.anyCauseIs
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.SongFeedCache
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.FeedSettings
import com.toasterofbread.spmp.model.settings.mutableSettingsState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.FormFactor
import com.toasterofbread.spmp.platform.form_factor
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.FeedLoadState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedLoadResult
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal const val ARTISTS_ROW_DEFAULT_MIN_OCCURRENCES: Int = 2
internal const val ARTISTS_ROW_MIN_ARTISTS: Int = 4

class SongFeedAppPage(override val state: AppPageState): AppPage() {
    internal val scroll_state: LazyListState = LazyListState()

    internal val feed_endpoint: HomeFeedEndpoint = state.context.ytapi.HomeFeed

    internal var load_state by mutableStateOf(FeedLoadState.PREINIT)
    internal var load_error: Throwable? by mutableStateOf(null)
    internal val load_lock: Mutex = Mutex()
    internal val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    internal var continuation: String? by mutableStateOf(null)
    internal var layouts: List<MediaItemLayout>? by mutableStateOf(null)
    internal var filter_chips: List<FilterChip>? by mutableStateOf(null)
    internal var selected_filter_chip: Int? by mutableStateOf(null)

    var retrying: Boolean = false

    fun resetSongFeed() {
        layouts = null
        filter_chips = null
        selected_filter_chip = null
    }

    @Composable
    override fun showTopBar(): Boolean =
        LocalPlayerState.current.form_factor == FormFactor.PORTRAIT

    @Composable
    override fun showTopBarContent(): Boolean = true

    override fun canReload(): Boolean = true
    override fun onReload() {
        loadFeed(false)
    }
    @Composable
    override fun isReloading(): Boolean = load_state == FeedLoadState.LOADING

    internal fun selectFilterChip(chip: Int?) {
        if (chip == selected_filter_chip) {
            selected_filter_chip = null
        }
        else {
            selected_filter_chip = chip
        }
        loadFeed(false)
    }

    @Composable
    fun FeedFiltersRow(modifier: Modifier = Modifier, show_scrollbar: Boolean = true, onSelected: (Int?) -> Unit = {}) {
        val player: PlayerState = LocalPlayerState.current
        
        Crossfade(filter_chips, modifier) { chips ->
            if (chips.isNullOrEmpty()) {
                if (load_state != FeedLoadState.LOADING && load_state != FeedLoadState.CONTINUING) {
                    Box(Modifier.padding(end = 40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CloudOff, null)
                    }
                }
            }
            else {
                FilterChipsRow(
                    chips.size,
                    { it == selected_filter_chip },
                    {
                        selectFilterChip(it)
                        onSelected(selected_filter_chip)
                    }
                ) { index ->
                    Text(chips[index].text.getString(player.context))
                }
            }
        }
    }

    @Composable
    override fun TopBarContent(modifier: Modifier, close: () -> Unit) {
        val player: PlayerState = LocalPlayerState.current
        val show: Boolean by mutableSettingsState(FeedSettings.Key.SHOW_FILTER_BAR)

        AnimatedVisibility(show) {
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ player.openAppPage(player.app_page_state.Search) }) {
                    Icon(Icons.Default.Search, null)
                }

                FeedFiltersRow()
            }
        }
    }

    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        when (player.form_factor) {
            FormFactor.PORTRAIT -> SFFSongFeedAppPage(multiselect_context, modifier, content_padding, close)
            FormFactor.LANDSCAPE -> LFFSongFeedAppPage(multiselect_context, modifier, content_padding, close)
        }
    }

    internal fun loadFeed(continuation: Boolean) {
        coroutine_scope.launchSingle {
            loadFeed(false, continuation, selected_filter_chip)
        }
    }

    internal suspend fun loadFeed(
        allow_cached: Boolean,
        continue_feed: Boolean,
        filter_chip: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        selected_filter_chip = filter_chip
        load_lock.lock()
        load_error = null

        try {
            if (load_state != FeedLoadState.PREINIT && load_state != FeedLoadState.NONE) {
                val error = IllegalStateException("Illegal load state $load_state")
                load_error = error
                return@withContext Result.failure(error)
            }

            if (allow_cached && !continue_feed && filter_chip == null) {
                val cached = SongFeedCache.loadFeedLayouts(state.context.database)
                if (cached?.layouts?.isNotEmpty() == true) {
                    layouts = cached.layouts
                    filter_chips = cached.filter_chips
                    continuation = cached.continuation_token
                    return@withContext Result.success(Unit)
                }
            }

            val filter_params = filter_chip?.let { filter_chips!![it].params }
            load_state = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

            val result = loadFeedLayouts(
                if (continue_feed && continuation != null) -1 else Settings.get(
                    FeedSettings.Key.INITIAL_ROWS),
                allow_cached,
                filter_params,
                if (continue_feed) continuation else null
            )

            result.fold(
                { data ->
                    if (continue_feed) {
                        layouts = (layouts ?: emptyList()) + data.layouts
                    }
                    else {
                        layouts = data.layouts
                        filter_chips = data.filter_chips

                        if (filter_chip == null) {
                            SongFeedCache.saveFeedLayouts(data.layouts, data.filter_chips, data.ctoken, state.context.database)
                        }
                    }

                    continuation = data.ctoken

                    return@withContext Result.success(Unit)
                },
                { error ->
                    if (error.anyCauseIs(CancellationException::class)) {
                        return@withContext Result.failure(error)
                    }

                    if (allow_cached) {
                        val cached = SongFeedCache.loadFeedLayouts(state.context.database)
                        if (cached?.layouts?.isNotEmpty() == true) {
                            layouts = cached.layouts
                            filter_chips = cached.filter_chips
                            continuation = cached.continuation_token
                        }
                    }
                    else {
                        layouts = null
                        filter_chips = null
                        continuation = null
                    }
                    load_error = error

                    return@withContext Result.failure(error)
                }
            )
        }
        catch (e: Throwable) {
            load_error = e
            return@withContext Result.failure(e)
        }
        finally {
            load_state = FeedLoadState.NONE
            load_lock.unlock()
        }
    }

    internal suspend fun loadFeedLayouts(
        min_rows: Int,
        allow_cached: Boolean,
        params: String?,
        continuation: String? = null
    ): Result<HomeFeedLoadResult> {
        val result = feed_endpoint.getHomeFeed(
            allow_cached = allow_cached,
            min_rows = min_rows,
            params = params,
            continuation = continuation
        )

        val data = result.getOrNull() ?: return result.cast()
        return Result.success(
            data.copy(
                layouts = data.layouts.filter { it.items.isNotEmpty() }
            )
        )
    }
}

internal fun populateArtistsLayout(
    layouts: List<MediaItemLayout>?,
    own_channel: Artist?,
    context: AppContext
): List<MediaItem> {
    val artists_map: MutableMap<String, Int?> = mutableMapOf()
    for (layout in layouts.orEmpty()) {
        for (item in layout.items) {
            if (item is Artist) {
                artists_map[item.id] = null
                continue
            }

            if (item !is MediaItem.WithArtist) {
                continue
            }

            val artist = item.Artist.get(context.database) ?: continue
            if (artist.id == own_channel?.id || artist.isForItem()) {
                continue
            }

            if (artists_map.containsKey(artist.id)) {
                val current = artists_map[artist.id]
                if (current != null) {
                    artists_map[artist.id] = current + 1
                }
            }
            else {
                artists_map[artist.id] = 1
            }
        }
    }

    artists_map.entries.removeAll { it.value == null }

    var min_occurrences: Int = ARTISTS_ROW_DEFAULT_MIN_OCCURRENCES
    while (min_occurrences >= 0) {
        val count: Int = artists_map.entries.count { artist ->
            (artist.value ?: 0) >= min_occurrences
        }
        if (count >= ARTISTS_ROW_MIN_ARTISTS || count == artists_map.size) {
            break
        }

        min_occurrences--
    }

    val artists = artists_map.mapNotNull { artist ->
        if ((artist.value ?: 0) < min_occurrences) null
        else Pair(artist.key, artist.value)
    }.sortedByDescending { it.second }

    return artists.map { ArtistRef(it.first) }
}
