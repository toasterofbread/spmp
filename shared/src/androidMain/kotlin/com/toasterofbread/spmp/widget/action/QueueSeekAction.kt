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
            controller.seekToItem(index)
        }
    }

    companion object {
        val keyIndex: ActionParameters.Key<Int> = ActionParameters.Key("index")

        operator fun invoke(index: Int): Action =
            actionRunCallback<QueueSeekAction>(
                actionParametersOf(keyIndex to index)
            )
    }
}