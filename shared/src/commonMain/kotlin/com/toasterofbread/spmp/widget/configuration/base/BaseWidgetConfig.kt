package com.toasterofbread.spmp.widget.configuration.base

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.configuration.WidgetConfig
import com.toasterofbread.spmp.widget.configuration.enum.WidgetStyledBorderMode
import dev.toastbits.composekit.commonsettings.impl.LocalComposeKitSettings
import dev.toastbits.composekit.commonsettings.impl.group.rememberThemeConfiguration
import dev.toastbits.composekit.settings.MutableStateSettingsProperty
import dev.toastbits.composekit.settings.PlatformSettingsProperty
import dev.toastbits.composekit.settings.ui.component.item.ThemeSelectorSettingsItem
import dev.toastbits.composekit.theme.model.ComposeKitFont
import dev.toastbits.composekit.theme.model.NamedTheme
import dev.toastbits.composekit.theme.model.ThemeConfiguration
import dev.toastbits.composekit.theme.model.ThemeValuesData
import dev.toastbits.composekit.util.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_application_theme_label
import spmp.shared.generated.resources.widget_config_common_key_accent_colour_source
import spmp.shared.generated.resources.widget_config_common_key_background_opacity
import spmp.shared.generated.resources.widget_config_common_key_border_radius
import spmp.shared.generated.resources.widget_config_common_key_content_colour
import spmp.shared.generated.resources.widget_config_common_key_font
import spmp.shared.generated.resources.widget_config_common_key_font_size
import spmp.shared.generated.resources.widget_config_common_key_hide_when_no_content
import spmp.shared.generated.resources.widget_config_common_key_show_app_icon
import spmp.shared.generated.resources.widget_config_common_key_styled_border_mode
import spmp.shared.generated.resources.widget_config_common_key_theme
import spmp.shared.generated.resources.widget_config_common_optionAccent_colour_source_app
import spmp.shared.generated.resources.widget_config_common_option_content_colour_dark
import spmp.shared.generated.resources.widget_config_common_option_content_colour_light
import spmp.shared.generated.resources.widget_config_common_option_content_colour_theme
import spmp.shared.generated.resources.widget_config_common_option_font_app
import spmp.shared.generated.resources.widget_config_common_option_styled_border_mode_none
import spmp.shared.generated.resources.widget_config_common_option_styled_border_mode_wave
import kotlin.math.roundToInt

@Serializable
data class BaseWidgetConfig(
    val theme_index: Int? = null,
    val accent_colour_source: AccentColourSource? = null,
    val font: ComposeKitFont? = null,
    val font_size: Float = 1f,
    val content_colour: ContentColour = ContentColour.THEME,
    val background_opacity: Float = 1f,
    val styled_border_mode: WidgetStyledBorderMode = WidgetStyledBorderMode.WAVE,
    val border_radius_dp: Float = 0f,
    val hide_when_no_content: Boolean = false,
    val show_app_icon: Boolean = true
): WidgetConfig() {
    fun LazyListScope.ConfigItems(
        context: AppContext,
        widget_type: SpMpWidgetType?,
        defaults_mask: BaseWidgetConfigDefaultsMask?,
        item_modifier: Modifier = Modifier,
        onChanged: (BaseWidgetConfig) -> Unit,
        onDefaultsMaskChanged: (BaseWidgetConfigDefaultsMask) -> Unit
    ) {
        configItem(
            defaults_mask?.theme_index,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(theme_index = it)) }
        ) { modifier, onItemChanged ->
            ThemeIndexItem(context, modifier) {
                onChanged(it)
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.accent_colour_source,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(accent_colour_source = it)) }
        ) { modifier, onItemChanged ->
            NullableDropdownItem(
                accent_colour_source,
                Res.string.widget_config_common_key_accent_colour_source,
                modifier,
                getItemName = {
                    stringResource(it?.getNameResource() ?: Res.string.widget_config_common_optionAccent_colour_source_app)
                }
            ) {
                onChanged(copy(accent_colour_source = it))
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.font,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(font = it)) }
        ) { modifier, onItemChanged ->
            val ui_language: String by context.observeUiLanguage()
            NullableDropdownItem(
                font,
                Res.string.widget_config_common_key_font,
                modifier,
                getItemName = {
                    it?.getReadable(ui_language) ?: stringResource(Res.string.widget_config_common_option_font_app)
                }
            ) {
                onChanged(copy(font = it))
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.font_size,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(font_size = it)) }
        ) { modifier, onItemChanged ->
            SliderItem(
                font_size,
                DEFAULT.font_size,
                Res.string.widget_config_common_key_font_size,
                modifier,
                range = 0.1f..5f,
            ) {
                onChanged(copy(font_size = it))
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.content_colour,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(content_colour = it)) }
        ) { modifier, onItemChanged ->
            DropdownItem(
                content_colour,
                Res.string.widget_config_common_key_content_colour,
                modifier,
                getItemName = {
                    when (it) {
                        ContentColour.THEME -> stringResource(Res.string.widget_config_common_option_content_colour_theme)
                        ContentColour.LIGHT -> stringResource(Res.string.widget_config_common_option_content_colour_light)
                        ContentColour.DARK -> stringResource(Res.string.widget_config_common_option_content_colour_dark)
                    }
                }
            ) {
                onChanged(copy(content_colour = it))
                onItemChanged()
            }
        }
        if (widget_type?.uses_standard_background != false) {
            configItem(
                defaults_mask?.background_opacity,
                item_modifier,
                { onDefaultsMaskChanged(defaults_mask!!.copy(background_opacity = it)) }
            ) { modifier, onItemChanged ->
                SliderItem(
                    background_opacity,
                    DEFAULT.background_opacity,
                    Res.string.widget_config_common_key_background_opacity,
                    modifier,
                    getValueText = {
                        (it as Float).times(100).roundToInt().toString() + '%'
                    }
                ) {
                    onChanged(copy(background_opacity = it))
                    onItemChanged()
                }
            }
        }
        configItem(
            defaults_mask?.styled_border_mode,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(styled_border_mode = it)) }
        ) { modifier, onItemChanged ->
            DropdownItem(
                styled_border_mode,
                Res.string.widget_config_common_key_styled_border_mode,
                modifier,
                getItemName = {
                    when (it) {
                        WidgetStyledBorderMode.WAVE -> stringResource(Res.string.widget_config_common_option_styled_border_mode_wave)
                        WidgetStyledBorderMode.NONE -> stringResource(Res.string.widget_config_common_option_styled_border_mode_none)
                    }
                }
            ) {
                onChanged(copy(styled_border_mode = it))
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.border_radius_dp,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(border_radius_dp = it)) }
        ) { modifier, onItemChanged ->
            SliderItem(
                border_radius_dp,
                DEFAULT.border_radius_dp,
                Res.string.widget_config_common_key_border_radius,
                modifier,
                range = 0f..5f
            ) {
                onChanged(copy(border_radius_dp = it))
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.hide_when_no_content,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(hide_when_no_content = it)) }
        ) { modifier, onItemChanged ->
            ToggleItem(
                hide_when_no_content,
                Res.string.widget_config_common_key_hide_when_no_content,
                modifier
            ) {
                onChanged(copy(hide_when_no_content = it))
                onItemChanged()
            }
        }
        configItem(
            defaults_mask?.show_app_icon,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(show_app_icon = it)) }
        ) { modifier, onItemChanged ->
            ToggleItem(
                show_app_icon,
                Res.string.widget_config_common_key_show_app_icon,
                modifier
            ) {
                onChanged(copy(show_app_icon = it))
                onItemChanged()
            }
        }
    }

    @Composable
    private fun ThemeIndexItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val theme_index_state: MutableState<Int> =
            remember { mutableIntStateOf(theme_index?.plus(1) ?: 0) }
        val theme_index_property: PlatformSettingsProperty<Int> = remember {
            MutableStateSettingsProperty(
                theme_index_state,
                { stringResource(Res.string.widget_config_common_key_theme) },
                { null }
            )
        }

        val widgetApplicationThemeLabel: String = stringResource(Res.string.widget_application_theme_label)

        remember(widgetApplicationThemeLabel) {
            ThemeSelectorSettingsItem(
                getThemeConfiguration = {
                    LocalComposeKitSettings.current?.Theme?.rememberThemeConfiguration() ?: ThemeConfiguration()
                },
                themeIndexProperty = theme_index_property,
                themesProperty = context.settings.Theme.THEMES,
                extraStartThemes =
                    listOf(
                        NamedTheme(
                            widgetApplicationThemeLabel,
                            ThemeValuesData.of(context.theme)
                        )
                    )
            )
        }.Item(modifier)

        OnChangedEffect(theme_index_state.value) {
            onChanged(
                this.copy(
                    theme_index =
                    theme_index_state.value.let { index ->
                        if (index <= 0) null
                        else index - 1
                    }
                )
            )
        }
    }

    enum class ContentColour {
        THEME,
        LIGHT,
        DARK
    }

    companion object {
        private val DEFAULT: BaseWidgetConfig = BaseWidgetConfig()
    }
}
