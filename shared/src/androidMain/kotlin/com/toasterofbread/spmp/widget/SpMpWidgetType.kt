package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import kotlin.reflect.KClass

enum class SpMpWidgetType(
    val widgetClass: KClass<out GlanceAppWidget>,
    val defaultConfiguration: TypeWidgetConfiguration
) {
    LYRICS_LINE_HORIZONTAL(LyricsLineHorizontalWidget::class, LyricsWidgetConfiguration());
}
