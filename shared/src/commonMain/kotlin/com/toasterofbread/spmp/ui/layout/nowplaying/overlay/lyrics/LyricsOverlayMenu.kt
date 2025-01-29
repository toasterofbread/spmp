@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import LocalPlayerState
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import dev.toastbits.composekit.components.platform.composable.BackHandler
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.composekit.components.utils.composable.SubtleLoadingIndicator
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.getOrNotify
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.PillMenu
import com.toasterofbread.spmp.ui.layout.nowplaying.overlay.PlayerOverlayMenu
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.action_close
import spmp.shared.generated.resources.lyrics_info_key_source
import spmp.shared.generated.resources.lyrics_info_key_id
import spmp.shared.generated.resources.lyrics_info_key_sync_type
import spmp.shared.generated.resources.lyrics_info_key_local_file
import spmp.shared.generated.resources.lyrics_loading
import spmp.shared.generated.resources.lyrics_no_lyrics_set_for_song

private enum class Submenu {
    SEARCH, SYNC
}

class LyricsPlayerOverlayMenu: PlayerOverlayMenu() {
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        getSong: () -> Song?,
        getExpansion: () -> Float,
        openMenu: (PlayerOverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val song: Song = getSong() ?: return
        val player: PlayerState = LocalPlayerState.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()
        val scroll_state: LazyListState = rememberLazyListState()
        val pill_menu: PillMenu = remember { PillMenu(expand_state = mutableStateOf(false)) }

        val lyrics_state: SongLyricsLoader.ItemState = SongLyricsLoader.rememberItemState(song, player.context)
        var show_furigana: Boolean by player.settings.Lyrics.DEFAULT_FURIGANA.observe()

        var submenu: Submenu? by remember { mutableStateOf(null) }
        var lyrics_sync_line_index: Int? by remember { mutableStateOf(null) }

        var special_mode: SpecialMode? by remember { mutableStateOf(null) }

        BackHandler(submenu != null || special_mode != null, priority = 3) {
            if (submenu != null) {
                submenu = null
            }
            else {
                special_mode = null
            }
        }

        LaunchedEffect(lyrics_state) {
            getSong()?.also { song ->
                SongLyricsLoader.loadBySong(song, player.context)
            }
        }

        LaunchedEffect(lyrics_state.loading) {
            if (!lyrics_state.loading && lyrics_state.lyrics == null && submenu == null) {
                submenu = Submenu.SEARCH
            }
        }

        LaunchedEffect(lyrics_state.lyrics) {
            submenu = null
            lyrics_sync_line_index = null
            special_mode = null
        }

        Box(contentAlignment = Alignment.Center) {
            var show_lyrics_info by remember { mutableStateOf(false) }
            if (show_lyrics_info) {
                AlertDialog(
                    onDismissRequest = { show_lyrics_info = false },
                    confirmButton = {
                        Button({ show_lyrics_info = false }) {
                            Text(stringResource(Res.string.action_close))
                        }
                    },
                    text = {
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
                                        val faint_colour = LocalContentColor.current.copy(alpha = 0.75f)
                                        Column(Modifier.fillMaxWidth().border(1.dp, faint_colour, RoundedCornerShape(16.dp)).padding(10.dp)) {
                                            Text(title, style = MaterialTheme.typography.bodySmall, color = faint_colour)
                                            Spacer(Modifier.height(5.dp))

                                            SelectionContainer {
                                                Text(text, style = MaterialTheme.typography.titleMedium)
                                            }
                                        }
                                    }

                                    Item(
                                        stringResource(Res.string.lyrics_info_key_source),
                                        LyricsSource.fromIdx(lyrics.source_idx).getReadable()
                                    )
                                    Item(stringResource(Res.string.lyrics_info_key_id), lyrics.id)
                                    Item(stringResource(Res.string.lyrics_info_key_sync_type), lyrics.sync_type.getReadable())
                                    Item(stringResource(Res.string.lyrics_info_key_local_file), lyrics.reference.local_file?.absolute_path.toString())
                                }
                            }
                        }
                    }
                )
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
                                        LocalIndication.current,
                                        onClick = {
                                            show_furigana = !show_furigana
                                            is_open = !is_open
                                        }
                                    )
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

            Crossfade(
                Triple(
                    submenu,
                    song,
                    lyrics_state.lyrics ?: if (lyrics_state.loading) true else if (lyrics_state.is_none) null else false
                ),
                Modifier.fillMaxSize()
            ) { state ->
                val (current_submenu, song, l) = state
                val lyrics: SongLyrics? = if (l is SongLyrics) l else null
                val loading: Boolean? = if (l is SongLyrics) false else (l as Boolean?)

                if (current_submenu == Submenu.SEARCH) {
                    LyricsSearchMenu(song, Modifier.fillMaxSize()) { changed ->
                        submenu = null
                        if (changed) {
                            coroutine_scope.launchSingle {
                                val result: SongLyrics? =
                                    SongLyricsLoader.loadBySong(song, player.context)
                                        ?.getOrNotify(player.context, "LyricsOverlayMenu lyrics search")
                            }
                        }
                        else if (loading == false && lyrics == null) {
                            openMenu(null)
                        }
                    }
                }
                else if (current_submenu == Submenu.SYNC) {
                    if (lyrics is SongLyrics) {
                        lyrics_sync_line_index?.also { line_index ->
                            LyricsSyncMenu(
                                song,
                                lyrics,
                                line_index,
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
                        val lyrics_follow_enabled: Boolean by player.settings.Lyrics.FOLLOW_ENABLED.observe()

                        CoreLyricsDisplay(
                            lyrics,
                            song,
                            scroll_state,
                            getExpansion,
                            show_furigana,
                            Modifier.fillMaxSize(),
                            enable_autoscroll = lyrics_follow_enabled && special_mode != SpecialMode.SELECT_SYNC_LINE,
                            onLineAltClick =
                                if (special_mode == SpecialMode.SELECT_SYNC_LINE) {{ line_index ->
                                    submenu = Submenu.SYNC
                                    lyrics_sync_line_index = line_index
                                    special_mode = null
                                }}
                                else null
                        )

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
                else if (loading == true) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SubtleLoadingIndicator(message = stringResource(Res.string.lyrics_loading))
                    }
                }
                else if (loading == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.lyrics_no_lyrics_set_for_song), style = MaterialTheme.typography.bodyLarge)
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
        lineHeight = font_size,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start
    )
