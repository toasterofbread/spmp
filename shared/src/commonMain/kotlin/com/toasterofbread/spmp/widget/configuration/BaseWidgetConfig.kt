package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.settings.category.FontMode
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.observeUiLanguage
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.createThemeSelectorSettingsItem
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.settings.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.utils.common.thenIf
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_application_theme_label
import spmp.shared.generated.resources.widget_config_button_use_default_value
import spmp.shared.generated.resources.widget_config_common_key_background_opacity
import spmp.shared.generated.resources.widget_config_common_key_border_radius
import spmp.shared.generated.resources.widget_config_common_key_content_colour
import spmp.shared.generated.resources.widget_config_common_key_font
import spmp.shared.generated.resources.widget_config_common_key_font_size
import spmp.shared.generated.resources.widget_config_common_key_hide_when_no_content
import spmp.shared.generated.resources.widget_config_common_key_show_debug_information
import spmp.shared.generated.resources.widget_config_common_key_theme
import spmp.shared.generated.resources.widget_config_common_option_content_colour_dark
import spmp.shared.generated.resources.widget_config_common_option_content_colour_light
import spmp.shared.generated.resources.widget_config_common_option_content_colour_theme
import spmp.shared.generated.resources.widget_config_common_option_font_app
import kotlin.math.roundToInt

private const val DEFAULT_BACKGROUND_OPACITY: Float = 1f

abstract class WidgetConfig {
    protected fun LazyListScope.configItem(
        default_mask_value: Boolean?,
        modifier: Modifier,
        onDefaultMaskValueChanged: (Boolean) -> Unit,
        content: @Composable (Modifier) -> Unit
    ) {
        item {
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (default_mask_value != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            SpMpWidgetConfiguration.DEFAULTS_ICON,
                            stringResource(Res.string.widget_config_button_use_default_value),
                            Modifier.size(15.dp)
                        )
                        RadioButton(
                            default_mask_value,
                            { onDefaultMaskValueChanged(!default_mask_value) },
                            Modifier.size(25.dp)
                        )
                    }
                }

                content(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .thenIf(default_mask_value == true) {
                            graphicsLayer { alpha = 0.5f; clip = false }
                        }
                )
            }
        }
    }
}

@Serializable
data class BaseWidgetConfig(
    val theme_index: Int? = null,
    val font: FontMode? = null,
    val font_size: Float = 1f,
    val content_colour: ContentColour = ContentColour.THEME,
    val background_opacity: Float = DEFAULT_BACKGROUND_OPACITY,
    val border_radius_dp: Float = 0f,
    val hide_when_no_content: Boolean = false,
    val show_debug_information: Boolean = ProjectBuildConfig.IS_DEBUG
): WidgetConfig() {
    fun LazyListScope.ConfigItems(
        context: AppContext,
        defaults_mask: BaseWidgetConfigDefaultsMask?,
        item_modifier: Modifier = Modifier,
        onChanged: (BaseWidgetConfig) -> Unit,
        onDefaultsMaskChanged: (BaseWidgetConfigDefaultsMask) -> Unit
    ) {
        configItem(
            defaults_mask?.theme_index,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(theme_index = it)) }
        ) {
            ThemeIndexItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.font,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(font = it)) }
        ) {
            FontItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.font_size,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(font_size = it)) }
        ) {
            FontSizeItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.content_colour,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(content_colour = it)) }
        ) {
            ContentColourItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.background_opacity,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(background_opacity = it)) }
        ) {
            BackgroundOpacityItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.border_radius_dp,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(border_radius_dp = it)) }
        ) {
            BorderRadiusItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.hide_when_no_content,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(hide_when_no_content = it)) }
        ) {
            HideWhenNoContentItem(context, it, onChanged)
        }
        configItem(
            defaults_mask?.show_debug_information,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.copy(show_debug_information = it)) }
        ) {
            ShowDebugInformationItem(context, it, onChanged)
        }
    }

    @Composable
    private fun ThemeIndexItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val theme_index_state: MutableState<Int> = remember { mutableIntStateOf(theme_index?.plus(1) ?: 0) }
        val theme_index_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                theme_index_state,
                { stringResource(Res.string.widget_config_common_key_theme) },
                { null }
            )
        }

        remember {
            createThemeSelectorSettingsItem(
                context,
                theme_index_property,
                getExtraStartThemes = {
                    listOf(
                        NamedTheme(stringResource(Res.string.widget_application_theme_label), ThemeValuesData.of(context.theme))
                    )
                }
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

    @Composable
    private fun FontItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val font_state: MutableState<Int> = remember { mutableIntStateOf(font?.ordinal?.plus(1) ?: 0) }
        val font_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                font_state,
                { stringResource(Res.string.widget_config_common_key_font) },
                { null }
            )
        }

        val ui_language: String by context.observeUiLanguage()

        remember {
            DropdownSettingsItem(
                font_property,
                FontMode.entries.size + 1
            ) {
                if (it == 0) {
                    stringResource(Res.string.widget_config_common_option_font_app)
                }
                else {
                    FontMode.entries[it - 1].getReadable(ui_language)
                }
            }
        }.Item(modifier)

        OnChangedEffect(font_state.value) {
            onChanged(
                this.copy(
                    font = font_state.value.let {
                        if (it == 0) null else FontMode.entries[it - 1]
                    }
                )
            )
        }
    }


    @Composable
    private fun FontSizeItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val font_size_state: MutableState<Float> = remember { mutableFloatStateOf(font_size) }
        val font_size_property: PreferencesProperty<Float> = remember {
            MutableStatePreferencesProperty(
                font_size_state,
                { stringResource(Res.string.widget_config_common_key_font_size) },
                { null },
                getPropertyDefaultValue = { 1f },
                getPropertyDefaultValueComposable = { 1f }
            )
        }

        OnChangedEffect(font_size_state.value) {
            onChanged(this.copy(font_size = font_size_state.value))
        }

        remember {
            AppSliderItem(
                font_size_property,
                range = 0.1f..5f
            )
        }.Item(modifier)
    }

    @Composable
    private fun ContentColourItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val content_colour_state: MutableState<ContentColour> = remember { mutableStateOf(content_colour) }
        val content_colour_property: PreferencesProperty<ContentColour> = remember {
            MutableStatePreferencesProperty(
                content_colour_state,
                { stringResource(Res.string.widget_config_common_key_content_colour) },
                { null }
            )
        }

        remember {
            DropdownSettingsItem(content_colour_property) {
                when (it) {
                    ContentColour.THEME -> stringResource(Res.string.widget_config_common_option_content_colour_theme)
                    ContentColour.LIGHT -> stringResource(Res.string.widget_config_common_option_content_colour_light)
                    ContentColour.DARK -> stringResource(Res.string.widget_config_common_option_content_colour_dark)
                }
            }
        }.Item(modifier)

        OnChangedEffect(content_colour_state.value) {
            onChanged(
                this.copy(content_colour = content_colour_state.value)
            )
        }
    }

    @Composable
    private fun BackgroundOpacityItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val background_opacity_state: MutableState<Float> = remember { mutableFloatStateOf(background_opacity) }
        val background_opacity_property: PreferencesProperty<Float> = remember {
            MutableStatePreferencesProperty(
                background_opacity_state,
                { stringResource(Res.string.widget_config_common_key_background_opacity) },
                { null },
                getPropertyDefaultValue = { DEFAULT_BACKGROUND_OPACITY },
                getPropertyDefaultValueComposable = { DEFAULT_BACKGROUND_OPACITY }
            )
        }

        remember {
            AppSliderItem(
                background_opacity_property,
                getValueText = {
                    (it as Float).times(100).roundToInt().toString() + '%'
                }
            )
        }.Item(modifier)

        OnChangedEffect(background_opacity_state.value) {
            onChanged(
                this.copy(background_opacity = background_opacity_state.value)
            )
        }
    }

    @Composable
    private fun BorderRadiusItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val border_radius_state: MutableState<Float> = remember { mutableFloatStateOf(border_radius_dp) }
        val border_radius_property: PreferencesProperty<Float> = remember {
            MutableStatePreferencesProperty(
                border_radius_state,
                { stringResource(Res.string.widget_config_common_key_border_radius) },
                { null },
                getPropertyDefaultValue = { 1f },
                getPropertyDefaultValueComposable = { 1f }
            )
        }

        OnChangedEffect(border_radius_state.value) {
            onChanged(this.copy(border_radius_dp = border_radius_state.value))
        }

        remember {
            AppSliderItem(
                border_radius_property,
                range = 0f..5f
            )
        }.Item(modifier)
    }

    @Composable
    private fun HideWhenNoContentItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val hide_when_no_content_state: MutableState<Boolean> = remember { mutableStateOf(hide_when_no_content) }
        val hide_when_no_content_property: PreferencesProperty<Boolean> = remember {
            MutableStatePreferencesProperty(
                hide_when_no_content_state,
                { stringResource(Res.string.widget_config_common_key_hide_when_no_content) },
                { null },
                getPropertyDefaultValue = { false },
                getPropertyDefaultValueComposable = { false }
            )
        }

        remember {
            ToggleSettingsItem(hide_when_no_content_property)
        }.Item(modifier)

        OnChangedEffect(hide_when_no_content_state.value) {
            onChanged(
                this.copy(hide_when_no_content = hide_when_no_content_state.value)
            )
        }
    }

    @Composable
    private fun ShowDebugInformationItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfig) -> Unit) {
        val show_debug_information_state: MutableState<Boolean> = remember { mutableStateOf(show_debug_information) }
        val show_debug_information_property: PreferencesProperty<Boolean> = remember {
            MutableStatePreferencesProperty(
                show_debug_information_state,
                { stringResource(Res.string.widget_config_common_key_show_debug_information) },
                { null },
                getPropertyDefaultValue = { false },
                getPropertyDefaultValueComposable = { false }
            )
        }

        remember {
            ToggleSettingsItem(show_debug_information_property)
        }.Item(modifier)

        OnChangedEffect(show_debug_information_state.value) {
            onChanged(
                this.copy(show_debug_information = show_debug_information_state.value)
            )
        }
    }

    enum class ContentColour {
        THEME,
        LIGHT,
        DARK
    }
}
