package com.toasterofbread.spmp.widget

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.updateAll
import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.SongQueueWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import com.toasterofbread.spmp.widget.impl.LyricsLineHorizontalWidget
import com.toasterofbread.spmp.widget.impl.SongQueueWidget
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class SpMpWidgetType(
    val widgetClass: KClass<out SpMpWidget<*, *>>,
    val defaultConfiguration: TypeWidgetConfiguration<*>,
    val clickActionClass: KClass<out TypeWidgetClickAction>,
    val updateTypes: List<WidgetUpdateType>
) {
    LYRICS_LINE_HORIZONTAL(
        widgetClass = LyricsLineHorizontalWidget::class,
        defaultConfiguration = LyricsWidgetConfiguration(),
        clickActionClass = LyricsWidgetClickAction::class,
        updateTypes = listOf(WidgetUpdateType.DuringPlayback(500.milliseconds))
    ),
    SONG_QUEUE(
        widgetClass = SongQueueWidget::class,
        defaultConfiguration = SongQueueWidgetConfiguration(),
        clickActionClass = SongQueueWidgetClickAction::class,
        updateTypes = listOf(WidgetUpdateType.OnSongTransition, WidgetUpdateType.OnQueueChange)
    );

    private fun incrementUpdateValue() {
        update_values[this] = update_values[this]!! + 1
    }

    suspend fun updateAll(context: Context) {
        println("Updating all widgets of type $this")
        incrementUpdateValue()
        widget_instances[this]!!.updateAll(context)
    }

    suspend fun update(context: Context, id: GlanceId) {
        println("Updating widget $id of type $this")
        incrementUpdateValue()
        widget_instances[this]!!.update(context, id)
    }

    fun getUpdateValue(): Any =
        update_values[this]!!

    companion object {
        private val widget_instances: Map<SpMpWidgetType, SpMpWidget<*, *>> by lazy {
            entries.associateWith { type ->
                type.widgetClass.java.getDeclaredConstructor().newInstance()
            }
        }

        private val update_values: MutableMap<SpMpWidgetType, Int> =
            mutableStateMapOf<SpMpWidgetType, Int>().apply {
                for (type in SpMpWidgetType.entries) {
                    put(type, 0)
                }
            }
    }
}

sealed interface WidgetUpdateType {
    data class DuringPlayback(val period: Duration): WidgetUpdateType
    data object OnSongTransition: WidgetUpdateType
    data object OnQueueChange: WidgetUpdateType
}
