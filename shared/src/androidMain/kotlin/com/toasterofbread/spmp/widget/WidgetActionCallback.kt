package com.toasterofbread.spmp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import kotlinx.serialization.encodeToString

internal class WidgetActionCallback: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val serialisedAction: String = parameters[keyAction] ?: return
        val action: WidgetClickAction<TypeWidgetClickAction> = SpMpWidgetConfiguration.json.decodeFromString(serialisedAction)
        SpMpWidget.runActionOnWidget(action, glanceId)
    }

    companion object {
        val keyAction: ActionParameters.Key<String> = ActionParameters.Key("action")

        operator fun invoke(action: WidgetClickAction<*>): Action =
            actionRunCallback<WidgetActionCallback>(
                actionParametersOf(
                    keyAction to SpMpWidgetConfiguration.json.encodeToString(action)
                )
            )
    }
}
