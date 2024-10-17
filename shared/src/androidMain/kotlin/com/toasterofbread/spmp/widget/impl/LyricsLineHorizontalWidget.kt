package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import SpMp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.lyrics.SongLyrics.Term
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.util.LyricsLineState
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration

internal class LyricsLineHorizontalWidget: SpMpWidget<LyricsWidgetConfiguration>(LyricsWidgetConfiguration::class) {
    private var lyrics_state: SongLyricsLoader.ItemState? by mutableStateOf(null)

    @Composable
    override fun hasContent(): Boolean {
        val song: Song? = LocalPlayerState.current.status.m_song
        lyrics_state = song?.let { SongLyricsLoader.rememberItemState(it, context) }
        return lyrics_state?.lyrics?.sync_type?.let { it != SongLyrics.SyncType.NONE } == true
    }

    @Composable
    override fun Content(modifier: GlanceModifier) {
        val player: PlayerState = LocalPlayerState.current

        Box(modifier) {
            val current_state: SongLyricsLoader.ItemState? = lyrics_state
            val lyrics: SongLyrics? = current_state?.lyrics

            if (lyrics == null) {
                Text("No lyrics", style = text_style.copy(fontSize = 15.sp * type_configuration.font_size))
            }
            else {
                SpMp.test

                val lyrics_sync_offset: Long by current_state.song.getLyricsSyncOffset(context.database, true)

                val line_state: LyricsLineState? =
                    LyricsLineState.rememberCurrentLineState(lyrics, true, update_interval = null) {
                        player.status.getPositionMs() + lyrics_sync_offset
                    }

                val show_readings: Boolean =
                    when (type_configuration.furigana_mode) {
                        LyricsWidgetConfiguration.FuriganaMode.APP_DEFAULT ->
                            player.settings.lyrics.DEFAULT_FURIGANA.observe().value
                        LyricsWidgetConfiguration.FuriganaMode.SHOW ->
                            true
                        LyricsWidgetConfiguration.FuriganaMode.HIDE ->
                            false
                    }

                line_state?.LyricsDisplay { show, line ->
                    if (show && line != null) {
                        LyricsLine(
                            line,
                            text_style.copy(fontSize = 15.sp * type_configuration.font_size),
                            text_style.copy(fontSize = 8.sp * type_configuration.font_size),
                            show_readings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsLine(
    line: List<Term>,
    text_style: TextStyle,
    reading_text_style: TextStyle,
    show_readings: Boolean
) {
    Row(verticalAlignment = Alignment.Bottom) {
        val term_chunks: List<List<Term.Text>> =
            remember(line) { line.flatMap { it.subterms }.chunked(10) }

        for (chunk in term_chunks) {
            Row(verticalAlignment = Alignment.Bottom) {
                for (term in chunk) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (show_readings) {
                            Text(
                                term.reading.orEmpty(),
                                style = reading_text_style
                            )
                        }
                        Text(term.text, style = text_style)
                    }
                }
            }
        }
    }
}
