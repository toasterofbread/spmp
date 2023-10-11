@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.platform.composable.PlatformAlertDialog
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import com.toasterofbread.utils.common.AnnotatedReadingTerm
import com.toasterofbread.utils.common.launchSingle
import com.toasterofbread.utils.common.setAlpha
import com.toasterofbread.utils.composable.SubtleLoadingIndicator

private enum class Submenu {
    SEARCH, SYNC
}

class LyricsPlayerOverlayMenu: PlayerOverlayMenu() {
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val player = LocalPlayerState.current
        val coroutine_scope = rememberCoroutineScope()
        val scroll_state = rememberLazyListState()
        val pill_menu = remember { PillMenu(expand_state = mutableStateOf(false)) }

        val lyrics_state = remember(getSong().id) { SongLyricsLoader.getItemState(getSong(), player.context) }
        var show_furigana: Boolean by remember { mutableStateOf(Settings.KEY_LYRICS_DEFAULT_FURIGANA.get()) }

        var submenu: Submenu? by remember { mutableStateOf(null) }
        var lyrics_sync_line_data: Pair<Int, List<AnnotatedReadingTerm>>? by remember { mutableStateOf(null) }

        var special_mode: SpecialMode? by remember { mutableStateOf(null) }

        BackHandler(submenu != null || special_mode != null) {
            if (submenu != null) {
                submenu = null
            }
            else {
                special_mode = null
            }
        }

        LaunchedEffect(lyrics_state) {
            SongLyricsLoader.loadBySong(getSong(), player.context)
        }

        LaunchedEffect(lyrics_state.loading) {
            if (!lyrics_state.loading && lyrics_state.lyrics == null && submenu == null) {
                submenu = Submenu.SEARCH
            }
        }

        LaunchedEffect(lyrics_state.lyrics) {
            submenu = null
            lyrics_sync_line_data = null
            special_mode = null
        }

        Box(contentAlignment = Alignment.Center) {
            var show_lyrics_info by remember { mutableStateOf(false) }
            if (show_lyrics_info) {
                PlatformAlertDialog(
                    { show_lyrics_info = false },
                    {
                        Button({ show_lyrics_info = false }) {
                            Text(getString("action_close"))
                        }
                    }
                ) {
                    Crossfade(lyrics_state.lyrics) { lyrics ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            if (lyrics == null) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                            else {
                                @Composable
                                fun Item(title: String, text: String) {
                                    val faint_colour = LocalContentColor.current.setAlpha(0.75f)
                                    Column(Modifier.fillMaxWidth().border(1.dp, faint_colour, RoundedCornerShape(16.dp)).padding(10.dp)) {
                                        Text(title, style = MaterialTheme.typography.bodySmall, color = faint_colour)
                                        Spacer(Modifier.height(5.dp))

                                        SelectionContainer {
                                            Text(text, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }

                                Item(
                                    getString("lyrics_info_key_source"),
                                    remember(lyrics.source_idx) {
                                        LyricsSource.fromIdx(lyrics.source_idx).getReadable()
                                    }
                                )
                                Item(getString("lyrics_info_key_id"), lyrics.id)
                                Item(getString("lyrics_info_key_sync_type"), lyrics.sync_type.getReadable())
                            }
                        }
                    }
                }
            }

            // Pill menu
            AnimatedVisibility(special_mode == null && submenu != Submenu.SEARCH, Modifier.zIndex(10f), enter = fadeIn(), exit = fadeOut()) {
                pill_menu.PillMenu(
                    if (submenu != null) 1 else if (lyrics_state.lyrics?.synced == true) 5 else 4,
                    { index, _ ->
                        when (index) {
                            0 -> ActionButton(Icons.Filled.Close) {
                                if (submenu != null) {
                                    openMenu(null)
                                }
                                else {
                                    submenu = null
                                }
                            }
                            1 -> ActionButton(Icons.Outlined.Info) { show_lyrics_info = !show_lyrics_info }
                            2 -> Box(
                                Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ふ", color = content_colour, fontSize = 20.sp, modifier = Modifier
                                    .offset(y = (-5).dp)
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        rememberRipple(bounded = false, radius = 20.dp),
                                        onClick = {
                                            show_furigana = !show_furigana
                                            is_open = !is_open
                                        })
                                )
                            }
                            3 -> ActionButton(Icons.Filled.Search) { submenu = Submenu.SEARCH }
                            4 -> ActionButton(Icons.Default.HourglassEmpty) { special_mode = SpecialMode.ADJUST_SYNC }
                        }
                    },
                    _background_colour = { theme.accent },
                    vertical = true
                )
            }

            Crossfade(Triple(submenu, getSong(), lyrics_state.lyrics ?: lyrics_state.loading), Modifier.fillMaxSize()) { state ->
                val (current_submenu, song, l) = state
                val lyrics: SongLyrics? = if (l is SongLyrics) l else null
                val loading: Boolean = l == true

                if (current_submenu == Submenu.SEARCH) {
                    LyricsSearchMenu(song, Modifier.fillMaxSize()) { changed ->
                        submenu = null
                        if (changed) {
                            coroutine_scope.launchSingle {
                                val result = SongLyricsLoader.loadBySong(getSong(), player.context)
                                result.onFailure { error ->
                                    // TODO
                                    player.context.sendToast(error.toString())
                                }
                            }
                        }
                        else if (!loading && lyrics == null) {
                            openMenu(null)
                        }
                    }
                }
                else if (current_submenu == Submenu.SYNC) {
                    if (lyrics is SongLyrics) {
                        lyrics_sync_line_data?.also { line_data ->
                            LyricsSyncMenu(
                                song, 
                                lyrics, 
                                line_data.first, 
                                line_data.second, 
                                Modifier.fillMaxSize()
                            ) {
                                submenu = null
                            }
                        }
                    }
                    else {
                        submenu = null
                    }
                }
                else if (lyrics != null) {
                    Box(Modifier.fillMaxSize()) {
                        val lyrics_follow_enabled: Boolean by Settings.KEY_LYRICS_FOLLOW_ENABLED.rememberMutableState()

                        CoreLyricsDisplay(
                            lyrics,
                            song,
                            scroll_state,
                            getExpansion,
                            show_furigana,
                            Modifier.fillMaxSize(),
                            enable_autoscroll = lyrics_follow_enabled && special_mode != SpecialMode.SELECT_SYNC_LINE
                        ) {
                            if (special_mode == SpecialMode.SELECT_SYNC_LINE) { line_data ->
                                submenu = Submenu.SYNC
                                lyrics_sync_line_data = line_data
                                special_mode = null
                            }
                            else null
                        }

                        AnimatedVisibility(
                            special_mode != null,
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            enter = slideInVertically { it * 2 },
                            exit = slideOutVertically { it * 2 }
                        ) {
                            SpecialModeMenu(special_mode, song) { special_mode = it }
                        }
                    }
                }
                else if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SubtleLoadingIndicator(message = getString("lyrics_loading"))
                    }
                }
            }
        }
    }
}

@Composable
fun getLyricsTextStyle(font_size: TextUnit): TextStyle =
    LocalTextStyle.current.copy(
        fontSize = font_size,
        lineHeight = (font_size.value * 1.5).sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start
    )
