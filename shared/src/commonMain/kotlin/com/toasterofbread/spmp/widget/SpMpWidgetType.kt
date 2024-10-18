package com.toasterofbread.spmp.widget

import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.SongQueueWidgetConfiguration
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfiguration
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class SpMpWidgetType(
    val defaultConfiguration: TypeWidgetConfiguration<*>,
    val clickActionClass: KClass<out TypeWidgetClickAction>,
    val updateTypes: List<WidgetUpdateType>
) {
    LYRICS_LINE_HORIZONTAL(
        defaultConfiguration = LyricsWidgetConfiguration(),
        clickActionClass = LyricsWidgetClickAction::class,
        updateTypes = listOf(WidgetUpdateType.DuringPlayback(500.milliseconds))
    ),
    SONG_QUEUE(
        defaultConfiguration = SongQueueWidgetConfiguration(),
        clickActionClass = SongQueueWidgetClickAction::class,
        updateTypes = listOf(WidgetUpdateType.OnSongTransition, WidgetUpdateType.OnQueueChange)
    );

    companion object
}

sealed interface WidgetUpdateType {
    data class DuringPlayback(val period: Duration): WidgetUpdateType
    data object OnSongTransition: WidgetUpdateType
    data object OnQueueChange: WidgetUpdateType
}
