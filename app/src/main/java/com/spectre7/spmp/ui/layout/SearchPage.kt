package com.spectre7.spmp.ui.layout

import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.os.Process.setThreadPriority
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.api.DataApi
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.Playlist
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.model.MediaItem
import com.spectre7.utils.getString
import com.spectre7.utils.setAlpha
import com.spectre7.spmp.ui.component.PillMenu
import kotlin.concurrent.thread

val SEARCH_FIELD_FONT_SIZE: TextUnit = 18.sp
val TAB_TEXT_FONT_SIZE: TextUnit = 14.sp

enum class ResourceType {
    SONG, ARTIST, PLAYLIST
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(pill_menu: PillMenu, setOverlayPage: (page: OverlayPage) -> Unit) {

    var tab_index by remember { mutableStateOf(0) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val result_tabs: MutableMap<ResourceType, SnapshotStateList<MediaItem>> = mutableMapOf()

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
//            for (result in search) {
//                when (result.id.kind) {
//                    "youtube#video" -> {
//                        Song.fromId(result.id.videoId).loadData {
//                            if (it != null) {
//                                result_tabs[ResourceType.SONG]?.add(it as Song)
//                            }
//                        }
//                    }
//                    "youtube#channel" -> {
//                        Artist.fromId(result.id.channelId).loadData {
//                            if (it != null) {
//                                result_tabs[ResourceType.ARTIST]?.add(it as Artist)
//                            }
//                        }
//                    }
//                    "youtube#playlist" -> {
//                        Playlist.fromId(result.id.playlistId).loadData {
//                            if (it != null) {
//                                result_tabs[ResourceType.PLAYLIST]?.add(it as Playlist)
//                            }
//                        }
//                    }
//                }
//            }
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

        setOverlayPage(OverlayPage.NONE)
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
                            .focusRequester(focusRequester),
                        Arrangement.End
                    ) {

                        // Search field
                        Box(Modifier.fillMaxWidth(0.9f)) {

                            // Query hint
                            if (input.isEmpty()) {
                                Text("Search query", fontSize = SEARCH_FIELD_FONT_SIZE, color = MainActivity.theme.getOnAccent())
                            }

                            // Text input
                            innerTextField()
                        }

                        // Clear field button
                        IconButton(onClick = { input = "" }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Clear, null, Modifier, MainActivity.theme.getOnAccent())
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
        TabRow(
            selectedTabIndex = tab_index,
            modifier = Modifier.fillMaxWidth(),
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[tab_index]),
                    color = MainActivity.theme.getVibrantAccent()
                )
            }
        ) {
            result_tabs.keys.forEachIndexed { i, type ->
                Tab(selected = tab_index == i, onClick = {
                    tab_index = i
                }, text = {
                    Text(text = getReadableType(type), fontSize = TAB_TEXT_FONT_SIZE)
                },
                    selectedContentColor = MainActivity.theme.getVibrantAccent(),
                    unselectedContentColor = MainActivity.theme.getVibrantAccent().setAlpha(0.75)
                )
            }
        }

        performSearch(result_tabs.keys.elementAt(tab_index))

        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(MainActivity.theme.getBackground(false))) {
            itemsIndexed(items = result_tabs.values.elementAt(tab_index), key = { _, item -> item.id }) { _, item ->
                item.PreviewLong(
                    content_colour = MainActivity.theme.getOnBackground(false),
                    onClick = null,
                    onLongClick = null,
                    modifier = Modifier
                )
            }
        }
    }
}