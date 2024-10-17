package com.toasterofbread.spmp.widget

import androidx.glance.appwidget.GlanceAppWidget
import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import com.toasterofbread.spmp.widget.impl.LyricsLineHorizontalWidget
import kotlin.reflect.KClass

enum class SpMpWidgetType(
    val widgetClass: KClass<out GlanceAppWidget>,
    val defaultConfiguration: TypeWidgetConfiguration<*>,
    val clickActionClass: KClass<out TypeWidgetClickAction>
) {
    LYRICS_LINE_HORIZONTAL(LyricsLineHorizontalWidget::class, LyricsWidgetConfiguration(), LyricsWidgetClickAction::class),
//    CONTROLS_BASIC(BasicControlsWidget::class, null);
}
