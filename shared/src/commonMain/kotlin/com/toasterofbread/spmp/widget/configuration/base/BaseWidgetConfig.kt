package com.toasterofbread.spmp.widget.configuration.base

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.settings.category.AccentColourSource
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import com.toasterofbread.spmp.widget.configuration.WidgetConfig
import com.toasterofbread.spmp.widget.configuration.enum.WidgetStyledBorderMode
import dev.toastbits.composekit.navigation.compositionlocal.LocalNavigator
import dev.toastbits.composekit.navigation.navigator.Navigator
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.StateSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ThemeSelectorSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.theme.provider.ThemeStorageHandlerProvider
import dev.toastbits.composekit.settingsitem.presentation.ui.component.util.ThemeStorageHandlerProviderImpl
import dev.toastbits.composekit.settingsitem.presentation.ui.screen.ThemeConfirmationScreen
import dev.toastbits.composekit.settingsitem.presentation.ui.screen.ThemePickerScreen
import dev.toastbits.composekit.settingsitem.presentation.ui.screen.ThemePickerScreen.ResultHandler
import dev.toastbits.composekit.theme.config.ThemeTypeConfigProviderImpl
import dev.toastbits.composekit.theme.core.ThemeValues
import dev.toastbits.composekit.theme.core.model.ComposeKitFont
import dev.toastbits.composekit.theme.core.model.SerialisableTheme
import dev.toastbits.composekit.theme.core.model.ThemeReference
import dev.toastbits.composekit.theme.core.provider.ThemeProvider
import dev.toastbits.composekit.theme.core.provider.ThemeTypeConfigProvider
import dev.toastbits.composekit.theme.core.type.ThemeType
import dev.toastbits.composekit.theme.core.type.ThemeTypeConfig
import dev.toastbits.composekit.theme.core.ui.LocalThemeProvider
import dev.toastbits.composekit.theme.core.util.rememberAvailableFonts
import dev.toastbits.composekit.util.composable.getValue
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
    val theme: ThemeReference? = null,
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
            defaults_mask?.theme,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(theme = it)) }
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
            val available_fonts: List<ComposeKitFont> = ComposeKitFont.rememberAvailableFonts()

            DropdownItem(
                if (font == null) 0 else (available_fonts.indexOf(font) + 1),
                available_fonts.size + 1,
                Res.string.widget_config_common_key_font,
                modifier,
                getItemName = {
                    if (it == 0) stringResource(Res.string.widget_config_common_option_font_app)
                    else available_fonts[it - 1].getDisplayName()
                }
            ) {
                onChanged(
                    copy(
                        font =
                            if (it == 0) null
                            else available_fonts[it - 1]
                    )
                )
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
        val navigator: Navigator = LocalNavigator.current
        val themeProvider: ThemeProvider = LocalThemeProvider.current

        val widget_application_theme_label: String =
            stringResource(Res.string.widget_application_theme_label)
        val application_theme_reference: ThemeReference =
            remember { ThemeReference.CustomTheme(-1) }

        val current_application_theme: ThemeReference? by context.settings.Theme.CURRENT_THEME.observe()

        val theme_property: PlatformSettingsProperty<ThemeReference> =
            remember {
                StateSettingsProperty(
                    initialValue = theme ?: application_theme_reference,
                    onValueSet = { value ->
                        onChanged(
                            this.copy(
                                theme =
                                if (value == application_theme_reference) null
                                else value
                            )
                        )
                    },
                    getPropertyName = { stringResource(Res.string.widget_config_common_key_theme) },
                    getPropertyDescription = { null }
                )
            }

        fun <T: ThemeValues> ThemeType.ThemeAndType<T>.callResultHandler(
            resultHandler: ResultHandler,
            navigator: Navigator,
            storageHandlerProvider: ThemeStorageHandlerProvider,
            themeTypeConfigProvider: ThemeTypeConfigProvider
        ) {
            resultHandler.onResult(
                navigator,
                theme,
                themeTypeConfigProvider.getConfig(type),
                storageHandlerProvider
            )
        }

        remember(widget_application_theme_label) {
            val storageHandlerProvider: ThemeStorageHandlerProvider =
                ThemeStorageHandlerProviderImpl(theme_property, context.settings.Theme.CUSTOM_THEMES)

            val pickerResultProvider: ResultHandler =
                object : ThemePickerScreen.ResultHandler {
                    override fun <T : ThemeValues> onResult(
                        navigator: Navigator,
                        initialTheme: T,
                        config: ThemeTypeConfig<T>,
                        themeStorageHandlerProvider: ThemeStorageHandlerProvider
                    ) {
                        navigator.pushScreen(
                            ThemeConfirmationScreen(
                                initialTheme,
                                config,
                                themeStorageHandlerProvider(config.type),
                                onFinished = {
                                    navigator.navigateBackward(2)
                                }
                            )
                        )
                    }
                }

            ThemeSelectorSettingsItem(
                currentThemeProperty = theme_property,
                customThemesProperty = context.settings.Theme.CUSTOM_THEMES,
                canSelectThemesDirectly = false,
                themeStorageHandlerProvider = storageHandlerProvider,
                extraThemes =
                    listOf(
                        application_theme_reference
                    ),
                onExtraThemeEdited = {
                    if (it != 0) {
                        return@ThemeSelectorSettingsItem
                    }

                    onChanged(this.copy(theme = null))
                    navigator.navigateBackward()
                },
                themeTypeConfigProvider = ThemeTypeConfigProviderImpl,
                pickerResultHandler = pickerResultProvider
            )
        }.Item(modifier)
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
