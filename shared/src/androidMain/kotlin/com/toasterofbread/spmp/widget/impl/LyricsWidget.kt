package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import com.toasterofbread.spmp.model.lyrics.SongLyrics
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.util.LyricsLineState
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.type.LyricsWidgetConfig
import com.toasterofbread.spmp.widget.modifier.padding
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_lyrics_status_no_lyrics

internal abstract class LyricsWidget: SpMpWidget<LyricsWidgetClickAction, LyricsWidgetConfig>(false) {
    private var current_song: Song? by mutableStateOf(null)
    private var lyrics_state: SongLyricsLoader.ItemState? by mutableStateOf(null)
    private var show_readings: Boolean by mutableStateOf(false)
    private var show_readings_override: Boolean? by mutableStateOf(null)
    private var hide_until_next_song: Boolean by mutableStateOf(false)

    override fun executeTypeAction(action: LyricsWidgetClickAction) =
        when (action) {
            LyricsWidgetClickAction.TOGGLE_FURIGANA -> show_readings_override = show_readings_override?.not() ?: !show_readings
            LyricsWidgetClickAction.HIDE_UNTIL_NEXT_SONG -> hide_until_next_song = !hide_until_next_song
        }

    @Composable
    override fun shouldHide(): Boolean {
        val song: Song? = LocalPlayerState.current.status.m_song
        if (hide_until_next_song && song?.id == current_song?.id) {
            return true
        }

        current_song = song
        hide_until_next_song = false
        return false
    }

    @Composable
    override fun hasContent(song: Song?): Boolean {
        lyrics_state = song?.let { SongLyricsLoader.rememberItemState(it, context) }
        return lyrics_state?.lyrics?.sync_type?.let { it != SongLyrics.SyncType.NONE } == true
    }

    @Composable
    final override fun Content(
        song: Song?,
        song_image: Bitmap?,
        modifier: GlanceModifier,
        content_padding: PaddingValues
    ) {
        Column(modifier.padding(content_padding)) {
            val current_state: SongLyricsLoader.ItemState? = lyrics_state
            val lyrics: SongLyrics? = current_state?.lyrics

            if (lyrics == null) {
                WidgetText(stringResource(Res.string.widget_lyrics_status_no_lyrics))
            }
            else {
                if (show_readings_override == null) {
                    show_readings =
                        when (type_configuration.furigana_mode) {
                            LyricsWidgetConfig.FuriganaMode.APP_DEFAULT ->
                                LocalPlayerState.current.settings.Lyrics.DEFAULT_FURIGANA.observe().value

                            LyricsWidgetConfig.FuriganaMode.SHOW ->
                                true

                            LyricsWidgetConfig.FuriganaMode.HIDE ->
                                false
                        }
                }

                val player: PlayerState = LocalPlayerState.current
                val lyrics_sync_offset: Long by current_state.song.getLyricsSyncOffset(context.database, true)

                val position_ms: Long = player.status.getPositionMs() + lyrics_sync_offset
                val line_state: LyricsLineState? =
                    LyricsLineState.rememberCurrentLineState(lyrics, true, update_interval = null) { position_ms }

                if (line_state != null) {
                    line_state.update(position_ms, true)

                    LyricsContent(line_state, GlanceModifier)
                }
            }
        }
    }

    @Composable
    protected abstract fun LyricsContent(lyrics: LyricsLineState, modifier: GlanceModifier)

    @Composable
    protected fun LyricsLine(
        line: List<SongLyrics.Term>,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val font_size: TextUnit = 15.sp
        val reading_font_size: TextUnit = 8.sp

        Row(modifier, verticalAlignment = Alignment.Bottom) {
            val term_chunks: List<List<SongLyrics.Term.Text>> =
                remember(line) { line.flatMap { it.subterms }.chunked(10) }

            for (chunk in term_chunks) {
                Row(verticalAlignment = Alignment.Bottom) {
                    for (term in chunk) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (show_readings_override ?: show_readings) {
                                WidgetText(
                                    term.reading.orEmpty(),
                                    font_size = reading_font_size
                                )
                            }
                            WidgetText(term.text, font_size = font_size)
                        }
                    }
                }
            }
        }
    }
}
