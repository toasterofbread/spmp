package com.toasterofbread.spmp.widget.configuration.type

import kotlinx.serialization.Serializable

@Serializable
data class SongQueueWidgetConfigDefaultsMask(
    val show_current_song: Boolean = true,
    val next_songs_to_show: Boolean = true,
    override val click_action: Boolean = true
): TypeConfigurationDefaultsMask<SongQueueWidgetConfig> {
    override fun applyTo(
        config: SongQueueWidgetConfig,
        default: SongQueueWidgetConfig
    ): SongQueueWidgetConfig =
        SongQueueWidgetConfig(
            show_current_song = if (show_current_song) default.show_current_song else config.show_current_song,
            next_songs_to_show = if (next_songs_to_show) default.next_songs_to_show else config.next_songs_to_show,
            click_action = if (click_action) default.click_action else config.click_action
        )

    override fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<SongQueueWidgetConfig> =
        copy(click_action = click_action)
}