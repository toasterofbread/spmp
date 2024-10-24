package com.toasterofbread.spmp.widget.configuration.type

import kotlinx.serialization.Serializable

@Serializable
data class SplitImageControlsWidgetConfigDefaultsMask(
    override val click_action: Boolean = true
): TypeConfigurationDefaultsMask<SplitImageControlsWidgetConfig> {
    override fun applyTo(
        config: SplitImageControlsWidgetConfig,
        default: SplitImageControlsWidgetConfig
    ): SplitImageControlsWidgetConfig =
        SplitImageControlsWidgetConfig(
            click_action = if (click_action) default.click_action else config.click_action
        )

    override fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<SplitImageControlsWidgetConfig> =
        copy(click_action = click_action)
}