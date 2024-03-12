package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import LocalNowPlayingExpansion
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.toasterofbread.composekit.platform.composable.*
import com.toasterofbread.composekit.utils.common.*
import com.toasterofbread.composekit.utils.composable.*
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.enums.*
import com.toasterofbread.spmp.model.mediaitem.layout.*
import com.toasterofbread.spmp.model.settings.category.BehaviourSettings
import com.toasterofbread.spmp.platform.*
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.appTextField
import com.toasterofbread.spmp.ui.layout.apppage.AppPage
import com.toasterofbread.spmp.ui.layout.apppage.AppPageState
import com.toasterofbread.spmp.ui.layout.contentbar.*
import com.toasterofbread.spmp.ui.layout.nowplaying.NowPlayingExpansionState
import com.toasterofbread.spmp.ui.theme.appHover
import com.toasterofbread.spmp.youtubeapi.NotImplementedMessage
import com.toasterofbread.spmp.youtubeapi.endpoint.*
import kotlinx.coroutines.*

internal val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
internal const val SEARCH_SUGGESTIONS_LOAD_DELAY_MS: Long = 200
internal const val SEARCH_MAX_SUGGESTIONS: Int = 5

class SearchAppPage(override val state: AppPageState, val context: AppContext): AppPage() {
    private val coroutine_scope = CoroutineScope(Job())
    private val search_lock = Object()
    private val search_endpoint = context.ytapi.Search

    private var clearFocus: (() -> Unit)? = null
    private var multiselect_context: MediaItemMultiSelectContext? = null

    internal var search_in_progress: Boolean by mutableStateOf(false)
    internal var current_results: SearchResults? by mutableStateOf(null)
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
    override fun showTopBarContent(): Boolean = true

    @Composable
    override fun PrimaryBarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        val show_suggestions: Boolean by BehaviourSettings.Key.SEARCH_SHOW_SUGGESTIONS.rememberMutableState()
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
            VerticalSearchPrimaryBar(suggestions, slot, modifier, content_padding)
        }
        else {
            HorizontalSearchPrimaryBar(suggestions, slot, modifier, content_padding)
        }
        return true
    }

    @Composable
    override fun SecondaryBarContent(slot: LayoutSlot, content_padding: PaddingValues, modifier: Modifier): Boolean {
        if (slot.is_vertical) {
            VerticalSearchSecondaryBar(slot, modifier, content_padding)
        }
        else {
            HorizontalSearchSecondaryBar(slot, modifier, content_padding)
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
                    if (results is SearchResults) {
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
                            SubtleLoadingIndicator(getColour = { context.theme.on_background }, message = getString("search_results_loading"))
                        }
                    }
                }
            }
        }
    }

    fun performSearch() {
        performSearch(current_filter?.let { SearchFilter(it, it.getDefaultParams()) })
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
                search_endpoint.searchMusic(query, filter?.params).fold(
                    { results ->
                        for (result in results.categories) {
                            if (result.second != null) {
                                result.first.view_more = LambdaViewMore { _, _ ->
                                    performSearch(result.second)
                                }
                            }
                        }

                        synchronized(search_lock) {
                            current_results = results
                            search_in_progress = false
                        }
                    },
                    {
                        error = it
                        synchronized(search_lock) {
                            search_in_progress = false
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun Results(results: SearchResults, padding: PaddingValues, multiselect_context: MediaItemMultiSelectContext) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            if (results.suggested_correction != null) {
                item {
                    SuggestedSearchCorrection(
                        results.suggested_correction,
                        Modifier.fillMaxWidth(),
                        onSelected = {
                            current_query = results.suggested_correction
                            performSearch()
                        },
                        onDismissed = {
                            current_results = results.copy(suggested_correction = null)
                        }
                    )
                }
            }

            for (category in results.categories.withIndex()) {
                val layout = category.value.first
                item {
                    (layout.type ?: MediaItemLayout.Type.LIST).Layout(layout, multiselect_context = multiselect_context)
                }
            }
        }
    }

    @Composable
    private fun SuggestedSearchCorrection(correction: String, modifier: Modifier = Modifier, onSelected: () -> Unit, onDismissed: () -> Unit) {
        Row(
            modifier
                .border(2.dp, context.theme.accent, CircleShape)
                .clickable(onClick = onSelected)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                getString("search_suggested_correction_\$x").replace("\$x", correction),
                Modifier.fillMaxWidth().weight(1f)
            )

            IconButton(onDismissed) {
                Icon(Icons.Default.Close, null)
            }
        }
    }
}