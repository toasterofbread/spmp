package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.BackHandler
import com.toasterofbread.composekit.platform.composable.SwipeRefresh
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.utils.common.anyCauseIs
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.composekit.utils.modifier.horizontal
import com.toasterofbread.composekit.utils.modifier.vertical
import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.SongFeedCache
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mutableSettingsState
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.FeedLoadState
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage
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
    override fun showTopBar(): Boolean = true

    @Composable
    override fun showTopBarContent(): Boolean = true

    @Composable
    override fun TopBarContent(modifier: Modifier, close: () -> Unit) {
        val player = LocalPlayerState.current
        val show: Boolean by mutableSettingsState(Settings.KEY_FEED_SHOW_FILTER_BAR)
        
        AnimatedVisibility(show) {
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ player.openAppPage(player.app_page_state.Search) }) {
                    Icon(Icons.Default.Search, null)
                }

                Crossfade(filter_chips, modifier) { chips ->
                    if (chips.isNullOrEmpty()) {
                        if (load_state != FeedLoadState.LOADING && load_state != FeedLoadState.CONTINUING) {
                            Box(Modifier.fillMaxWidth().padding(end = 40.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudOff, null)
                            }
                        }
                    }
                    else {
                        FilterChipsRow(
                            chips.size,
                            { it == selected_filter_chip },
                            {
                                if (it == selected_filter_chip) {
                                    selected_filter_chip = null
                                }
                                else {
                                    selected_filter_chip = it
                                }
                                loadFeed(false)
                            },
                            Modifier.fillMaxWidth()
                        ) { index ->
                            Text(chips[index].text.getString(player.context))
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun ColumnScope.SFFPage(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        SFFSongFeedAppPage(multiselect_context, modifier, content_padding, close)
    }

    @Composable
    override fun ColumnScope.LFFPage(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        LFFSongFeedAppPage(multiselect_context, modifier, content_padding, close)
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
                if (continue_feed && continuation != null) -1 else Settings.get(Settings.KEY_FEED_INITIAL_ROWS),
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
    artists_layout_items: MutableList<MediaItem>,
    layouts: List<MediaItemLayout>?,
    own_channel: Artist?,
    context: AppContext
) {
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

    artists_layout_items.clear()
    for (artist in artists) {
        artists_layout_items.add(ArtistRef(artist.first))
    }
}
