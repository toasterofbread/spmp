package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.db.SongFeedCache
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.model.mutableSettingsState
import com.toasterofbread.spmp.platform.BackHandler
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.platform.composable.SwipeRefresh
import com.toasterofbread.spmp.platform.composable.platformClickable
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.uilocalisation.LocalisedString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.PinnedItemsRow
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.FeedLoadState
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedEndpoint
import com.toasterofbread.spmp.youtubeapi.endpoint.HomeFeedLoadResult
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.cast
import com.toasterofbread.utils.common.anyCauseIs
import com.toasterofbread.utils.common.launchSingle
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private const val ARTISTS_ROW_DEFAULT_MIN_OCCURRENCES: Int = 2
private const val ARTISTS_ROW_MIN_ARTISTS: Int = 4

class SongFeedAppPage(override val state: AppPageState): AppPage() {
    private val scroll_state: LazyListState = LazyListState()

    private val feed_endpoint: HomeFeedEndpoint = state.context.ytapi.HomeFeed

    private var load_state by mutableStateOf(FeedLoadState.PREINIT)
    private var load_error: Throwable? by mutableStateOf(null)
    private val load_lock: Mutex = Mutex()
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    private var continuation: String? by mutableStateOf(null)
    private var layouts: List<MediaItemLayout>? by mutableStateOf(null)
    private var filter_chips: List<FilterChip>? by mutableStateOf(null)
    private var selected_filter_chip: Int? by mutableStateOf(null)

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
                            Text(chips[index].text.getString())
                        }
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

        BackHandler({ selected_filter_chip != null }) {
            selected_filter_chip = null
            loadFeed(false)
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
        val hidden_rows: Set<String> by Settings.KEY_FEED_HIDDEN_ROWS.rememberMutableState()

        val square_item_max_text_rows: Int by Settings.KEY_FEED_SQUARE_PREVIEW_TEXT_LINES.rememberMutableState()
        val show_download_indicators: Boolean by Settings.KEY_FEED_SHOW_SONG_DOWNLOAD_INDICATORS.rememberMutableState()

        LaunchedEffect(Unit) {
            if (layouts.isNullOrEmpty()) {
                coroutine_scope.launchSingle {
                    loadFeed(allow_cached = !retrying, continue_feed = false)
                    retrying = false
                }
            }
        }

        LaunchedEffect(layouts) {
            populateArtistsLayout(
                artists_layout.items as MutableList<MediaItem>,
                layouts,
                player.context.ytapi.user_auth_state?.own_channel,
                player.context
            )
        }

        // Main scrolling view
        SwipeRefresh(
            state = load_state == FeedLoadState.LOADING,
            onRefresh = { loadFeed(false) },
            swipe_enabled = load_state == FeedLoadState.NONE,
            indicator = false,
            modifier = Modifier.fillMaxSize()
        ) {
            val target_state = if (load_state == FeedLoadState.LOADING || load_state == FeedLoadState.PREINIT) null else layouts ?: false
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

            var hiding_layout: MediaItemLayout? by remember { mutableStateOf(null) }

            hiding_layout?.also { layout ->
                check(layout.title != null)

                PlatformAlertDialog(
                    onDismissRequest = { hiding_layout = null },
                    confirmButton = {
                        Button({
                            val hidden_rows: Set<String> = Settings.KEY_FEED_HIDDEN_ROWS.get()
                            Settings.KEY_FEED_HIDDEN_ROWS.set(
                                hidden_rows.plus(layout.title.serialise())
                            )

                            hiding_layout = null
                        }) {
                            Text(getString("action_confirm_action"))
                        }
                    },
                    dismissButton = {
                        Button({
                            hiding_layout = null
                        }) {
                            Text(getString("action_deny_action"))
                        }
                    },
                    title = {
                        Text(getString("prompt_confirm_action"))
                    },
                    text = {
                        Text(getString("prompt_hide_feed_rows_with_\$title").replace("\$title", layout.title.getString()))
                    }
                )
            }

            val state = current_state

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

                        items((state as List<MediaItemLayout>)) { layout ->
                            if (layout.items.isEmpty()) {
                                return@items
                            }

                            if (layout.title != null) {
                                val title: String = layout.title.getString()
                                if (
                                    hidden_rows.any { row_title ->
                                        LocalisedString.deserialise(row_title).getString() == title
                                    }
                                ) {
                                    return@items
                                }
                            }

                            val type = layout.type ?: MediaItemLayout.Type.GRID
                            type.Layout(
                                layout,
                                Modifier.padding(top = 20.dp),
                                title_modifier = Modifier.platformClickable(
                                    onAltClick = {
                                        if (layout.title != null) {
                                            hiding_layout = layout
                                        }
                                    }
                                ),
                                multiselect_context = player.main_multiselect_context,
                                apply_filter = true,
                                square_item_max_text_rows = square_item_max_text_rows,
                                show_download_indicators = show_download_indicators
                            )
                        }

                        item {
                            Crossfade(Pair(onContinuationRequested, loading_continuation)) { data ->
                                val (requestContinuation, loading) = data

                                if (loading || requestContinuation != null) {
                                    Box(Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.Center) {
                                        if (loading) {
                                            SubtleLoadingIndicator()
                                        }
                                        else if (requestContinuation != null) {
                                            IconButton({ requestContinuation() }) {
                                                Icon(Icons.Filled.KeyboardDoubleArrowDown, null, tint = Theme.on_background)
                                            }
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
                    LaunchedEffect(load_error) {
                        if (load_error == null) {
                            return@LaunchedEffect
                        }

                        val library = player.app_page_state.Library
                        library.external_load_error = load_error
                        load_error = null

                        player.openAppPage(library)
                    }
                }
            }
        }
    }

    private fun loadFeed(continuation: Boolean) {
        coroutine_scope.launchSingle {
            loadFeed(false, continuation, selected_filter_chip)
        }
    }

    private suspend fun loadFeed(
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
