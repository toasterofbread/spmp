package com.toasterofbread.spmp.widget

import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfig
import com.toasterofbread.spmp.widget.configuration.LyricsWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.SongQueueWidgetConfig
import com.toasterofbread.spmp.widget.configuration.SongQueueWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.TypeConfigurationDefaultsMask
import com.toasterofbread.spmp.widget.configuration.TypeWidgetConfig
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class SpMpWidgetType(
    val default_config: TypeWidgetConfig<*>,
    val default_defaults_mask: TypeConfigurationDefaultsMask<*>,
    val click_action_class: KClass<out TypeWidgetClickAction>,
    val update_types: List<WidgetUpdateType>
) {
    LYRICS_LINE_HORIZONTAL(
        default_config = LyricsWidgetConfig(),
        default_defaults_mask = LyricsWidgetConfigDefaultsMask(),
        click_action_class = LyricsWidgetClickAction::class,
        update_types = listOf(WidgetUpdateType.DuringPlayback(500.milliseconds))
    ),
    SONG_QUEUE(
        default_config = SongQueueWidgetConfig(),
        default_defaults_mask = SongQueueWidgetConfigDefaultsMask(),
        click_action_class = SongQueueWidgetClickAction::class,
        update_types = listOf(WidgetUpdateType.OnSongTransition, WidgetUpdateType.OnQueueChange)
    );

    companion object
}

sealed interface WidgetUpdateType {
    data class DuringPlayback(val period: Duration): WidgetUpdateType
    data object OnSongTransition: WidgetUpdateType
    data object OnQueueChange: WidgetUpdateType
}
