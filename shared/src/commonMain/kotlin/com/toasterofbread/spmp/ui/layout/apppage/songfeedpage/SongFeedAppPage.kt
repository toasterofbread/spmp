package com.toasterofbread.spmp.ui.layout.apppage.songfeedpage

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.components.utils.composable.RowOrColumn
import com.toasterofbread.spmp.model.*
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.artist.*
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistData
import com.toasterofbread.spmp.model.mediaitem.db.*
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.settings.*
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.service.playercontroller.*
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.*
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import dev.toastbits.ytmkt.endpoint.*
import dev.toastbits.ytmkt.model.external.mediaitem.*
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import PlatformIO

internal const val ARTISTS_ROW_DEFAULT_MIN_OCCURRENCES: Int = 2
internal const val ARTISTS_ROW_MIN_ARTISTS: Int = 4

class SongFeedAppPage(override val state: AppPageState): AppPage() {
    internal val scroll_state: LazyListState = LazyListState()

    internal val feed_endpoint: SongFeedEndpoint = state.context.ytapi.SongFeed

    internal var load_state by mutableStateOf(FeedLoadState.PREINIT)
    internal var load_error: Throwable? by mutableStateOf(null)
    internal val load_lock: Mutex = Mutex()
    internal val coroutine_scope: CoroutineScope = CoroutineScope(Job())

    internal var continuation: String? by mutableStateOf(null)
    internal var layouts: List<AppMediaItemLayout>? by mutableStateOf(null)
    internal var filter_chips: List<SongFeedFilterChip>? by mutableStateOf(null)
    internal var selected_filter_chip: Int? by mutableStateOf(null)

    internal var artists_layout: AppMediaItemLayout by mutableStateOf(
        AppMediaItemLayout(
            emptyList(),
            null,
            null,
            type = ItemLayoutType.GRID
        )
    )

    var retrying: Boolean = false

    fun resetSongFeed() {
        layouts = null
        filter_chips = null
        selected_filter_chip = null
    }

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
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        val player: PlayerState = LocalPlayerState.current
        val form_factor: FormFactor by FormFactor.observe()

        LaunchedEffect(Unit) {
            if (layouts.isNullOrEmpty()) {
                coroutine_scope.launchSingle {
                    loadFeed(allow_cached = !retrying, continue_feed = false)
                    retrying = false
                }
            }
        }

        LaunchedEffect(layouts) {
            artists_layout = artists_layout.copy(
                items = populateArtistsLayout(
                    layouts,
                    player.context.ytapi.user_auth_state?.own_channel_id,
                    player.context
                )
            )
        }

        when (form_factor) {
            FormFactor.PORTRAIT -> SFFSongFeedAppPage(multiselect_context, modifier, content_padding, close)
            FormFactor.LANDSCAPE -> LFFSongFeedAppPage(multiselect_context, modifier, content_padding, close)
        }
    }

    @Composable
    override fun shouldShowPrimaryBarContent(): Boolean = !filter_chips.isNullOrEmpty()

    @Composable
    override fun PrimaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        val form_factor: FormFactor by FormFactor.observe()
        return when (form_factor) {
            FormFactor.PORTRAIT -> SFFSongFeedPagePrimaryBar(slot, modifier, content_padding, lazy)
            FormFactor.LANDSCAPE -> LFFSongFeedPagePrimaryBar(slot, modifier, content_padding, lazy)
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
    ): Result<Unit> = withContext(Dispatchers.PlatformIO) {
        selected_filter_chip = filter_chip
        load_lock.lock()
        load_error = null

        try {
            if (load_state != FeedLoadState.PREINIT && load_state != FeedLoadState.NONE) {
                val error: Throwable = IllegalStateException("Illegal load state $load_state")
                load_error = error
                return@withContext Result.failure(error)
            }

            if (allow_cached && !continue_feed && filter_chip == null) {
                val cached: SongFeedData? = SongFeedCache.loadFeedLayouts(state.context.database)
                if (cached?.layouts?.isNotEmpty() == true) {
                    layouts = cached.layouts.map { it.layout }
                    filter_chips = cached.filter_chips.sortFilterChips()
                    continuation = cached.continuation_token
                    return@withContext Result.success(Unit)
                }
            }

            val filter_params: String? = filter_chip?.let { filter_chips!![it].params }
            load_state = if (continue_feed) FeedLoadState.CONTINUING else FeedLoadState.LOADING

            val result: Result<SongFeedLoadResult> = loadFeedLayouts(
                if (continue_feed && continuation != null) -1 else state.context.settings.Feed.INITIAL_ROWS.get(),
                filter_params,
                if (continue_feed) continuation else null
            )

            result.fold(
                { data ->
                    val data_layouts: List<AppMediaItemLayout> = data.layouts.map { AppMediaItemLayout(it) }
                    if (continue_feed) {
                        layouts = (layouts ?: emptyList()) + data_layouts
                    }
                    else {
                        layouts = data_layouts
                        filter_chips = data.filter_chips?.sortFilterChips()

                        if (filter_chip == null) {
                            SongFeedCache.saveFeedLayouts(data_layouts, data.filter_chips, data.ctoken, state.context.database)
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
                            layouts = cached.layouts.map { it.layout }
                            filter_chips = cached.filter_chips.sortFilterChips()
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
        params: String?,
        continuation: String? = null
    ): Result<SongFeedLoadResult> = runCatching {
        val load_result: SongFeedLoadResult =
            feed_endpoint.getSongFeed(
                min_rows = min_rows,
                params = params,
                continuation = continuation
            ).getOrThrow()

        return@runCatching load_result.copy(
            layouts = load_result.layouts.filter { it.items.isNotEmpty() }
        )
    }
}

internal fun populateArtistsLayout(
    layouts: List<AppMediaItemLayout>?,
    own_channel_id: String?,
    context: AppContext
): List<MediaItemData> {
    val artists_map: MutableMap<String, Int?> = mutableMapOf()
    for (layout in layouts.orEmpty()) {
        for (item in layout.items) {
            if (item is Artist) {
                artists_map[item.id] = null
                continue
            }

            if (item !is MediaItem.WithArtists) {
                continue
            }

            for (artist in item.Artists.get(context.database) ?: emptyList()) {
                if (artist.isForItem()) {
                    continue
                }

                if (artist.id == own_channel_id) {
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

    return artists.map { ArtistData(it.first) }
}
