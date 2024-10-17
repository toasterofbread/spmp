package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget
import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import com.toasterofbread.spmp.widget.impl.LyricsLineHorizontalWidget
import com.toasterofbread.spmp.widget.impl.LyricsWidget
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class SpMpWidgetType(
    val widgetClass: KClass<out GlanceAppWidget>,
    val defaultConfiguration: TypeWidgetConfiguration<*>,
    val clickActionClass: KClass<out TypeWidgetClickAction>,
    val updateType: WidgetUpdateType?
) {
    LYRICS_LINE_HORIZONTAL(LyricsLineHorizontalWidget::class, LyricsWidgetConfiguration(), LyricsWidgetClickAction::class, WidgetUpdateType.DuringPlayback(500.milliseconds)) {
        override fun incrementUpdateVariable() {
            LyricsWidget.update++
        }
    };

    abstract fun incrementUpdateVariable()
}

sealed interface WidgetUpdateType {
    data class DuringPlayback(val period: Duration): WidgetUpdateType
    data object OnSongTransition: WidgetUpdateType
}
