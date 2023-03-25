package com.spectre7.spmp.ui.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.*
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.*
import kotlin.concurrent.thread

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
private val SEARCH_BAR_HEIGHT = 45.dp
private val SEARCH_BAR_PADDING = 15.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {
    val focus_state = remember { mutableStateOf(false) }
    val focus_manager = LocalFocusManager.current
    val keyboard_controller = LocalSoftwareKeyboardController.current

    var search_in_progress: Boolean by remember { mutableStateOf(false) }
    val search_lock = remember { Object() }

    var current_results: List<Pair<MediaItemLayout, SearchFilter?>>? by remember { mutableStateOf(null) }
    var current_query: String? by remember { mutableStateOf(null) }
    var current_filter: SearchType? by remember { mutableStateOf(null) }

    // TODO
    var error: Throwable? by remember { mutableStateOf(null) }

    fun performSearch(query: String, filter: SearchFilter? = null) {
        synchronized(search_lock) {
            if (search_in_progress) {
                return
            }
            search_in_progress = true
            current_results = null
        }

        thread {
            searchYoutubeMusic(query, filter?.params).fold(
                { results ->
                    for (result in results) {
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
                        current_query = query
                        current_filter = filter?.type
                        search_in_progress = false
                    }
                },
                {
                    MainActivity.error_manager.onError("SearchPage", it)
                    synchronized(search_lock) {
                        search_in_progress = false
                    }
                }
            )
        }
    }

    BackHandler(current_filter != null || focus_state.value) {
        if (focus_state.value) {
            focus_manager.clearFocus()
            keyboard_controller?.hide()
        }
        else {
            performSearch(current_query!!)
        }
    }

    LaunchedEffect(Unit) {
        pill_menu.top = true

        // DEBUG
        performSearch("やっぱり雨は降るんだね", null)
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(current_results) { results ->
            if (results != null) {
                Results(
                    results.map { it.first },
                    playerProvider,
                    bottom_padding + SEARCH_BAR_HEIGHT + (SEARCH_BAR_PADDING * 2)
                )
            }
            else if (search_in_progress) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        SubtleLoadingIndicator(Theme.current.on_background, size = 20.dp)
                    }
                    Text(getString("Loading"), Modifier.padding(top = 20.dp))
                }
            }
            else {
                // TODO
                Text(getString(""))
            }
        }

        val screen_height = getScreenHeight()
        SearchBar(
            search_in_progress,
            focus_state,
            Modifier
                .align(Alignment.BottomCenter)
                .offset {
                    IntOffset(0, playerProvider().getNowPlayingTopOffset(screen_height, this))
                }
        ) { query, filter ->
            performSearch(query, filter)
        }
    }
}

@Composable
private fun Results(layouts: List<MediaItemLayout>, playerProvider: () -> PlayerViewContext, bottom_padding: Dp) {
    val horizontal_padding = 10.dp
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = getStatusBarHeight() + 20.dp,
            bottom = bottom_padding,
            start = horizontal_padding,
            end = horizontal_padding
        ),
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        for (layout in layouts) {
            item {
                (layout.type ?: MediaItemLayout.Type.LIST).Layout(layout, playerProvider)
            }
        }
    }
}

@Composable
private fun SearchBar(
    search_in_progress: Boolean,
    focus_state: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    requestSearch: (String, SearchFilter?) -> Unit
) {
    val focus_requester = remember { FocusRequester() }
    var query_text by remember { mutableStateOf("") }

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(SEARCH_BAR_PADDING),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {

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
                .weight(1f),
            decorationBox = { innerTextField ->
                Row(
                    Modifier
                        .background(
                            Theme.current.accent,
                            CircleShape
                        )
                        .padding(horizontal = 10.dp)
                        .fillMaxSize()
                        .focusRequester(focus_requester)
                        .onFocusChanged {
                            focus_state.value = it.isFocused
                        },
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Search field
                    Box(Modifier.fillMaxWidth(0.9f), contentAlignment = Alignment.CenterStart) {

                        // Query hint
                        if (query_text.isEmpty()) {
                            Text(getString("検索"), fontSize = SEARCH_FIELD_FONT_SIZE, color = Theme.current.on_accent)
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