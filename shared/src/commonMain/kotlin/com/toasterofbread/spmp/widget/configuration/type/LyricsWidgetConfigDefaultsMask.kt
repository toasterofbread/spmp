package com.toasterofbread.spmp.widget.configuration.type

import kotlinx.serialization.Serializable

@Serializable
internal data class LyricsWidgetConfigDefaultsMask(
    val furigana_mode: Boolean = true,
    override val click_action: Boolean = true
): TypeConfigurationDefaultsMask<LyricsWidgetConfig> {
    override fun applyTo(
        config: LyricsWidgetConfig,
        default: LyricsWidgetConfig
    ): LyricsWidgetConfig =
        LyricsWidgetConfig(
            furigana_mode = if (furigana_mode) default.furigana_mode else config.furigana_mode,
            click_action = if (click_action) default.click_action else config.click_action
        )

    override fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<LyricsWidgetConfig> =
        copy(click_action = click_action)
}