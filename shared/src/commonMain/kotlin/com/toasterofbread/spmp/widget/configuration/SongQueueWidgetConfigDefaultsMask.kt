package com.toasterofbread.spmp.widget.configuration

import kotlinx.serialization.Serializable

@Serializable
data class SongQueueWidgetConfigDefaultsMask(
    val next_songs_to_show: Boolean = true,
    override val click_action: Boolean = true
): TypeConfigurationDefaultsMask<SongQueueWidgetConfig> {
    override fun applyTo(
        config: SongQueueWidgetConfig,
        default: SongQueueWidgetConfig
    ): SongQueueWidgetConfig =
        SongQueueWidgetConfig(
            next_songs_to_show = if (next_songs_to_show) default.next_songs_to_show else config.next_songs_to_show,
            click_action = if (click_action) default.click_action else config.click_action
        )

    override fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<SongQueueWidgetConfig> =
        copy(click_action = click_action)
}
