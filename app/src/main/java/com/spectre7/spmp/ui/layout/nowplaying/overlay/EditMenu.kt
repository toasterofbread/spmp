package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.getContrasted
import com.spectre7.utils.getString
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class)
@Composable
fun EditMenu(song: Song, openShutterMenu: (@Composable () -> Unit) -> Unit, close: () -> Unit) {
    val tab_state = rememberPagerState()
    val scope = rememberCoroutineScope()

    var main_title = remember { mutableStateOf(TextFieldValue()) }
    var main_artist_id = remember { mutableStateOf(TextFieldValue()) }

    var lyrics_title = remember { mutableStateOf(TextFieldValue()) }
    var lyrics_artist = remember { mutableStateOf(TextFieldValue()) }

    var lyrics_use_main by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {

        PillMenu(
            action_count = 3,
            getAction = { i, _ ->
                when (i) {
                    0 ->  {
                        ActionButton(Icons.Filled.Check) {

                        }
                    }
                    1 -> {
                        ActionButton(Icons.Filled.Info) {
                            openShutterMenu {
                                Text(getString(R.string.song_details_edit_info))
                            }
                        }
                    }
                    else -> ActionButton(Icons.Filled.Close, close)
                }
            },
            expand_state = null,
            background_colour = MainActivity.theme.getAccent(),
            content_colour = MainActivity.theme.getOnAccent()
        )

        Column(
            Modifier.padding(25.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            TabRow(
                tab_state.targetPage,
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .height(30.dp)
                    .fillMaxWidth(0.75f),
                containerColor = MainActivity.theme.getOnAccent(),
                contentColor = MainActivity.theme.getAccent(),
                indicator = { tab_positions ->
                    val offset =
                        if (tab_state.currentPageOffset == 0.0f) tab_positions[tab_state.currentPage].left
                        else tab_positions[tab_state.currentPage].left + (tab_positions[1].left - tab_positions[0].left) * tab_state.currentPageOffset

                    if (!offset.isUnspecified) {
                        Box(
                            Modifier
                                .requiredWidth(tab_positions[tab_state.targetPage].width)
                                .offset(offset - (tab_positions[tab_state.targetPage].width / 2))
                                .background(
                                    MainActivity.theme.getAccent(),
                                    RoundedCornerShape(16.dp)
                                )
                                .zIndex(-1f)
                        )
                    }
                }
            ) {
                for (i in 0 until 2) {
                    Tab(true, {
                        scope.launch {
                            tab_state.animateScrollToPage(i)
                        }
                    }) {

                        val offset = tab_state.currentPageOffset.absoluteValue
                        if (!offset.isNaN()) {
                            val width = if (tab_state.currentPage == i) offset else 1 - offset
                            val text = when (i) {
                                0 -> getString(R.string.song_details_edit_main)
                                else -> getString(R.string.song_details_edit_lyrics)
                            }

                            Box {
                                Text(text, color = MainActivity.theme.getAccent().getContrasted())
                                Text(text, Modifier.clip(object : Shape {
                                    override fun createOutline(
                                        size: Size,
                                        layoutDirection: LayoutDirection,
                                        density: Density
                                    ): Outline {
                                        return Outline.Rectangle(
                                            if (i == 0 || width == 0f) {
                                                Rect(0f, 0f, size.width * width, size.height)
                                            } else {
                                                Rect(
                                                    size.width * (1 - width),
                                                    0f,
                                                    size.width,
                                                    size.height
                                                )
                                            }
                                        )
                                    }

                                    override fun toString(): String = "RectangleShape"
                                }), color = MainActivity.theme.getAccent())
                            }
                        }
                    }
                }
            }

            HorizontalPager(2, state = tab_state) { page ->
                Column(Modifier.padding(start = 10.dp, end = 10.dp).fillMaxHeight(0.8f), verticalArrangement = Arrangement.SpaceAround) {
                    val focus = LocalFocusManager.current

                    @Composable
                    fun Field(state: MutableState<TextFieldValue>, placeholder: String, label: String) {
                        TextField(
                            state.value,
                            { state.value = it },
                            singleLine = true,
                            placeholder = {
                                Text(placeholder, softWrap = false, overflow = TextOverflow.Ellipsis)
                            },
                            label = {
                                Text(label)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focus.clearFocus()
                            })
                        )
                    }

                    when (page) {
                        0 -> {
                            Field(main_title, song.title, "Title")
                            Field(main_artist_id, song.artist.id, "Artist ID")
                        }
                        else -> {
                            Field(lyrics_title, song.title, "Title")
                            Field(lyrics_artist, song.artist.name, "Artist")
                        }
                    }

                    if (page == 1) {
                        Switch(
                            lyrics_use_main,
                            { lyrics_use_main = it }
                        )
                    }
                }
            }
        }
    }
}
