package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import net.zerotask.libraries.android.compose.furigana.TermInfo
import kotlin.math.abs

@Composable
fun LyricsTimingOverlay(lyrics: Song.Lyrics, text_positions: List<TermInfo>, full_line: Boolean, seek_state: Any, scrollTo: suspend (Float) -> Unit) {

    var show_highlight by remember { mutableStateOf(false) }
    var highlight_instantly by remember { mutableStateOf(false) }
    var highlight_unset by remember { mutableStateOf(true) }

    var highlight_position_x by remember { mutableStateOf(-1f) }
    var highlight_position_y by remember { mutableStateOf(-1f) }
    var highlight_width by remember { mutableStateOf(-1f) }
    var highlight_height by remember { mutableStateOf(-1f) }

    val highlight_position_state = animateOffsetAsState(
        targetValue = Offset(highlight_position_x, highlight_position_y)
    )
    val highlight_size_state = animateOffsetAsState(
        targetValue = Offset(highlight_width, highlight_height)
    )

    AnimatedVisibility(show_highlight && !highlight_unset) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (highlight_instantly) {
                drawRoundRect(
                    MainActivity.theme.getBackground(true),
                    Offset(highlight_position_x, highlight_position_y),
                    Size(highlight_width, highlight_height),
                    CornerRadius(25f, 25f)
                )
            } else {
                drawRoundRect(
                    MainActivity.theme.getBackground(true),
                    highlight_position_state.value,
                    Size(highlight_size_state.value.x, highlight_size_state.value.y),
                    CornerRadius(25f, 25f)
                )
            }
        }
    }

    LaunchedEffect(lyrics, seek_state) {
        show_highlight = false
        highlight_unset = true
    }

    LaunchedEffect(highlight_position_y) {
        scrollTo(highlight_position_y)
    }

    LaunchedEffect(PlayerServiceHost.status.m_position, full_line) {

        if (lyrics.sync_type == Song.Lyrics.SyncType.NONE) {
            return@LaunchedEffect
        }

        val offset = Offset(-100f, -170f)

        val terms = mutableListOf<Song.Lyrics.Subterm>()
        val pos = (PlayerServiceHost.status.duration * PlayerServiceHost.status.position)
        var finished = false

        for (line in lyrics.lyrics) {
            for (term in line) {
                if (pos >= term.start!! && pos < term.end!!) {
                    if (full_line) {
                        for (_term in line) {
                            for (subterm in _term.subterms) {
                                terms.add(subterm)
                            }
                        }
                        finished = true
                    } else {
                        for (subterm in term.subterms) {
                            terms.add(subterm)
                        }
                    }
                    break
                }
            }
            if (finished) {
                break
            }
        }

        var target_x: Float? = null
        var target_y: Float? = null
        var target_br_x: Float? = null
        var target_br_y: Float? = null

        for (term in terms) {
            val rect = text_positions[term.index].rect

            if (target_x == null || rect.left < target_x) {
                target_x = rect.left + offset.x
            }
            if (target_y == null || rect.top < target_y) {
                target_y = rect.top + offset.y
            }

            if (target_br_x == null || rect.right > target_br_x) {
                target_br_x = rect.right + offset.x
            }
            if (target_br_y == null || rect.bottom > target_br_y) {
                target_br_y = rect.bottom + offset.y
            }
        }

        if (target_x != null) {

            if (highlight_position_x != target_x || highlight_position_y != target_y || highlight_width != abs(
                    target_br_x!! - target_x
                ) || highlight_height != abs(target_br_y!! - target_y)
            ) {
                highlight_position_x = target_x
                highlight_position_y = target_y!!
                highlight_width = abs(target_br_x!! - target_x)
                highlight_height = abs(target_br_y!! - target_y)

                highlight_instantly = highlight_unset
                highlight_unset = false
            }

            show_highlight = true
        }
    }
}