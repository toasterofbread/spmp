package com.toasterofbread.spmp.widget

import LocalPlayerState
import ProgramArguments
import SpMp
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.lyrics.SongLyrics.Term
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.STATIC_LYRICS_SYNC_OFFSET
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.youtubeapi.lyrics.LyricsSource
import dev.toastbits.composekit.utils.common.getValue
import kotlinx.coroutines.CoroutineScope

class SpMpMainWidget: GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val coroutine_scope: CoroutineScope = rememberCoroutineScope()
            val app_context: AppContext = remember { AppContext(context, coroutine_scope) }
            val state: PlayerState =
                remember { PlayerState(app_context, ProgramArguments(), coroutine_scope) }

            CompositionLocalProvider(LocalPlayerState provides state) {
                Content(app_context)
            }
        }
    }

    @Composable
    fun Content(app_context: AppContext) {
        val song: Song? = SpMp._player_state?.status?.m_song
        val lyrics: SongLyricsLoader.ItemState? = song?.let { SongLyricsLoader.rememberItemState(it, app_context) }

        Column(GlanceModifier.fillMaxSize().background(Color.Black)) {
            val text_style: TextStyle = TextStyle(color = ColorProvider(Color.White))

            val l = lyrics?.lyrics
            if (l == null) {
                Text("No lyrics", style = text_style)
            }
            else {
                SpMp.test

                Column {
                    val lyrics_sync_offset: Long = song.getImmediateLyricsSyncOffset(app_context.database, false)
                    val position: Long? = SpMp._player_state?.status?.getPositionMs()?.plus(lyrics_sync_offset)
                    println("SYNC OFFSET $lyrics_sync_offset")

                    Text(position.toString(), style = text_style)
                    Text(l.reference.toString(), style = text_style)

                    val currentLine: List<Term>? = l.getCurrentLine(position ?: 0L, true)
                    if (currentLine != null) {
                        LyricsLine(currentLine, text_style)
                    }
                }
            }
        }
    }
}

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
