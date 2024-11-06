package com.toasterofbread.spmp.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.updateAll
import com.toasterofbread.spmp.widget.SpMpWidgetUpdater.getUpdateValue
import com.toasterofbread.spmp.widget.impl.LyricsLineHorizontalWidget
import com.toasterofbread.spmp.widget.impl.SongQueueWidget
import com.toasterofbread.spmp.widget.impl.SplitImageControlsWidget
import kotlin.reflect.KClass

val SpMpWidgetType.widgetClass: KClass<out SpMpWidget<*, *>>
    get() = when (this) {
        SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> LyricsLineHorizontalWidget::class
        SpMpWidgetType.SONG_QUEUE -> SongQueueWidget::class
        SpMpWidgetType.SPLIT_IMAGE_CONTROLS -> SplitImageControlsWidget::class
    }

object SpMpWidgetUpdater {
    private val widget_instances: Map<SpMpWidgetType, SpMpWidget<*, *>> by lazy {
        SpMpWidgetType.entries.associateWith { type ->
            type.widgetClass.java.getDeclaredConstructor().newInstance()
        }
    }

    private var update_value_LYRICS_LINE_HORIZONTAL: Int by mutableIntStateOf(0)
    private var update_value_SONG_QUEUE: Int by mutableStateOf(0)
    private var update_value_SPLIT_IMAGE_CONTROLS: Int by mutableStateOf(0)

    private fun SpMpWidgetType.incrementUpdateValue() {
        when (this) {
            SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> update_value_LYRICS_LINE_HORIZONTAL++
            SpMpWidgetType.SONG_QUEUE -> update_value_SONG_QUEUE++
            SpMpWidgetType.SPLIT_IMAGE_CONTROLS -> update_value_SPLIT_IMAGE_CONTROLS++
        }
    }

    fun SpMpWidgetType.getUpdateValue(): Any =
        when (this) {
            SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> update_value_LYRICS_LINE_HORIZONTAL
            SpMpWidgetType.SONG_QUEUE -> update_value_SONG_QUEUE
            SpMpWidgetType.SPLIT_IMAGE_CONTROLS -> update_value_SPLIT_IMAGE_CONTROLS
        }

    suspend fun SpMpWidgetType.updateAll(context: Context) {
        incrementUpdateValue()
        widget_instances[this]!!.updateAll(context)
    }

    suspend fun SpMpWidgetType.update(context: Context, id: GlanceId) {
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
