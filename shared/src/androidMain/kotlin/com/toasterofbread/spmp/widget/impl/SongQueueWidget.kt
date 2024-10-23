package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import SpMp
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.Thumbnail
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.component.LazyColumn
import com.toasterofbread.spmp.widget.configuration.SongQueueWidgetConfig
import dev.toastbits.composekit.utils.common.getValue
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class SongQueueWidget: SpMpWidget<SongQueueWidgetClickAction, SongQueueWidgetConfig>(true) {
    override fun executeTypeAction(action: SongQueueWidgetClickAction) =
        when (action) {
            else -> throw IllegalStateException(action.toString())
        }

    @Composable
    override fun hasContent(): Boolean =
        LocalPlayerState.current.status.m_song != null

    @Composable
    override fun Content(modifier: GlanceModifier, content_padding: PaddingValues) {
        val player: PlayerState = LocalPlayerState.current
        val song: Song? = player.status.m_song

        if (song != null) {
            LazyColumn(
                content_padding,
                modifier.fillMaxWidth()
            ) {
                if (type_configuration.show_current_song) {
                    item {
                        WidgetText("NOW PLAYING", font_size = 13.sp, alpha = 0.5f)
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
                        WidgetText(
                            "UP NEXT",
                            GlanceModifier.padding(vertical = 5.dp),
                            font_size = 13.sp,
                            alpha = 0.5f
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
            WidgetText("No songs", modifier)
        }
    }
}

@Composable
internal fun SpMpWidget<*, *>.GlanceSongPreview(
    song: Song,
    modifier: GlanceModifier = GlanceModifier
) {
    val player: PlayerState = LocalPlayerState.current

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title: String? by song.observeActiveTitle()
        val artist: Artist? = song.Artists.observe(player.database).value?.firstOrNull()
        val artist_title: String? by artist?.observeActiveTitle()

        val image_size: Dp = 50.dp
        val spacing: Dp = 10.dp

        val showing: Boolean =
            song.Thumbnail(
                ThumbnailProvider.Quality.LOW,
                contentOverride = {
                    Image(
                        ImageProvider(it.asAndroidBitmap().scale(100, 100, true)),
                        title,
                        GlanceModifier
                            .size(image_size)
                            .padding(end = spacing)
                    )
                }
            )

        Column {
            val text_width: Dp = LocalSize.current.width.thenIf(showing) { this - image_size - spacing }

            title?.also { text ->
                WidgetText(
                    text,
                    font_size = 17.sp,
                    max_width = text_width
                )
            }

            artist_title?.also { text ->
                WidgetText(
                    text,
                    font_size = 12.sp,
                    alpha = 0.75f,
                    max_width = text_width
                )
            }
        }
    }
}

class QueueSeekAction: ActionCallback {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val index: Int = parameters[keyIndex] ?: return
        val controller: PlayerService = SpMp._player_state?.controller ?: return

        GlobalScope.launch(Dispatchers.Main) {
            controller.seekToSong(index)
        }
    }

    companion object {
        val keyIndex: ActionParameters.Key<Int> = ActionParameters.Key("index")

        operator fun invoke(index: Int): Action =
            actionRunCallback<QueueSeekAction>(
                actionParametersOf(QueueSeekAction.keyIndex to index)
            )
    }
}
