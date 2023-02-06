package com.spectre7.spmp.ui.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.api.searchYoutube
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.PillMenu
import kotlin.concurrent.thread

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(
    pill_menu: PillMenu,
    player: PlayerViewContext,
    close: () -> Unit
) {

    val focus_manager = LocalFocusManager.current
    val focus_requester = remember { FocusRequester() }
    val keyboard_controller = LocalSoftwareKeyboardController.current

    var search_in_progress: Boolean by remember { mutableStateOf(false) }
    val search_lock = remember { Object() }

    var type_to_search: MediaItem.Type = MediaItem.Type.SONG
    var query_text by remember { mutableStateOf("") }

    val found_results = remember { mutableStateListOf<MediaItem>() }
    
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
            val results = searchYoutube(query, type).getDataOrThrow()
            
            synchronized(search_lock) {
                found_results.clear()

                for (result in results) {
                    when (result.id.kind) {
                        "youtube#video" -> {
                            found_results.add(Song.fromId(result.id.videoId).loadData())
                        }
                        "youtube#channel" -> {
                            found_results.add(Artist.fromId(result.id.channelId).loadData())
                        }
                        "youtube#playlist" -> {
                            found_results.add(Playlist.fromId(result.id.playlistId).loadData())
                        }
                        else -> throw NotImplementedError(result.id.kind)
                    }
                }

                search_in_progress = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Search results
        LazyColumn(Modifier.fillMaxHeight().weight(1f)) {
            itemsIndexed(items = found_results, key = { _, item -> item.id }) { _, item ->
                item.PreviewLong(
                    MainActivity.theme.getOnBackgroundProvider(false),
                    player,
                    true,
                    Modifier
                )
            }
        }

        // Bottom bar
        Row(Modifier.fillMaxWidth()) {

            @Composable
            fun PillMenu.Action.resourceTypeAction(type: MediaItem.Type, action: () -> Unit) {
                ActionButton(
                    when (type) {
                        MediaItem.Type.SONG     -> Icons.Filled.MusicNote
                        MediaItem.Type.ARTIST   -> Icons.Filled.Person
                        MediaItem.Type.PLAYLIST -> Icons.Filled.FeaturedPlayList
                    },
                    action
                )
            }

            val type_menu = remember {
                PillMenu(
                    action_count = MediaItem.Type.values().size - 1,
                    getAction = { i, _ ->
                        val type = MediaItem.Type.values().filter{ it != type_to_search }[i]
                        resourceTypeAction(type) {
                            type_to_search = type
                        }
                    },
                    mutableStateOf(false),
                    container_modifier = Modifier,
                    vertical = true,
                    top = true,
                    toggleButton = { modifier ->
                        Crossfade(type_to_search) { type ->
                            resourceTypeAction(type) {
                                is_open = !is_open
                            }
                        }
                    }
                ) 
            }

            type_menu.PillMenu()

            // Query input field
            BasicTextField(
                value = query_text,
                onValueChange = { query_text = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = SEARCH_FIELD_FONT_SIZE, color = MainActivity.theme.getOnAccent()),
                modifier = Modifier.height(45.dp),
                decorationBox = { innerTextField ->
                    Row(
                        Modifier
                            .background(
                                MainActivity.theme.getAccent(),
                                RoundedCornerShape(percent = 35)
                            )
                            .padding(10.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .focusRequester(focus_requester),
                        Arrangement.End
                    ) {

                        // Search field
                        Box(Modifier.fillMaxWidth(0.9f)) {

                            // Query hint
                            if (query_text.isEmpty()) {
                                Text(when (type_to_search) {
                                    MediaItem.Type.SONG -> "Search for songs"
                                    MediaItem.Type.ARTIST -> "Search for artists"
                                    MediaItem.Type.PLAYLIST -> "Search for playlists"
                                }, fontSize = SEARCH_FIELD_FONT_SIZE, color = MainActivity.theme.getOnAccent())
                            }

                            // Text input
                            innerTextField()
                        }

                        // Clear field button
                        IconButton(onClick = { query_text = "" }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Clear, null, Modifier, MainActivity.theme.getOnAccent())
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
                    CircularProgressIndicator()
                }
            }

        }
    }

    BackHandler {
        goBack()
    }
}
