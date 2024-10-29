package com.toasterofbread.spmp.widget

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.SongLikedStatusListener
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.toSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WidgetUpdateListener(private val ctx: Context, private val context: AppContext): Player.Listener {
    private val widget_update_coroutine_scope: CoroutineScope =
        CoroutineScope(Dispatchers.Default)
    private val during_playback_widget_update_coroutine_scope: CoroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val song_liked_listener: Query.Listener = Query.Listener {
        widget_update_coroutine_scope.launch {
            updateWidgetsWithType(WidgetUpdateType.OnCurrentSongLikedChanged)
        }
    }

    private var current_song: Song? = null

    init {
        snapshotFlow { context.ytapi.user_auth_state }
            .onEach {
                updateWidgetsWithType(WidgetUpdateType.OnAuthStateChanged)
            }
            .launchIn(widget_update_coroutine_scope)
    }

    fun release() {
        widget_update_coroutine_scope.cancel()
        during_playback_widget_update_coroutine_scope.cancel()
    }

    suspend fun updateAll() {
        for (type in SpMpWidgetType.entries) {
            type.updateAll(ctx)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        widget_update_coroutine_scope.launch {
            updateWidgetsWithType(WidgetUpdateType.OnSongTransition)
        }

        val song: Song? = mediaItem?.toSong()

        if (current_song == song) {
            return
        }

        current_song?.also { current ->
            context.database.songQueries.likedById(current.id)
                .removeListener(song_liked_listener)
        }

        current_song = song

        if (song != null) {
            context.database.songQueries.likedById(song.id).addListener(song_liked_listener)
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        widget_update_coroutine_scope.launch {
            updateWidgetsWithType(WidgetUpdateType.OnQueueChange)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            during_playback_widget_update_coroutine_scope.coroutineContext.cancelChildren()
            return
        }

        widget_update_coroutine_scope.launch {
            updateWidgetsWithType(WidgetUpdateType.OnPlayingChange)

            for (type in SpMpWidgetType.entries) {
                for (update in type.update_types.filterIsInstance<WidgetUpdateType.DuringPlayback>()) {
                    during_playback_widget_update_coroutine_scope.launch {
                        while (true) {
                            delay(update.period)
                            type.updateAll(ctx)
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidgetsWithType(update_type: WidgetUpdateType) {
        for (type in SpMpWidgetType.entries) {
            if (type.update_types.contains(update_type)) {
                type.updateAll(ctx)
            }
        }
    }
}
