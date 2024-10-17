package com.toasterofbread.spmp.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetUpdateListener(private val context: Context): Player.Listener {
    private val widgets: Map<SpMpWidgetType, GlanceAppWidget> = SpMpWidgetType.entries.associateWith { it.widgetClass.java.getDeclaredConstructor().newInstance() }

    private val song_transition_widget_update_coroutine_scope: CoroutineScope =
        CoroutineScope(Dispatchers.Default)
    private val during_playback_widget_update_coroutine_scope: CoroutineScope =
        CoroutineScope(Dispatchers.Default)

    fun release() {
        song_transition_widget_update_coroutine_scope.cancel()
        during_playback_widget_update_coroutine_scope.cancel()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)

        song_transition_widget_update_coroutine_scope.launch {
            for ((type, widget) in widgets) {
                if (type.updateTypes.contains(WidgetUpdateType.OnSongTransition)) {
                    type.incrementUpdateVariable()
                    widget.updateAll(context)
                }
            }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        song_transition_widget_update_coroutine_scope.launch {
            for ((type, widget) in widgets) {
                if (type.updateTypes.contains(WidgetUpdateType.OnQueueChange)) {
                    type.incrementUpdateVariable()
                    widget.updateAll(context)
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            during_playback_widget_update_coroutine_scope.coroutineContext.cancelChildren()
            return
        }

        for ((type, widget) in widgets) {
            for (update in type.updateTypes.filterIsInstance<WidgetUpdateType.DuringPlayback>()) {
                during_playback_widget_update_coroutine_scope.launch {
                    while (true) {
                        delay(update.period)
                        type.incrementUpdateVariable()
                        widget.updateAll(context)
                    }
                }
            }
        }
    }
}
