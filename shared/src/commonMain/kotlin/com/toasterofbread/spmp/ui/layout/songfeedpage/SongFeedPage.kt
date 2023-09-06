package com.toasterofbread.spmp.ui.layout.songfeedpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.model.FilterChip
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mutableSettingsState
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.composable.SwipeRefresh
import com.toasterofbread.spmp.platform.getDefaultVerticalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.PinnedItemsRow
import com.toasterofbread.spmp.ui.layout.library.LibraryPage
import com.toasterofbread.spmp.ui.layout.mainpage.FeedLoadState
import com.toasterofbread.spmp.ui.layout.mainpage.MainPage
import com.toasterofbread.spmp.ui.layout.mainpage.MainPageState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedLoadResult
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.unit
import com.toasterofbread.utils.common.launchSingle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val ARTISTS_ROW_DEFAULT_MIN_OCCURRENCES: Int = 2
private const val ARTISTS_ROW_MIN_ARTISTS: Int = 4

class SongFeedPage(state: MainPageState): MainPage(state) {
    private val scroll_state = LazyListState()

    private val feed_endpoint = state.context.ytapi.HomeFeed

    private var load_state by mutableStateOf(FeedLoadState.PREINIT)
    private var load_error: Throwable? by mutableStateOf(null)
    private val load_lock = Mutex()
    private val coroutine_scope = CoroutineScope(Job())

    private var continuation: String? by mutableStateOf(null)
    private var layouts: List<MediaItemLayout>? by mutableStateOf(null)
    private var filter_chips: List<FilterChip>? by mutableStateOf(null)
    private var selected_filter_chip: Int? by mutableStateOf(null)

    fun resetSongFeed() {
        layouts = emptyList()
        filter_chips = null
        selected_filter_chip = null
    }

    @Composable
    override fun showTopBarContent(): Boolean = true

    @Composable
    override fun TopBarContent(modifier: Modifier, close: () -> Unit) {
        val player = LocalPlayerState.current
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ player.setMainPage(player.main_page_state.Search) }) {
                Icon(Icons.Default.Search, null)
            }

            val enabled: Boolean by mutableSettingsState(Settings.KEY_FEED_SHOW_FILTERS)

            Crossfade(if (enabled) filter_chips else null, modifier) { chips ->
                if (chips?.isNotEmpty() == true) {
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
                        Text(chips[index].text.getString())
                    }
                }
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
        if (!feed_endpoint.isImplemented()) {
            feed_endpoint.NotImplementedMessage(modifier.fillMaxSize())
            return
        }

        val player = LocalPlayerState.current
        val artists_layout: MediaItemLayout = remember {
            MediaItemLayout(
                mutableStateListOf(),
                null,
                null,
                type = MediaItemLayout.Type.ROW
            )
        }

        LaunchedEffect(layouts) {
            populateArtistsLayout(
                artists_layout.items as MutableList<MediaItem>,
                layouts,
                player.context.ytapi.user_auth_state?.own_channel,
                player.context
            )
        }

        LaunchedEffect(Unit) {
            if (layouts.isNullOrEmpty()) {
                coroutine_scope.launchSingle {
                    val r = loadFeed(Settings.get(Settings.KEY_FEED_INITIAL_ROWS), allow_cached = true, continue_feed = false)
                }
            }
        }

        // Main scrolling view
        SwipeRefresh(
            state = load_state == FeedLoadState.LOADING,
            onRefresh = { loadFeed(false) },
            swipe_enabled = load_state == FeedLoadState.NONE,
            indicator = false
        ) {
            val target_state =
                if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts?.ifEmpty { load_error } ?: false
            var current_state by remember { mutableStateOf(target_state) }
            val state_alpha = remember { Animatable(1f) }

            LaunchedEffect(target_state) {
                if (current_state == target_state) {
                    state_alpha.animateTo(1f, tween(300))
                    return@LaunchedEffect
                }

                if (current_state is List<*> && target_state is List<*>) {
                    state_alpha.snapTo(1f)
                    current_state = target_state
                }
                else {
                    state_alpha.animateTo(0f, tween(300))
                    current_state = target_state
                    state_alpha.animateTo(1f, tween(300))
                }
            }

            @Composable
            fun TopContent() {
                PinnedItemsRow(Modifier.padding(bottom = 10.dp))
            }

            current_state.also { state ->
                when (state) {
                    // Loaded
                    is List<*> -> {
                        val onContinuationRequested = if (continuation != null) {
                            { loadFeed(true) }
                        } else null
                        val loading_continuation = load_state != FeedLoadState.NONE

                        LazyColumn(
                            Modifier.graphicsLayer { alpha = state_alpha.value },
                            state = scroll_state,
                            contentPadding = content_padding,
                            userScrollEnabled = !state_alpha.isRunning
                        ) {
                            item {
                                TopContent()
                            }

                            item {
                                if (artists_layout.items.isNotEmpty()) {
                                    artists_layout.Layout(multiselect_context = player.main_multiselect_context, apply_filter = true)
                                }
                            }

                            items(state as List<MediaItemLayout>) { layout ->
                                if (layout.items.isEmpty()) {
                                    return@items
                                }

                                val type = layout.type ?: MediaItemLayout.Type.GRID
                                type.Layout(
                                    layout,
                                    Modifier.padding(top = 20.dp),
                                    multiselect_context = player.main_multiselect_context,
                                    apply_filter = true
                                )
                            }

                            item {
                                Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                                    val (requestContinuation, loading) = data

                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                        if (loading) {
                                            CircularProgressIndicator(color = Theme.on_background)
                                        } else if (requestContinuation != null) {
                                            IconButton({ requestContinuation() }) {
                                                Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.on_background)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading
                    null -> {
                        Column(Modifier.fillMaxSize()) {
                            TopContent()
                            SongFeedPageLoadingView(Modifier.graphicsLayer { alpha = state_alpha.value }.fillMaxSize())
                        }
                    }

                    // Load failed
                    else -> {
                        var error_dismissed by remember { mutableStateOf(false) }

                        Column(
                            Modifier
                                .graphicsLayer { alpha = state_alpha.value }
                                .padding(content_padding)
                                .fillMaxHeight()
                        ) {
                            if (state is Throwable) {
                                AnimatedVisibility(!error_dismissed) {
                                    ErrorInfoDisplay(
                                        state,
                                        modifier = Modifier.padding(bottom = 20.dp),
                                        expanded_modifier = Modifier.padding(bottom = player.getDefaultVerticalPadding()),
                                        message = getString("error_yt_feed_parse_failed"),
                                        onDismiss = {
                                            error_dismissed = true
                                        },
                                        disable_parent_scroll = false
                                    )
                                }
                            }

                            LibraryPage(
                                PaddingValues(bottom = LocalPlayerState.current.bottom_padding_dp),
                                close = {},
                                outer_multiselect_context = LocalPlayerState.current.main_multiselect_context
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadFeed(continuation: Boolean) {
        coroutine_scope.launchSingle {
            loadFeed(-1, false, continuation, selected_filter_chip)
        }
    }

    private suspend fun loadFeed(
        min_rows: Int,
        allow_cached: Boolean,
        continue_feed: Boolean,
        filter_chip: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        selected_filter_chip = filter_chip
        load_lock.lock()

        try {
            if (load_state != FeedLoadState.PREINIT && load_state != FeedLoadState.NONE) {
                return@withContext Result.failure(IllegalStateException("Illegal load state $load_state"))
            }

            val filter_params = filter_chip?.let { filter_chips!![it].params }
            load_state = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

            val result = loadFeedLayouts(min_rows, allow_cached, filter_params, if (continue_feed) continuation else null)

            result.fold(
                { data ->
                    if (continue_feed) {
                        layouts = (layouts ?: emptyList()) + data.layouts
                    } else {
                        layouts = data.layouts
                        filter_chips = data.filter_chips
                    }

                    continuation = data.ctoken
                },
                { error ->
                    load_error = error
                    layouts = emptyList()
                    filter_chips = null
                }
            )

            return@withContext result.unit()
        }
        finally {
            load_state = FeedLoadState.NONE
            load_lock.unlock()
        }
    }

    private suspend fun loadFeedLayouts(
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

private fun populateArtistsLayout(
    artists_layout_items: MutableList<MediaItem>,
    layouts: List<MediaItemLayout>?,
    own_channel: Artist?,
    context: PlatformContext
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
            if (artist.id == own_channel?.id) {
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
    while (true) {
        val count: Int = artists_map.entries.count { artist ->
            (artist.value ?: 0) >= min_occurrences
        }
        if (count >= ARTISTS_ROW_MIN_ARTISTS || count == artists_map.size) {
            break
        }

        min_occurrences++
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
