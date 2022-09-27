package com.spectre7.spmp.ui.layout

import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.os.Process.setThreadPriority
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.spectre7.utils.getString
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Previewable
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.model.SongData
import kotlin.concurrent.thread
import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
val TAB_TEXT_FONT_SIZE: TextUnit = 14.sp

enum class ResourceType {
    SONG, ARTIST, PLAYLIST
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(set_overlay_page: (page: OverlayPage) -> Unit) {

    var tab_index by remember { mutableStateOf(0) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val result_tabs: MutableMap<ResourceType, SnapshotStateList<Previewable>> = mutableMapOf()

    var current_search_query: String? = null

    val search_performed: MutableMap<ResourceType, Boolean> = mutableMapOf()
    for (type in ResourceType.values()) {
        search_performed[type] = false
    }

    fun performSearch(type: ResourceType) {

        if (current_search_query == null || search_performed[type]!!) {
            return
        }

        search_performed[type] = true

        // Hide keyboard and clear search field focus
        keyboardController?.hide()
        focusManager.clearFocus()

        thread {

            setThreadPriority(THREAD_PRIORITY_BACKGROUND)

            // Perform search with passed query
            val search = DataApi.search(current_search_query!!, type)

            // Display new search results
            for (result in search) {
                when (result.id.kind) {
                    "youtube#video" -> {
                        result_tabs[ResourceType.SONG]?.add(Song(
                            result.id.videoId, SongData(null, result.snippet.title, result.snippet.description), Artist.fromId(result.snippet.channelId)
                        ))
                        result_tabs[ResourceType.SONG]?.add(Song(
                            result.id.videoId + "1", SongData(null, result.snippet.title, result.snippet.description), Artist.fromId(result.snippet.channelId)
                        ))
                    }
                    "youtube#channel" -> result_tabs[ResourceType.ARTIST]?.add(Artist.fromId(result.id.channelId))
                    "youtube#playlist" -> {} // TODO
                }
            }
        }
    }

    fun setSearchQuery(query: String?) {
        for (type in ResourceType.values()) {
            search_performed[type] = false
        }

        // Clear previous search results
        for (results in result_tabs.values) {
            results.clear()
        }

        current_search_query = query

        if (current_search_query != null) {
            performSearch(ResourceType.values()[tab_index])
        }
    }

    fun navigateBack() {
        focusManager.clearFocus()
        keyboardController?.hide()

        set_overlay_page(OverlayPage.NONE)
    }

    BackHandler {
        navigateBack()
    }

    var input by rememberSaveable { mutableStateOf("") }

    SmallTopAppBar(
        title = {
            BasicTextField(
                value = input,
                onValueChange = { text ->  input = text },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = SEARCH_FIELD_FONT_SIZE, color = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.height(45.dp),
                decorationBox = { innerTextField ->
                    Row(
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(percent = 35)
                            )
                            .padding(10.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .focusRequester(focusRequester),
                        Arrangement.End
                    ) {

                        // Search field
                        Box(Modifier.fillMaxWidth(0.9f)) {

                            // Query hint
                            if (input.isEmpty()) {
                                Text("Search query", fontSize = SEARCH_FIELD_FONT_SIZE, color = MaterialTheme.colorScheme.onPrimary)
                            }

                            // Text input
                            innerTextField()
                        }

                        // Clear field button
                        IconButton(onClick = { input = "" }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Clear, null, Modifier, MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { setSearchQuery(input) }
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = { navigateBack() }) {
                Icon(Icons.Filled.ArrowBack,"")
            }
        },
        actions = {
            IconButton(onClick = { setSearchQuery(input) }) {
                Icon(Icons.Filled.Search, "")
            }
        }
    )

    for (type in ResourceType.values()) {
        result_tabs[type] = remember { mutableStateListOf() }
    }

    fun getReadableType(type: ResourceType): String {
        return when (type) {
            ResourceType.SONG -> getString(R.string.songs)
            ResourceType.ARTIST -> getString(R.string.artists)
            ResourceType.PLAYLIST -> getString(R.string.playlists)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab_index, Modifier.fillMaxWidth()) {
            result_tabs.keys.forEachIndexed { i, type ->
                Tab(selected = tab_index == i, onClick = {
                    tab_index = i
                }, text = {
                    Text(text = getReadableType(type), fontSize = TAB_TEXT_FONT_SIZE)
                })
            }
        }

        performSearch(result_tabs.keys.elementAt(tab_index))

        LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            itemsIndexed(items = result_tabs.values.elementAt(tab_index), key = { _, item -> item.getId() }) { _, item ->
                item.Preview(false)
            }
        }
    }
}