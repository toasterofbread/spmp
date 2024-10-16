package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import SpMp
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.lyrics.SongLyrics.Term
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.STATIC_LYRICS_SYNC_OFFSET
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.shared.R
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration
import dev.toastbits.composekit.platform.composable.theme.LocalApplicationTheme
import dev.toastbits.composekit.settings.ui.ThemeValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class LyricsLineHorizontalWidget: SpMpWidget<LyricsWidgetConfiguration>(LyricsWidgetConfiguration::class) {
    private val coroutine_scope = CoroutineScope(Job())

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        coroutine_scope.cancel()
    }

    @Composable
    override fun Content(modifier: GlanceModifier) {
        val theme: ThemeValues = LocalApplicationTheme.current
        val song: Song? = SpMp._player_state?.status?.m_song
        val lyrics_state: SongLyricsLoader.ItemState? = song?.let { SongLyricsLoader.rememberItemState(it, context) }

        Column(
            GlanceModifier
                .fillMaxSize()
                .background(
                    theme.background
                        .copy(alpha = base_configuration.background_opacity)
                )
        ) {
            val lyrics = lyrics_state?.lyrics
            if (lyrics == null) {
                Text("No lyrics", style = text_style)
            }
            else {
                SpMp.test

                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val lyrics_sync_offset: Long = song.getImmediateLyricsSyncOffset(context.database, false)
                    val position: Long? = SpMp._player_state?.status?.getPositionMs()?.plus(lyrics_sync_offset)

                    val currentLine: List<Term>? = lyrics.getCurrentLine(position ?: 0L, true)

                    if (currentLine != null) {
                        LyricsLine(
                            currentLine,
                            text_style,
                        )
                    }
                }
            }
        }
    }
}

//@Composable
//private fun <T> Crossfade(
//    value: T,
//    modifier: GlanceModifier = GlanceModifier,
//    content: @Composable (T) -> Unit
//) {
//    val ANIM_DURATION: Duration = 500.milliseconds
//
//    var A: T by remember { mutableStateOf(value) }
//    var B: T by remember { mutableStateOf(value) }
//
//    LaunchedEffect(value) {
//        A = value
//
//        delay(ANIM_DURATION)
//        B = value
//    }
//
//    Flipper(
//        ANIM_DURATION.inWholeMilliseconds.toInt(),
//        modifier
//    ) {
//        content(A)
//        content(B)
//    }
//}
//
//@Composable
//private fun Flipper(
//    interval_millis: Int,
//    modifier: GlanceModifier = GlanceModifier,
//    content: @Composable () -> Unit
//) {
//    val view_id: Int = R.id.view_flipper
//
//    val context: Context = LocalContext.current
//    val flipper: RemoteViews = RemoteViews(context.packageName, R.layout.view_flipper)
//
//    flipper.setInt(view_id, "setFlipInterval", interval_millis)
//
//    AndroidRemoteViews(
//        remoteViews = flipper,
//        containerViewId = view_id,
//        modifier = modifier,
//        content = content
//    )
//}

@Composable
private fun LyricsLine(line: List<Term>, text_style: TextStyle) {
    Row {
        for (term in line) {
            for (subterm in term.subterms) {
                Text(subterm.text, style = text_style)
            }
        }
    }
}

private fun SongLyrics.getCurrentLine(time: Long, linger: Boolean): List<Term>? {
    var last_before: List<Term>? = null

    for (line in lines) {
        val range: LongRange = line.firstOrNull()?.line_range ?: continue
        if (range.contains(time)) {
            return line
        }

        if (linger && range.last < time) {
            last_before = line
        }
    }
    return last_before
}

@Composable
fun Song.getImmediateLyricsSyncOffset(database: Database, is_topbar: Boolean): Long {
    val player: PlayerState = LocalPlayerState.current

    val internal_offset: Long? by LyricsSyncOffset.observe(database)
    val settings_delay: Float by player.settings.lyrics.SYNC_DELAY.observe()
    val settings_delay_topbar: Float by player.settings.lyrics.SYNC_DELAY_TOPBAR.observe()
    val settings_delay_bt: Float by player.settings.lyrics.SYNC_DELAY_BLUETOOTH.observe()

    var delay: Float = settings_delay

    if (is_topbar) {
        delay += settings_delay_topbar
    }

    // Ensure recomposition on value change, as device change is not observed directly
    @Suppress("UNUSED_EXPRESSION")
    settings_delay_bt

    if (player.controller?.isPlayingOverLatentDevice() == true) {
        delay += settings_delay_bt
    }

    return (internal_offset ?: 0) - (delay * 1000L).toLong() + STATIC_LYRICS_SYNC_OFFSET
}
