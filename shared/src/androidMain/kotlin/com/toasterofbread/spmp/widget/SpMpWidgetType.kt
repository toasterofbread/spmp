package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget
import com.toasterofbread.spmp.widget.configuration.SpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.LyricsSpMpWidgetConfiguration
import kotlin.reflect.KClass

enum class SpMpWidgetType(
    val widgetClass: KClass<out GlanceAppWidget>,
    val defaultConfiguration: SpMpWidgetConfiguration
) {
    LYRICS_LINE_HORIZONTAL(LyricsLineHorizontalWidget::class, LyricsSpMpWidgetConfiguration());
}
