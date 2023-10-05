package com.toasterofbread.spmp.ui.layout.apppage

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.model.mediaitem.layout.LambdaViewMore
import com.toasterofbread.spmp.model.mediaitem.layout.MediaItemLayout
import com.toasterofbread.spmp.platform.BackHandler
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.nowplaying.LocalNowPlayingExpansion
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.*
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchFilter
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchResults
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchType
import com.toasterofbread.utils.*
import com.toasterofbread.utils.common.copy
import com.toasterofbread.utils.common.launchSingle
import com.toasterofbread.utils.composable.AlignableCrossfade
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.utils.composable.rememberKeyboardOpen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
private const val SEARCH_BAR_HEIGHT_DP = 45f
private const val SEARCH_BAR_V_PADDING_DP = 15f
private const val SEARCH_SUGGESTIONS_LOAD_DELAY_MS: Long = 200
private const val SEARCH_MAX_SUGGESTIONS: Int = 5

class SearchAppPage(override val state: AppPageState, val context: PlatformContext): AppPage() {
    private val coroutine_scope = CoroutineScope(Job())
    private val search_lock = Object()
    private val search_endpoint = context.ytapi.Search

    private var clearFocus: (() -> Unit)? = null
    private var multiselect_context: MediaItemMultiSelectContext? = null

    private var search_in_progress: Boolean by mutableStateOf(false)
    private var current_results: SearchResults? by mutableStateOf(null)
    private var current_query: String by mutableStateOf("")
    private var current_filter: SearchType? by mutableStateOf(null)
    private var error: Throwable? by mutableStateOf(null)

    override fun onOpened(from_item: MediaItemHolder?) {
        coroutine_scope.coroutineContext.cancelChildren()
        search_in_progress = false
        current_results = null
        current_query = ""
        current_filter = null
        error = null
    }

    private fun setFilter(filter: SearchType?) {
        if (filter == current_filter) {
            return
        }

        current_filter = filter
        if (current_results != null || search_in_progress) {
            performSearch()
        }
    }

    @Composable
    override fun showTopBar(): Boolean = true

    @Composable
    override fun showTopBarContent(): Boolean = true

    @Composable
    override fun TopBarContent(modifier: Modifier, close: () -> Unit) {
        Row(modifier) {
            IconButton(close) {
                Icon(Icons.Default.Close, null)
            }

            FilterChipsRow(
                SearchType.values().size + 1,
                { index ->
                    if (current_filter == null) index == 0 else current_filter!!.ordinal == index - 1
                },
                { index ->
                    if (index == 0) {
                        setFilter(null)
                    }
                    else {
                        setFilter(SearchType.values()[index - 1])
                    }
                },
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .height(SEARCH_BAR_HEIGHT_DP.dp),
                spacing = 5.dp
            ) { index ->
                Text(when (if (index == 0) null else SearchType.values()[index - 1]) {
                    null -> getString("search_filter_all")
                    SearchType.VIDEO -> getString("search_filter_videos")
                    SearchType.SONG -> MediaItemType.SONG.getReadable(true)
                    SearchType.ARTIST -> MediaItemType.ARTIST.getReadable(true)
                    SearchType.PLAYLIST -> PlaylistType.PLAYLIST.getReadable(true)
                    SearchType.ALBUM -> PlaylistType.ALBUM.getReadable(true)
                })
            }
        }
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

        val player = LocalPlayerState.current
        val focus_manager = LocalFocusManager.current
        val keyboard_controller = LocalSoftwareKeyboardController.current
        val focus_state = remember { mutableStateOf(false) }

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

        val keyboard_open by rememberKeyboardOpen()
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
                            SubtleLoadingIndicator(getColour = { Theme.on_background }, message = getString("search_results_loading"))
                        }
                    }
                }
            }

            SearchBar(
                focus_state,
                player.nowPlayingTopOffset(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = player.getDefaultHorizontalPadding())
                        .zIndex(1f)
                ),
                close = close
            )
        }
    }

    fun performSearch() {
        performSearch(current_filter?.let { SearchFilter(it, it.getDefaultParams()) })
    }

    fun performSearch(filter: SearchFilter?) {
        check(search_endpoint.isImplemented())

        clearFocus?.invoke()

        synchronized(search_lock) {
            search_in_progress = true

            val query = current_query
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
    private fun SearchSuggestion(
        suggestion: String,
        shape: Shape,
        modifier: Modifier = Modifier,
        onSelected: () -> Unit,
    ) {
        Row(
            modifier
                .clickable(onClick = onSelected)
                .clip(shape)
                .background(Theme.background)
                .border(2.dp, Theme.accent, shape)
                .padding(10.dp)
        ) {
            Text(suggestion, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    private fun SearchBar(
        focus_state: MutableState<Boolean>,
        modifier: Modifier = Modifier,
        shape: Shape = CircleShape,
        close: () -> Unit
    ) {
        val player = LocalPlayerState.current
        val expansion = LocalNowPlayingExpansion.current
        val focus_requester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            if (expansion.getPage() == 0 && current_results == null && !search_in_progress) {
                focus_requester.requestFocus()
            }
        }

        var suggestions: List<String> by remember { mutableStateOf(emptyList()) }

        LaunchedEffect(focus_state.value) {
            if (!focus_state.value) {
                suggestions = emptyList()
            }
        }

        LaunchedEffect(current_query, focus_state.value) {
            if (search_in_progress || !focus_state.value) {
                return@LaunchedEffect
            }

            val query = current_query
            if (query.isBlank()) {
                suggestions = emptyList()
                return@LaunchedEffect
            }

            delay(SEARCH_SUGGESTIONS_LOAD_DELAY_MS)

            val suggestions_endpoint = player.context.ytapi.SearchSuggestions
            if (!suggestions_endpoint.isImplemented()) {
                suggestions = emptyList()
                return@LaunchedEffect
            }

            suggestions = suggestions_endpoint
                .getSearchSuggestions(query).getOrNull()?.take(SEARCH_MAX_SUGGESTIONS)?.asReversed()
                    ?: emptyList()
        }

        Column(modifier) {
            AnimatedVisibility(suggestions.isNotEmpty()) {
                var current_suggestions: List<String> by remember { mutableStateOf(suggestions) }
                LaunchedEffect(suggestions) {
                    if (suggestions.isNotEmpty()) {
                        current_suggestions = suggestions
                    }
                }

                AlignableCrossfade(
                    current_suggestions,
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
                    ) {
                        for (suggestion in it) {
                            SearchSuggestion(suggestion, shape) {
                                if (!search_in_progress) {
                                    current_query = suggestion
                                    current_suggestions = emptyList()
                                    performSearch()
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SEARCH_BAR_V_PADDING_DP.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = current_query,
                    onValueChange = { current_query = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = SEARCH_FIELD_FONT_SIZE,
                        color = Theme.on_accent
                    ),
                    modifier = Modifier
                        .height(SEARCH_BAR_HEIGHT_DP.dp)
                        .weight(1f)
                        .focusRequester(focus_requester)
                        .onFocusChanged {
                            focus_state.value = it.isFocused
                        },
                    decorationBox = { innerTextField ->
                        Row(
                            Modifier
                                .background(
                                    Theme.accent,
                                    shape
                                )
                                .padding(horizontal = 10.dp)
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Search field
                            Box(Modifier.fillMaxWidth(0.9f), contentAlignment = Alignment.CenterStart) {

                                // Query hint
                                if (current_query.isEmpty()) {
                                    Text(getString("search_entry_field_hint"), fontSize = SEARCH_FIELD_FONT_SIZE, color = Theme.on_accent)
                                }

                                // Text input
                                innerTextField()
                            }

                            // Clear field button
                            IconButton(onClick = { current_query = "" }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Clear, null, Modifier, Theme.on_accent)
                            }

                            // Search button / search indicator
                            Crossfade(search_in_progress) { in_progress ->
                                if (!in_progress) {
                                    IconButton(onClick = {
                                        if (!search_in_progress) {
                                            performSearch()
                                        }
                                    }) {
                                        Icon(Icons.Filled.Search, null)
                                    }
                                }
                                else {
                                    CircularProgressIndicator(Modifier.size(30.dp))
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (!search_in_progress) {
                                performSearch()
                            }
                        }
                    )
                )

                ShapedIconButton(
                    { performSearch() },
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    colours = IconButtonDefaults.iconButtonColors(
                        containerColor = Theme.accent,
                        contentColor = Theme.on_accent
                    )
                ) {
                    Icon(Icons.Filled.Search, null)
                }
            }
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
                // TODO
                Text(results.suggested_correction)
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
