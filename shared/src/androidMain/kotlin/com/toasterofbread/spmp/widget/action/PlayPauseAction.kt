package com.toasterofbread.spmp.widget.action

import SpMp
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.toasterofbread.spmp.platform.playerservice.PlayerService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayPauseAction: ActionCallback {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val mode: Int = parameters[keyPlay] ?: return
        val controller: PlayerService = SpMp._player_state?.controller ?: return

        GlobalScope.launch(Dispatchers.Main) {
            if (mode == 0) {
                controller.pause()
            }
            else if (mode > 0) {
                controller.play()
            }
            else {
                controller.playPause()
            }
        }
    }

    companion object {
        val keyPlay: ActionParameters.Key<Int> = ActionParameters.Key("play")

        operator fun invoke(play: Boolean?): Action =
            actionRunCallback<PlayPauseAction>(
                actionParametersOf(
                    keyPlay to when (play) {
                        true -> 1
                        false -> 0
                        null -> -1
                    }
                )
            )
    }
}