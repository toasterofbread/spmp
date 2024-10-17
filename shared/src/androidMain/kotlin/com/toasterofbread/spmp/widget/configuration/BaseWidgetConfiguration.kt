package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.category.createThemeSelectorSettingsItem
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.NamedTheme
import dev.toastbits.composekit.settings.ui.ThemeValuesData
import dev.toastbits.composekit.settings.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_application_theme_label
import spmp.shared.generated.resources.widget_config_common_key_background_opacity
import spmp.shared.generated.resources.widget_config_common_key_content_colour
import spmp.shared.generated.resources.widget_config_common_key_hide_when_no_content
import spmp.shared.generated.resources.widget_config_common_key_theme
import spmp.shared.generated.resources.widget_config_common_option_content_colour_dark
import spmp.shared.generated.resources.widget_config_common_option_content_colour_light
import spmp.shared.generated.resources.widget_config_common_option_content_colour_theme
import kotlin.math.roundToInt

private const val DEFAULT_BACKGROUND_OPACITY: Float = 1f

@Serializable
data class BaseWidgetConfiguration(
    val theme_index: Int? = null,
    val content_colour: ContentColour = ContentColour.THEME,
    val background_opacity: Float = DEFAULT_BACKGROUND_OPACITY,
    val hide_when_no_content: Boolean = false
) {
    fun LazyListScope.ConfigurationItems(context: AppContext, item_modifier: Modifier = Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
        item {
            ThemeIndexItem(context, item_modifier, onChanged)
        }
        item {
            ContentColourItem(context, item_modifier, onChanged)
        }
        item {
            BackgroundOpacityItem(context, item_modifier, onChanged)
        }
        item {
            HideWhenNoContentItem(context, item_modifier, onChanged)
        }
    }

    @Composable
    private fun ThemeIndexItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
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
    private fun ContentColourItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
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
    private fun BackgroundOpacityItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
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
    private fun HideWhenNoContentItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
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

    enum class ContentColour {
        THEME,
        LIGHT,
        DARK
    }
}
