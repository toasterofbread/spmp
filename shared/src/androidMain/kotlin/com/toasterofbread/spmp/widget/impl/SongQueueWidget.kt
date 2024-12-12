package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.component.GlanceLazyColumn
import com.toasterofbread.spmp.widget.configuration.type.SongQueueWidgetConfig
import com.toasterofbread.spmp.widget.action.QueueSeekAction
import com.toasterofbread.spmp.widget.component.spmp.GlanceSongPreview
import dev.toastbits.composekit.util.thenIf
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_empty_status_nothing_playing
import spmp.shared.generated.resources.widget_queue_heading_now_playing
import spmp.shared.generated.resources.widget_queue_heading_up_next

internal class SongQueueWidget: SpMpWidget<SongQueueWidgetClickAction, SongQueueWidgetConfig>(false) {
    override fun executeTypeAction(action: SongQueueWidgetClickAction) =
        when (action) {
            else -> throw IllegalStateException(action.toString())
        }

    @Composable
    private fun Heading(text: String, modifier: GlanceModifier = GlanceModifier) {
        WidgetText(
            text,
            modifier.padding(bottom = 5.dp),
            font_size = 15.sp,
            alpha = 0.5f
        )
    }

    @Composable
    override fun Content(
        song: Song?,
        song_image: Bitmap?,
        modifier: GlanceModifier,
        content_padding: PaddingValues
    ) {
        val player: PlayerState = LocalPlayerState.current

        if (song != null) {
            GlanceLazyColumn(
                content_padding,
                modifier.fillMaxWidth()
            ) {
                if (type_configuration.show_current_song) {
                    item {
                        Heading(stringResource(Res.string.widget_queue_heading_now_playing))
                    }

                    item {
                        GlanceSongPreview(song, GlanceModifier.fillMaxWidth())
                    }
                }

                val playing_index: Int = player.status.m_index
                val following_songs: MutableList<Song> = mutableListOf()

                for (offset in 1 .. type_configuration.next_songs_to_show.let { if (it < 0) player.status.m_song_count else it }) {
                    val following: Song = player.controller?.getSong(playing_index + offset) ?: break
                    following_songs.add(following)
                }

                if (following_songs.isNotEmpty()) {
                    item {
                        Heading(
                            stringResource(Res.string.widget_queue_heading_up_next),
                            GlanceModifier.padding(top = 15.dp)
                        )
                    }

                    itemsIndexed(following_songs) { index, following ->
                        GlanceSongPreview(
                            following,
                            GlanceModifier
                                .fillMaxWidth()
                                .clickable(
                                    QueueSeekAction(playing_index + 1 + index)
                                )
                                .thenIf(index + 1 != following_songs.size) {
                                    padding(bottom = 3.dp)
                                }
                        )
                    }
                }
            }
        }
        else {
            WidgetText(stringResource(Res.string.widget_empty_status_nothing_playing), modifier)
        }
    }
}
