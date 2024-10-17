package com.toasterofbread.spmp.widget.impl

import LocalPlayerState
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import com.toasterofbread.spmp.widget.configuration.SongQueueWidgetConfiguration
import dev.toastbits.composekit.utils.common.getValue
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class SongQueueWidget: SpMpWidget<SongQueueWidgetClickAction, SongQueueWidgetConfiguration>() {
    override fun executeTypeAction(action: SongQueueWidgetClickAction) =
        when (action) {
            else -> throw IllegalStateException(action.toString())
        }

    @Composable
    override fun hasContent(): Boolean =
        LocalPlayerState.current.status.m_song != null

    @Composable
    override fun Content(modifier: GlanceModifier) {
        // Force recomposition
        update

        val player: PlayerState = LocalPlayerState.current
        val song: Song? = player.status.m_song

        if (song != null) {
            Column(modifier) {
//                WidgetText("Now playing", font_size = 10.sp, alpha = 0.5f)

//                GlanceSongPreview(song)

                val playing_index: Int = player.status.m_index
                val following_songs: MutableList<Song> = mutableListOf()

                for (offset in 1 .. type_configuration.next_songs_to_show) {
                    val following: Song = player.controller?.getSong(playing_index + offset) ?: break
                    following_songs.add(following)
                }

                if (following_songs.isNotEmpty()) {
                    WidgetText(
                        "Up next",
                        GlanceModifier.padding(vertical = 5.dp),
                        font_size = 13.sp,
                        alpha = 0.5f
                    )

                    LazyColumn {
                        itemsIndexed(following_songs) { index, following ->
                            GlanceSongPreview(
                                following,
                                GlanceModifier
//                                    .fillMaxWidth()
                                    .clickable(
                                        QueueSeekAction(playing_index + 1 + index)
                                    )
//                                    .thenIf(index + 1 != following_songs.size) {
//                                        padding(bottom = 3.dp)
//                                    }
                            )
                        }
                    }
                }
            }
        }
        else {
            WidgetText("No songs", modifier)
        }
    }

    companion object {
        var update: Int by mutableIntStateOf(0)
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

        song.Thumbnail(
            ThumbnailProvider.Quality.LOW,
            contentOverride = {
                Image(
                    ImageProvider(it.asAndroidBitmap()),
                    title,
                    GlanceModifier
                        .size(50.dp)
                        .padding(end = 10.dp)
                )
            }
        )

        Column {
            title?.also { text ->
                WidgetText(
                    text,
                    font_size = 17.sp
                )
            }

            artist_title?.also { text ->
                WidgetText(
                    text,
                    font_size = 12.sp,
                    alpha = 0.75f
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
