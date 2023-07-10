package com.toasterofbread.spmp.ui.layout

import LocalPlayerState
import SpMp
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.toasterofbread.spmp.api.*
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.enums.getReadable
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.getDefaultHorizontalPadding
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.ErrorInfoDisplay
import com.toasterofbread.spmp.ui.component.MediaItemLayout
import com.toasterofbread.spmp.ui.component.MusicTopBar
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.*
import com.toasterofbread.utils.composable.ShapedIconButton
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
private val SEARCH_BAR_HEIGHT = 45.dp
private val SEARCH_BAR_PADDING = 15.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    close: () -> Unit
) {
    val focus_state = remember { mutableStateOf(false) }
    val focus_manager = LocalFocusManager.current
    val player = LocalPlayerState.current
    val multiselect_context = remember { MediaItemMultiSelectContext() {} }
    val coroutine_scope = rememberCoroutineScope()
    val keyboard_controller = LocalSoftwareKeyboardController.current

    var search_in_progress: Boolean by remember { mutableStateOf(false) }
    val search_lock = remember { Object() }

    var current_results: SearchResults? by remember { mutableStateOf(null) }
    var current_query: String? by remember { mutableStateOf(null) }
    var current_filter: SearchType? by remember { mutableStateOf(null) }

    // TODO
    var error: Throwable? by remember { mutableStateOf(null) }

    fun performSearch(query: String, filter: SearchFilter? = null) {
        focus_manager.clearFocus()
        keyboard_controller?.hide()

        synchronized(search_lock) {
            search_in_progress = true

            current_results = null
            current_query = query
            current_filter = filter?.type
            multiselect_context.setActive(false)

            coroutine_scope.launchSingle {
                searchYoutubeMusic(query, filter?.params).fold(
                    { results ->
                        for (result in results.categories) {
                            if (result.second != null) {
                                result.first.view_more = MediaItemLayout.ViewMore(
                                    action = {
                                        performSearch(current_query!!, result.second)
                                    }
                                )
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

    BackHandler(focus_state.value) {
        focus_manager.clearFocus()
        keyboard_controller?.hide()
    }

    LaunchedEffect(Unit) {
        pill_menu.showing = false
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = SpMp.context.getDefaultHorizontalPadding())
        ) {
            val status_bar_height = SpMp.context.getStatusBarHeight()
            var top_bar_showing by remember { mutableStateOf(false) }

            MusicTopBar(
                Settings.KEY_LYRICS_SHOW_IN_SEARCH,
                Modifier.fillMaxWidth().padding(top = status_bar_height),
                onShowingChanged = { top_bar_showing = it }
            )

            AnimatedVisibility(multiselect_context.is_active) {
                multiselect_context.InfoDisplay(
                    Modifier.padding(
                        top = animateDpAsState(if (!top_bar_showing) status_bar_height else 0.dp).value
                    )
                )
            }

            val total_bottom_padding = bottom_padding + (SEARCH_BAR_HEIGHT) + (SEARCH_BAR_PADDING)

            Crossfade(
                error ?: current_results
            ) { results ->
                if (results is SearchResults) {
                    Results(
                        results,
                        PaddingValues(
                            top = animateDpAsState(
                                if (!top_bar_showing && !multiselect_context.is_active) status_bar_height else 0.dp
                            ).value,
                            bottom = total_bottom_padding
                        ),
                        multiselect_context
                    )
                }
                else if (results is Throwable) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = status_bar_height, bottom = total_bottom_padding),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorInfoDisplay(results, Modifier.fillMaxWidth())
                    }
                }
                else if (search_in_progress) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            SubtleLoadingIndicator(colourProvider = { Theme.current.on_background }, size = 20.dp)
                        }
                        Text(getString("search_results_loading"), Modifier.padding(top = 5.dp))
                    }
                }
            }
        }

        SearchBar(
            search_in_progress,
            focus_state,
            current_filter,
            player.nowPlayingTopOffset(Modifier.align(Alignment.BottomCenter)),
            { query, filter ->
                performSearch(query, filter?.let { SearchFilter(it, it.getDefaultParams()) })
            },
            { filter ->
                if (current_query != null) {
                    performSearch(current_query!!, filter?.let { SearchFilter(it, it.getDefaultParams()) })
                }
            },
            close
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    search_in_progress: Boolean,
    focus_state: MutableState<Boolean>,
    filter: SearchType?,
    modifier: Modifier = Modifier,
    requestSearch: (String, SearchType?) -> Unit,
    onFilterChanged: (SearchType?) -> Unit,
    close: () -> Unit
) {
    var query_text by remember { mutableStateOf("") }
    val focus_requester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focus_requester.requestFocus()
    }

    Column(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(SEARCH_BAR_PADDING)
    ) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .height(SEARCH_BAR_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (type in listOf(null) + SearchType.values()) {
                FilterChip(
                    selected = filter == type,
                    onClick = {
                        if (filter != type) {
                            onFilterChanged(type)
                        }
                    },
                    label = {
                        Text(when (type) {
                            null -> getString("search_filter_all")
                            SearchType.VIDEO -> getString("search_filter_videos")
                            SearchType.SONG -> MediaItemType.SONG.getReadable(true)
                            SearchType.ARTIST -> MediaItemType.ARTIST.getReadable(true)
                            SearchType.PLAYLIST -> PlaylistType.PLAYLIST.getReadable(true)
                            SearchType.ALBUM -> PlaylistType.ALBUM.getReadable(true)
                        })
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Theme.current.background,
                        labelColor = Theme.current.on_background,
                        selectedContainerColor = Theme.current.accent,
                        selectedLabelColor = Theme.current.on_accent
                    )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ShapedIconButton(
                close,
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Theme.current.accent,
                    contentColor = Theme.current.on_accent
                )
            ) {
                Icon(Icons.Filled.Close, null)
            }

            BasicTextField(
                value = query_text,
                onValueChange = { query_text = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = SEARCH_FIELD_FONT_SIZE,
                    color = Theme.current.on_accent
                ),
                modifier = Modifier
                    .height(SEARCH_BAR_HEIGHT)
                    .weight(1f)
                    .focusRequester(focus_requester)
                    .onFocusChanged {
                        focus_state.value = it.isFocused
                    },
                decorationBox = { innerTextField ->
                    Row(
                        Modifier
                            .background(
                                Theme.current.accent,
                                CircleShape
                            )
                            .padding(horizontal = 10.dp)
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Search field
                        Box(Modifier.fillMaxWidth(0.9f), contentAlignment = Alignment.CenterStart) {

                            // Query hint
                            if (query_text.isEmpty()) {
                                Text(getString("search_entry_field_hint"), fontSize = SEARCH_FIELD_FONT_SIZE, color = Theme.current.on_accent)
                            }

                            // Text input
                            innerTextField()
                        }

                        // Clear field button
                        IconButton(onClick = { query_text = "" }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Clear, null, Modifier, Theme.current.on_accent)
                        }

                        // Search button / search indicator
                        Crossfade(search_in_progress) { in_progress ->
                            if (!in_progress) {
                                IconButton(onClick = {
                                    if (!search_in_progress) {
                                        requestSearch(query_text, null)
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
                            requestSearch(query_text, null)
                        }
                    }
                )
            )

            ShapedIconButton(
                { requestSearch(query_text, null) },
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Theme.current.accent,
                    contentColor = Theme.current.on_accent
                )
            ) {
                Icon(Icons.Filled.Search, null)
            }
        }
    }
}
