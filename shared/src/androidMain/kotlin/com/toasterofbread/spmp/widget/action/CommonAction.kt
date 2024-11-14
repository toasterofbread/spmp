package com.toasterofbread.spmp.widget.action

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.song.updateLiked
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.playerservice.seekToPreviousOrRepeat
import com.toasterofbread.spmp.util.getToggleTarget
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.NONE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_SPMP
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.OPEN_WIDGET_CONFIG
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.PLAY_PAUSE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.SEEK_NEXT
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.SEEK_PREVIOUS
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.TOGGLE_LIKE
import com.toasterofbread.spmp.widget.action.WidgetClickAction.CommonWidgetClickAction.TOGGLE_VISIBILITY
import dev.toastbits.ytmkt.model.external.SongLikedStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun WidgetClickAction.CommonWidgetClickAction.execute(
    id: GlanceId,
    context: AppContext,
    widget: SpMpWidget<*, *>
) = withContext(Dispatchers.Main) {
    when (this@execute) {
        NONE -> {}
        OPEN_SPMP -> {
            with (context.ctx) {
                startActivity(packageManager.getLaunchIntentForPackage(packageName))
            }
        }
        OPEN_WIDGET_CONFIG -> {
            val intent: Intent = Intent()
            intent.setComponent(ComponentName(context.ctx, "com.toasterofbread.spmp.widget.WidgetConfigurationActivity"))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, GlanceAppWidgetManager(context.ctx).getAppWidgetId(id))
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            context.ctx.startActivity(intent)
        }
        TOGGLE_VISIBILITY -> {
            widget.visible = !widget.visible
        }
        PLAY_PAUSE -> SpMp._player_state?.controller?.playPause()
        SEEK_NEXT -> SpMp._player_state?.controller?.seekToNext()
        SEEK_PREVIOUS -> SpMp._player_state?.controller?.seekToPreviousOrRepeat()
        TOGGLE_LIKE -> {

            val song: Song = SpMp._player_state?.status?.song ?: return@withContext
            val liked: SongLikedStatus? = song.Liked.get(context.database)
            song.updateLiked(liked.getToggleTarget(), context.ytapi.user_auth_state?.SetSongLiked, context)
        }
    }
}
