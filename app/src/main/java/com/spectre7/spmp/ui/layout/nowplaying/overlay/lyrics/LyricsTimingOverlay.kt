package com.spectre7.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.spectre7.spmp.PlayerServiceHost
import com.spectre7.spmp.model.Song
import com.spectre7.spmp.ui.layout.PlayerViewContext
import com.spectre7.spmp.ui.layout.nowplaying.POSITION_UPDATE_INTERVAL_MS
import com.spectre7.spmp.ui.layout.nowplaying.getNPBackground
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun LyricsTimingOverlay(
    playerProvider: () -> PlayerViewContext,
    lyrics: Song.Lyrics,
    full_line: Boolean,
    seek_state: Any,
    scroll_state: LazyListState,
    scrollTo: suspend (Float) -> Unit
) {

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
    //            val offset = scroll_state.firstVisibleItemScrollOffset
            if (highlight_instantly) {
                drawRoundRect(
                    getNPBackground(playerProvider),
                    Offset(highlight_position_x, highlight_position_y),
                    Size(highlight_width, highlight_height),
                    CornerRadius(25f, 25f)
                )
            } else {
                drawRoundRect(
                    getNPBackground(playerProvider),
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
//        scrollTo(highlight_position_y)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(POSITION_UPDATE_INTERVAL_MS)

            if (lyrics.sync_type == Song.Lyrics.SyncType.NONE) {
                return@LaunchedEffect
            }

            val terms = mutableListOf<Song.Lyrics.Term>()
            var offset: Offset? = null

            for (item in scroll_state.layoutInfo.visibleItemsInfo) {
                if (item.key !is Int) {
                    continue
                }
                
                val line = lyrics.lines[item.key as Int]
                for (term in line) {
                    if (term.range.contains(PlayerServiceHost.status.position_seconds)) {
                        if (full_line) {
                            for (_term in line) {
                                terms.add(_term)
                            }
                            break
                        } 
                        else {
                            terms.add(term)
                        }
                    }
                }

                if (terms.isNotEmpty()) {
                    offset = Offset(0f, item.offset.toFloat() + 75f)
                    break
                }
            }

            if (terms.isEmpty()) {
                return@LaunchedEffect
            }

            var target_x: Float = Float.NaN
            var target_y: Float = Float.NaN
            var target_br_x: Float = Float.NaN
            var target_br_y: Float = Float.NaN

            for (term in terms) {
                var rect = (term.data as Rect)
                rect = rect.copy(left = rect.left, right = rect.right)
                println("${rect.left} | ${rect.right} | ${term.subterms.first().text}")

                if (target_x.isNaN() || rect.left < target_x) {
                    target_x = offset!!.x + rect.left
                }
                if (target_y.isNaN() || rect.top < target_y) {
                    target_y = offset!!.y// + rect.top
                }

                if (target_br_x.isNaN() || rect.right > target_br_x) {
                    target_br_x = offset!!.x + rect.right
                }
                if (target_br_y.isNaN() || rect.bottom > target_br_y) {
                    target_br_y = offset!!.y// + rect.bottom
                }
            }

            if (
                highlight_position_x != target_x || 
                highlight_position_y != target_y || 
                highlight_width != abs(target_br_x - target_x) || 
                highlight_height != abs(target_br_y - target_y)
            ) {
                highlight_position_x = target_x
                highlight_position_y = target_y
                highlight_width = abs(target_br_x - target_x)
                highlight_height = abs(target_br_y - target_y)

                highlight_instantly = highlight_unset
                highlight_unset = false
            }

            show_highlight = true
        }
    }
}