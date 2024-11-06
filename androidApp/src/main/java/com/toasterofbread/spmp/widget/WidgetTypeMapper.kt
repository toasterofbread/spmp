package com.toasterofbread.spmp.widget

import android.content.ComponentName
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlin.reflect.KClass

internal fun SpMpWidgetType.getWidgetReceiverClass(): KClass<out GlanceAppWidgetReceiver> =
    when (this) {
        SpMpWidgetType.LYRICS_LINE_HORIZONTAL -> LyricsLineHorizontalWidgetReceiver::class
        SpMpWidgetType.SONG_QUEUE -> SongQueueWidgetReceiver::class
        SpMpWidgetType.SPLIT_IMAGE_CONTROLS -> SplitImageControlsWidgetReceiver::class
    }

internal fun getSpMpWidgetTypeForActivityInfo(provider: ComponentName): SpMpWidgetType =
    SpMpWidgetType.entries.firstOrNull { it.getWidgetReceiverClass().qualifiedName == provider.className }
        ?: throw RuntimeException("No SpMpWidgetType found for $provider")
