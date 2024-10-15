package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget
import com.toasterofbread.spmp.widget.configuration.BaseSpMpWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.LyricsSpMpWidgetConfiguration
import kotlin.reflect.KClass

enum class SpMpWidgetType(
    val widgetClass: KClass<out GlanceAppWidget>,
    val defaultConfiguration: BaseSpMpWidgetConfiguration
) {
    LYRICS_LINE_HORIZONTAL(LyricsLineHorizontalWidget::class, LyricsSpMpWidgetConfiguration());
}
