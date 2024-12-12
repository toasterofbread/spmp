package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalPlayerState
import SpMp.isDebugBuild
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.components.platform.composable.*
import dev.toastbits.composekit.util.*
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.components.utils.composable.*
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.enums.*
import com.toasterofbread.spmp.model.mediaitem.layout.*
import com.toasterofbread.spmp.model.mediaitem.layout.AppMediaItemLayout
import com.toasterofbread.spmp.model.MediaItemLayoutParams
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.contentbar.layoutslot.LayoutSlot
import com.toasterofbread.spmp.ui.component.NotImplementedMessage
import dev.toastbits.composekit.util.composable.copy
import dev.toastbits.ytmkt.endpoint.*
import dev.toastbits.ytmkt.endpoint.SearchFilter
import dev.toastbits.ytmkt.endpoint.SearchResults
import dev.toastbits.ytmkt.model.external.ItemLayoutType
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.search_results_loading
import spmp.shared.generated.resources.`search_suggested_correction_$x`
import java.util.concurrent.locks.ReentrantLock

internal val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
internal const val SEARCH_SUGGESTIONS_LOAD_DELAY_MS: Long = 200
internal const val SEARCH_MAX_SUGGESTIONS: Int = 5

internal data class AppSearchResults(
    val categories: List<Pair<AppMediaItemLayout, SearchFilter?>>,
    val suggested_correction: String?
) {
    constructor(results: SearchResults): this(
        results.categories.map { category ->
            Pair(AppMediaItemLayout(category.first), category.second)
        },
        results.suggested_correction
    )
}

class SearchAppPage(override val state: AppPageState, val context: AppContext): AppPage() {
    private val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    private val search_lock: ReentrantLock = ReentrantLock()
    private val search_endpoint: SearchEndpoint = context.ytapi.Search

    private var clearFocus: (() -> Unit)? = null
    private var multiselect_context: MediaItemMultiSelectContext? = null

    internal var search_in_progress: Boolean by mutableStateOf(false)
    internal var current_results: AppSearchResults? by mutableStateOf(null)
    internal var current_query: String by mutableStateOf("")
    internal var current_filter: SearchType? by mutableStateOf(null)
    private var error: Throwable? by mutableStateOf(null)

    override fun onOpened(from_item: MediaItemHolder?) {
        coroutine_scope.coroutineContext.cancelChildren()
        search_in_progress = false
        current_results = null
        current_query = ""
        current_filter = null
        error = null
    }

    internal fun setFilter(filter: SearchType?) {
        if (filter == current_filter) {
            return
        }

        current_filter = filter
        if (current_results != null || search_in_progress) {
            performSearch()
        }
    }

    @Composable
    override fun shouldShowPrimaryBarContent(): Boolean = true

    @Composable
    override fun PrimaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        if (slot.is_vertical) {
            VerticalSearchPrimaryBar(slot, modifier, content_padding, lazy)
        }
        else {
            HorizontalSearchPrimaryBar(slot, modifier, content_padding, lazy)
        }
        return true
    }

    @Composable
    override fun shouldShowSecondaryBarContent(): Boolean = true

    @Composable
    override fun SecondaryBarContent(
        slot: LayoutSlot,
        content_padding: PaddingValues,
        distance_to_page: Dp,
        lazy: Boolean,
        modifier: Modifier
    ): Boolean {
        val show_suggestions: Boolean by context.settings.Behaviour.SEARCH_SHOW_SUGGESTIONS.observe()
        var suggestions: List<SearchSuggestion> by remember { mutableStateOf(emptyList()) }

        LaunchedEffect(current_query, show_suggestions) {
            if (!show_suggestions) {
                suggestions = emptyList()
                return@LaunchedEffect
            }

            if (search_in_progress) {
                return@LaunchedEffect
            }

            val query: String = current_query
            if (query.isBlank()) {
                suggestions = emptyList()
                return@LaunchedEffect
            }

            delay(SEARCH_SUGGESTIONS_LOAD_DELAY_MS)

            val suggestions_endpoint = context.ytapi.SearchSuggestions
            if (!suggestions_endpoint.isImplemented()) {
                suggestions = emptyList()
                return@LaunchedEffect
            }

            suggestions = suggestions_endpoint
                .getSearchSuggestions(query).getOrNull()?.take(SEARCH_MAX_SUGGESTIONS)?.asReversed()
                    ?: emptyList()
        }

        if (slot.is_vertical) {
            VerticalSearchSecondaryBar(suggestions, slot, distance_to_page, modifier, content_padding)
        }
        else {
            HorizontalSearchSecondaryBar(suggestions, slot, modifier, content_padding)
        }
        return true
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    ) {
        if (!search_endpoint.isImplemented()) {
            search_endpoint.NotImplementedMessage(modifier.fillMaxSize().padding(content_padding))
            return
        }

        BackHandler({ current_filter != null }) {
            setFilter(null)
        }

        val player: PlayerState = LocalPlayerState.current
        val focus_manager: FocusManager = LocalFocusManager.current
        val keyboard_controller: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
        val focus_state: MutableState<Boolean> = remember { mutableStateOf(false) }

        DisposableEffect(focus_manager, keyboard_controller) {
            clearFocus = {
                focus_manager.clearFocus()
                keyboard_controller?.hide()
            }
            this@SearchAppPage.multiselect_context = multiselect_context

            onDispose {
                clearFocus = null
                this@SearchAppPage.multiselect_context = null
            }
        }

        val keyboard_open: Boolean by rememberKeyboardOpen()
        LaunchedEffect(keyboard_open) {
            if (!keyboard_open) {
                clearFocus?.invoke()
            }
        }

        Box(modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                val padding = content_padding.copy(
                    bottom = content_padding.calculateBottomPadding() + SEARCH_BAR_HEIGHT_DP.dp + (SEARCH_BAR_V_PADDING_DP.dp * 2)
                )

                Crossfade(
                    error ?: current_results
                ) { results ->
                    if (results is AppSearchResults) {
                        Results(
                            results,
                            padding,
                            multiselect_context
                        )
                    }
                    else if (results is Throwable) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorInfoDisplay(
                                results,
                                isDebugBuild(),
                                Modifier.fillMaxWidth(),
                                onDismiss = { error = null },
                                onRetry = {
                                    performSearch()
                                }
                            )
                        }
                    }
                    else if (search_in_progress) {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            SubtleLoadingIndicator(getColour = { context.theme.onBackground }, message = stringResource(Res.string.search_results_loading))
                        }
                    }
                }
            }
        }
    }

    fun performSearch() {
        performSearch(current_filter?.let { SearchFilter(it, it.getDefaultParams()) })
    }

    fun performSearch(query: String) {
        current_query = query
        current_filter = null
        performSearch()
    }

    fun performSearch(filter: SearchFilter?) {
        check(search_endpoint.isImplemented())

        clearFocus?.invoke()

        synchronized(search_lock) {
            if (current_query.isBlank()) {
                return
            }

            search_in_progress = true

            val query: String = current_query
            current_results = null
            current_filter = filter?.type
            error = null
            multiselect_context?.setActive(false)

            coroutine_scope.launchSingle {
                val non_music: Boolean = context.settings.Search.SEARCH_FOR_NON_MUSIC.get()
                search_endpoint.search(query, filter?.params, non_music = non_music).fold(
                    { results ->
                        for (result in results.categories) {
                            if (result.second != null) {
                                result.copy(
                                    first = result.first.copy(
                                        view_more = LambdaYoutubePage { _, _ ->
                                            performSearch(result.second)
                                        }
                                    )
                                )
                            }
                        }

                        synchronized(search_lock) {
                            current_results = AppSearchResults(results)
                            search_in_progress = false
                        }
                    },
                    {
                        if (it !is CancellationException) {
                            error = it
                        }
                        synchronized(search_lock) {
                            search_in_progress = false
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun Results(results: AppSearchResults, padding: PaddingValues, multiselect_context: MediaItemMultiSelectContext) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            results.suggested_correction?.also { suggested_correction ->
                item {
                    SuggestedSearchCorrection(
                        suggested_correction,
                        Modifier.fillMaxWidth(),
                        onSelected = {
                            current_query = suggested_correction
                            performSearch()
                        },
                        onDismissed = {
                            current_results = results.copy(suggested_correction = null)
                        }
                    )
                }
            }

            for (category in results.categories.withIndex()) {
                val layout: AppMediaItemLayout = category.value.first
                item {
                    (layout.type ?: ItemLayoutType.LIST).Layout(
                        layout,
                        MediaItemLayoutParams(
                            multiselect_context = multiselect_context
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun SuggestedSearchCorrection(correction: String, modifier: Modifier = Modifier, onSelected: () -> Unit, onDismissed: () -> Unit) {
        val shape: Shape = CircleShape
        Row(
            modifier
                .border(2.dp, context.theme.accent, shape)
                .clip(shape)
                .clickable(onClick = onSelected)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.`search_suggested_correction_$x`).replace("\$x", correction),
                Modifier.fillMaxWidth().weight(1f)
            )

            IconButton(onDismissed) {
                Icon(Icons.Default.Close, null)
            }
        }
    }
}
