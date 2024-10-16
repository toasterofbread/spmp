package com.toasterofbread.spmp.widget.configuration

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
import dev.toastbits.composekit.settings.ui.component.item.SliderSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ThemeSelectorSettingsItem
import dev.toastbits.composekit.utils.common.roundTo
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

private const val DEFAULT_BACKGROUND_OPACITY: Float = 1f

@Serializable
data class BaseWidgetConfiguration(
    val theme_index: Int? = null,
    val content_colour: ContentColour = ContentColour.THEME,
    val background_opacity: Float = DEFAULT_BACKGROUND_OPACITY
) {
    @Composable
    fun ConfigurationItems(context: AppContext, item_modifier: Modifier = Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
        ThemeIndexItem(context, item_modifier, onChanged)
        ContentColourItem(context, item_modifier, onChanged)
        BackgroundOpacityItem(context, item_modifier, onChanged)
    }

    @Composable
    private fun ThemeIndexItem(context: AppContext, modifier: Modifier, onChanged: (BaseWidgetConfiguration) -> Unit) {
        val theme_index_state: MutableState<Int> = remember { mutableIntStateOf(theme_index?.plus(1) ?: 0) }
        val theme_index_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                theme_index_state,
                { "WIDGET THEME" },
                { null }
            )
        }

        remember {
            createThemeSelectorSettingsItem(
                context,
                theme_index_property,
                getExtraStartThemes = {
                    listOf(
                        NamedTheme("APPLICATION THEME", ThemeValuesData.of(context.theme))
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
                { "CONTENT COLOUR" },
                { null }
            )
        }

        remember {
            DropdownSettingsItem(content_colour_property) {
                when (it) {
                    ContentColour.THEME -> "THEME"
                    ContentColour.LIGHT -> "LIGHT"
                    ContentColour.DARK -> "DARK"
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
                { "BACKGROUND OPACITY" },
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

    enum class ContentColour {
        THEME,
        LIGHT,
        DARK
    }
}
