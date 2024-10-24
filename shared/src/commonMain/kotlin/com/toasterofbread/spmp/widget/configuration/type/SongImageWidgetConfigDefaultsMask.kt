package com.toasterofbread.spmp.widget.configuration.type

import kotlinx.serialization.Serializable

@Serializable
data class SongImageWidgetConfigDefaultsMask(
    override val click_action: Boolean = true
): TypeConfigurationDefaultsMask<SongImageWidgetConfig> {
    override fun applyTo(
        config: SongImageWidgetConfig,
        default: SongImageWidgetConfig
    ): SongImageWidgetConfig =
        SongImageWidgetConfig(
            click_action = if (click_action) default.click_action else config.click_action
        )

    override fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<SongImageWidgetConfig> =
        copy(click_action = click_action)
}