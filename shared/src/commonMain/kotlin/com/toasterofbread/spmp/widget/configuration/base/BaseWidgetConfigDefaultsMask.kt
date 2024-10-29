package com.toasterofbread.spmp.widget.configuration.base

import kotlinx.serialization.Serializable

@Serializable
data class BaseWidgetConfigDefaultsMask(
    val theme_index: Boolean = true,
    val font: Boolean = true,
    val font_size: Boolean = true,
    val content_colour: Boolean = true,
    val background_opacity: Boolean = true,
    val styled_border_mode: Boolean = true,
    val border_radius_dp: Boolean = true,
    val hide_when_no_content: Boolean = true,
    val show_debug_information: Boolean = true
) {
    fun applyTo(config: BaseWidgetConfig, default: BaseWidgetConfig): BaseWidgetConfig =
        BaseWidgetConfig(
            theme_index = if (this.theme_index) default.theme_index else config.theme_index,
            font = if (this.font) default.font else config.font,
            font_size = if (this.font_size) default.font_size else config.font_size,
            content_colour = if (this.content_colour) default.content_colour else config.content_colour,
            background_opacity = if (this.background_opacity) default.background_opacity else config.background_opacity,
            styled_border_mode = if (this.styled_border_mode) default.styled_border_mode else config.styled_border_mode,
            border_radius_dp = if (this.border_radius_dp) default.border_radius_dp else config.border_radius_dp,
            hide_when_no_content = if (this.hide_when_no_content) default.hide_when_no_content else config.hide_when_no_content,
            show_debug_information = if (this.show_debug_information) default.show_debug_information else config.show_debug_information,
        )
}