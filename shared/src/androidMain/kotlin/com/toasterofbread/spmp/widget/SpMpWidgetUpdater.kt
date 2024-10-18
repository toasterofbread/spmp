package com.toasterofbread.spmp.widget

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.updateAll
import com.toasterofbread.spmp.widget.impl.LyricsLineHorizontalWidget
import com.toasterofbread.spmp.widget.impl.SongQueueWidget
import kotlin.reflect.KClass

val SpMpWidgetType.widgetClass: KClass<out SpMpWidget<*, *>>
    get() = when (this) {
        SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> LyricsLineHorizontalWidget::class
        SpMpWidgetType.SONG_QUEUE -> SongQueueWidget::class
    }

object SpMpWidgetUpdater {
    private val widget_instances: Map<SpMpWidgetType, SpMpWidget<*, *>> by lazy {
        SpMpWidgetType.entries.associateWith { type ->
            type.widgetClass.java.getDeclaredConstructor().newInstance()
        }
    }

    private val update_values: MutableMap<SpMpWidgetType, Int> =
        mutableStateMapOf<SpMpWidgetType, Int>().apply {
            for (type in SpMpWidgetType.entries) {
                put(type, 0)
            }
        }

    private fun SpMpWidgetType.incrementUpdateValue() {
        update_values[this] = update_values[this]!! + 1
    }

    fun SpMpWidgetType.getUpdateValue(): Any =
        update_values[this]!!

    suspend fun SpMpWidgetType.updateAll(context: Context) {
        println("Updating all widgets of type $this")
        incrementUpdateValue()
        widget_instances[this]!!.updateAll(context)
    }

    suspend fun SpMpWidgetType.update(context: Context, id: GlanceId) {
        println("Updating widget $id of type $this")
        incrementUpdateValue()
        widget_instances[this]!!.update(context, id)
    }
}

fun SpMpWidgetType.getUpdateValue(): Any = with (SpMpWidgetUpdater) {
    getUpdateValue()
}

suspend fun SpMpWidgetType.updateAll(context: Context) = with (SpMpWidgetUpdater) {
    updateAll(context)
}

suspend fun SpMpWidgetType.update(context: Context, id: GlanceId) = with (SpMpWidgetUpdater) {
    update(context, id)
}
