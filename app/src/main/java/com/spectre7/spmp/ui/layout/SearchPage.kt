package com.spectre7.spmp.ui.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.api.searchYoutubeMusic
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.LazyMediaItemLayoutColumn
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.getStatusBarHeight
import kotlin.concurrent.thread

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp

private enum class SearchType {
    ALL, SONG, VIDEO, PLAYLIST, ALBUM, ARTIST;

    fun getIcon(): ImageVector {
        return when (this) {
            ALL -> Icons.Filled.AllInclusive
            SONG -> MediaItem.Type.SONG.getIcon()
            PLAYLIST -> MediaItem.Type.ARTIST.getIcon()
            ARTIST -> MediaItem.Type.PLAYLIST.getIcon()
            VIDEO -> Icons.Filled.PlayArrow
            ALBUM -> Icons.Filled.Album
        }
    }

    fun getParams(): String? {
        return when (this) {
            ALL -> null
            SONG -> "EgWKAQIIAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            PLAYLIST -> "EgWKAQIoAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ARTIST -> "EgWKAQIgAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            VIDEO -> "EgWKAQIQAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            ALBUM -> "EgWKAQIYAUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(
    pill_menu: PillMenu,
    bottom_padding: Dp,
    playerProvider: () -> PlayerViewContext,
    close: () -> Unit
) {

    val focus_manager = LocalFocusManager.current
    val keyboard_controller = LocalSoftwareKeyboardController.current

    var search_in_progress: Boolean by remember { mutableStateOf(false) }
    val search_lock = remember { Object() }

    var current_results: List<Pair<MediaItemLayout, String?>>? by remember { mutableStateOf(null) }

    // TODO
    var error: Throwable? by remember { mutableStateOf(null) }
    
    fun goBack() {
        focus_manager.clearFocus()
        keyboard_controller?.hide()
        close()
    }

    fun performSearch(query: String, params: String?) {
        synchronized(search_lock) {
            if (search_in_progress) {
                return
            }
            search_in_progress = true
        }

        thread {
            val results = searchYoutubeMusic(query, params).getOrThrowHere()

            synchronized(search_lock) {
                current_results = results
                search_in_progress = false
            }
        }
    }

    LaunchedEffect(Unit) {
        pill_menu.top = true
    }

    Box(Modifier.fillMaxSize()) {
        if (current_results != null) {
            LazyMediaItemLayoutColumn(
                current_results!!.map { it.first },
                playerProvider,
                Modifier.fillMaxSize(),
                padding = PaddingValues(top = getStatusBarHeight(MainActivity.context), bottom = 50.dp),
                vertical_arrangement = Arrangement.spacedBy(30.dp)
            ) { layout ->
                return@LazyMediaItemLayoutColumn MediaItemLayout.Type.LIST
            }
        }

        SearchBar(search_in_progress, Modifier.align(Alignment.BottomCenter)) { query, type ->
            performSearch(query, type.getParams())
        }
    }

    BackHandler {
        goBack()
    }
}

@Composable
private fun SearchBar(search_in_progress: Boolean, modifier: Modifier = Modifier, requestSearch: (String, SearchType) -> Unit) {
    val focus_requester = remember { FocusRequester() }
    var type_to_search: SearchType by remember { mutableStateOf(SearchType.ALL) }
    var query_text by remember { mutableStateOf("") }

    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(15.dp),
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
            modifier = Modifier.height(45.dp).weight(1f),
            decorationBox = { innerTextField ->
                Row(
                    Modifier
                        .background(
                            Theme.current.accent,
                            CircleShape
                        )
                        .padding(10.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .focusRequester(focus_requester),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Search field
                    Box(Modifier.fillMaxWidth(0.9f), contentAlignment = Alignment.CenterStart) {

                        // Query hint
                        if (query_text.isEmpty()) {
                            Text(when (type_to_search) {
                                SearchType.ALL -> "Search"
                                SearchType.SONG -> "Search for songs"
                                SearchType.PLAYLIST -> "Search for artists"
                                SearchType.ARTIST -> "Search for playlists"
                                SearchType.VIDEO -> "Search for videos"
                                SearchType.ALBUM -> "Search for albums"
                            }, fontSize = SEARCH_FIELD_FONT_SIZE, color = Theme.current.on_accent)
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
                                    requestSearch(query_text, type_to_search)
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
                        requestSearch(query_text, type_to_search)
                    }
                }
            )
        )
    }
}