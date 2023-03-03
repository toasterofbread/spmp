package com.spectre7.spmp.ui.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.api.getOrThrowHere
import com.spectre7.spmp.api.searchYoutubeMusic
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.theme.Theme
import kotlin.concurrent.thread

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp

fun YTMusicSearchResults.getLayouts(): List<MediaItemLayout> {
    return listOf(
        MediaItemLayout(MediaItem.Type.SONG.readable(true), null, items = songs)
        MediaItemLayout(getString(R.string.videos), null, items = videos)
        MediaItemLayout(MediaItem.Type.ARTIST.readable(true), null, items = artists)
        MediaItemLayout(MediaItem.Type.PLAYLIST.readable(true), null, items = playlists)
        MediaItemLayout(getString(R.string.albums), null, items = albums)
    )
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
    val focus_requester = remember { FocusRequester() }
    val keyboard_controller = LocalSoftwareKeyboardController.current

    var search_in_progress: Boolean by remember { mutableStateOf(false) }
    val search_lock = remember { Object() }

    var type_to_search: MediaItem.Type? by remember { mutableStateOf(null) }
    var query_text by remember { mutableStateOf("") }

    val current_results: List<MediaItemLayout>? by remember { mutableStateOf(null) }

    // TODO
    var error: Throwable? by remember { mutableStateOf(null) }
    
    fun goBack() {
        focus_manager.clearFocus()
        keyboard_controller?.hide()
        close()
    }

    fun performSearch(query: String, type: MediaItem.Type) {
        synchronized(search_lock) {
            if (search_in_progress) {
                return
            }
            search_in_progress = true
        }

        thread {
            val results = searchYoutubeMusic(query, type).getOrThrowHere()

            synchronized(search_lock) {
                current_results = results.getLayouts()
                search_in_progress = false
            }
        }
    }

    LaunchedEffect(Unit) {
        pill_menu.top = true
    }

    Column(
        Modifier.fillMaxSize().padding(bottom = bottom_padding),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (current_results != null) {
            LazyMediaItemLayoutColumn(
                current_results,
                playerProvider,
                Modifier.fillMaxWidth()
            )
        }


        // Bottom bar
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {

            var type_selector_open by remember { mutableStateOf(false) }

            Box(Modifier.clip(CircleShape), contentAlignment = Alignment.BottomCenter) {
                this@Row.AnimatedVisibility(
                    !type_selector_open,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton({ type_selector_open = true }, Modifier.background(Theme.current.accent, CircleShape)) {
                        Icon(
                            type_to_search.getIcon(),
                            null,
                            tint = Theme.current.on_accent
                        )
                    }
                }

                this@Row.AnimatedVisibility(
                    type_selector_open,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    Column(Modifier.background(Theme.current.accent, CircleShape)) {
                        val types = MediaItem.Type.values()
                        for (i in types.size - 1 downTo 0) {
                            val type = if (i == 0) type_to_search else if (i - 1 < type_to_search.ordinal) types[i - 1] else types[i]
                            IconButton({
                                type_to_search = type
                                type_selector_open = false
                            }) {
                                Icon(
                                    type.getIcon(),
                                    null,
                                    tint = Theme.current.on_accent
                                )
                            }
                        }
                    }
                }
            }

            // Query input field
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
                                    MediaItem.Type.SONG -> "Search for songs"
                                    MediaItem.Type.ARTIST -> "Search for artists"
                                    MediaItem.Type.PLAYLIST -> "Search for playlists"
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
                                        performSearch(query_text, type_to_search)
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
                            performSearch(query_text, type_to_search)
                        }
                    }
                )
            )
        }
    }

    BackHandler {
        goBack()
    }
}
