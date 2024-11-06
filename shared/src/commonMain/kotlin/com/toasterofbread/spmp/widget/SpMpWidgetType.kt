package com.toasterofbread.spmp.widget

import com.toasterofbread.spmp.widget.action.LyricsWidgetClickAction
import com.toasterofbread.spmp.widget.action.SongImageWidgetClickAction
import com.toasterofbread.spmp.widget.action.SongQueueWidgetClickAction
import com.toasterofbread.spmp.widget.action.SplitImageControlsWidgetClickAction
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.configuration.type.LyricsWidgetConfig
import com.toasterofbread.spmp.widget.configuration.type.LyricsWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.SongImageWidgetConfig
import com.toasterofbread.spmp.widget.configuration.type.SongImageWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.SongQueueWidgetConfig
import com.toasterofbread.spmp.widget.configuration.type.SongQueueWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.SplitImageControlsWidgetConfig
import com.toasterofbread.spmp.widget.configuration.type.SplitImageControlsWidgetConfigDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeConfigurationDefaultsMask
import com.toasterofbread.spmp.widget.configuration.type.TypeWidgetConfig
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class SpMpWidgetType(
    val default_config: TypeWidgetConfig<*>,
    val default_defaults_mask: TypeConfigurationDefaultsMask<*>,
    val click_action_class: KClass<out TypeWidgetClickAction>,
    val update_types: List<WidgetUpdateType>,
    val uses_standard_background: Boolean = true
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
    ),
    SPLIT_IMAGE_CONTROLS(
        default_config = SplitImageControlsWidgetConfig(),
        default_defaults_mask = SplitImageControlsWidgetConfigDefaultsMask(),
        click_action_class = SplitImageControlsWidgetClickAction::class,
        update_types = listOf(WidgetUpdateType.OnSongTransition, WidgetUpdateType.OnPlayingChange),
        uses_standard_background = false
    );

    companion object
}

sealed interface WidgetUpdateType {
    data class DuringPlayback(val period: Duration): WidgetUpdateType
    data object OnSongTransition: WidgetUpdateType
    data object OnQueueChange: WidgetUpdateType
    data object OnPlayingChange: WidgetUpdateType
    data object OnCurrentSongLikedChanged: WidgetUpdateType
    data object OnAuthStateChanged: WidgetUpdateType
}
