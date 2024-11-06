package com.toasterofbread.spmp.widget.configuration.type

import kotlinx.serialization.Serializable

@Serializable
data class SplitImageControlsWidgetConfigDefaultsMask(
    override val click_action: Boolean = true,
    val display_lyrics: Boolean = true,
    val swap_title_content_rows: Boolean = true,
    val title_row_theme: Boolean = true,
    val content_row_theme: Boolean = true,
    val top_start_button_action: Boolean = true,
    val top_end_button_action: Boolean = true,
    val bottom_start_button_action: Boolean = true,
    val bottom_end_button_action: Boolean = true
): TypeConfigurationDefaultsMask<SplitImageControlsWidgetConfig> {
    override fun applyTo(
        config: SplitImageControlsWidgetConfig,
        default: SplitImageControlsWidgetConfig
    ): SplitImageControlsWidgetConfig =
        SplitImageControlsWidgetConfig(
            click_action = if (click_action) default.click_action else config.click_action,
            display_lyrics = if (display_lyrics) default.display_lyrics else config.display_lyrics,
            swap_title_content_rows = if (swap_title_content_rows) default.swap_title_content_rows else config.swap_title_content_rows,
            title_row_theme = if (title_row_theme) default.title_row_theme else config.title_row_theme,
            content_row_theme = if (content_row_theme) default.content_row_theme else config.content_row_theme,
            top_start_button_action = if (top_start_button_action) default.top_start_button_action else config.top_start_button_action,
            top_end_button_action = if (top_end_button_action) default.top_end_button_action else config.top_end_button_action,
            bottom_start_button_action = if (bottom_start_button_action) default.bottom_start_button_action else config.bottom_start_button_action,
            bottom_end_button_action = if (bottom_end_button_action) default.bottom_end_button_action else config.bottom_end_button_action
        )

    override fun setClickAction(click_action: Boolean): TypeConfigurationDefaultsMask<SplitImageControlsWidgetConfig> =
        copy(click_action = click_action)
}