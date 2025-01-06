package com.toasterofbread.spmp.widget.configuration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionTheme
import dev.toastbits.composekit.components.utils.composable.WithStickySize
import dev.toastbits.composekit.settingsitem.domain.MutableStateSettingsProperty
import dev.toastbits.composekit.settingsitem.domain.PlatformSettingsProperty
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.SliderSettingsItem
import dev.toastbits.composekit.settingsitem.presentation.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.util.composable.OnChangedEffect
import dev.toastbits.composekit.util.roundTo
import dev.toastbits.composekit.util.thenIf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_button_use_default_value
import spmp.shared.generated.resources.widget_config_common_key_section_theme_opacity
import spmp.shared.generated.resources.widget_config_common_option_section_theme_mode_accent
import spmp.shared.generated.resources.widget_config_common_option_section_theme_mode_background
import spmp.shared.generated.resources.widget_config_common_option_section_theme_mode_transparent
import kotlin.enums.enumEntries

abstract class WidgetConfig {
    protected fun LazyListScope.configItem(
        default_mask_value: Boolean?,
        modifier: Modifier,
        onDefaultMaskValueChanged: (Boolean) -> Unit,
        content: @Composable (Modifier, onChange: () -> Unit) -> Unit
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
                ) {
                    if (default_mask_value != null) {
                        onDefaultMaskValueChanged(false)
                    }
                }
            }
        }
    }


    @Composable
    protected fun SectionThemeItem(
        theme: WidgetSectionTheme,
        title: StringResource,
        modifier: Modifier = Modifier,
        onChanged: (WidgetSectionTheme) -> Unit
    ) {
        var show_opacity_slider: Boolean by remember { mutableStateOf(false) }

        val mode_state: MutableState<WidgetSectionTheme.Mode> = remember { mutableStateOf(theme.mode) }
        val mode_property: PlatformSettingsProperty<WidgetSectionTheme.Mode> = remember {
            MutableStateSettingsProperty(
                mode_state,
                { stringResource(title) },
                { null }
            )
        }
        val mode_item: DropdownSettingsItem = remember {
            DropdownSettingsItem.ofEnumState(
                mode_property
            ) {
                when (it) {
                    WidgetSectionTheme.Mode.BACKGROUND -> stringResource(Res.string.widget_config_common_option_section_theme_mode_background)
                    WidgetSectionTheme.Mode.ACCENT -> stringResource(Res.string.widget_config_common_option_section_theme_mode_accent)
                    WidgetSectionTheme.Mode.TRANSPARENT -> stringResource(Res.string.widget_config_common_option_section_theme_mode_transparent)
                }
            }
        }

        val opacity_state: MutableState<Float> = remember { mutableStateOf(theme.opacity) }
        val opacity_property: PlatformSettingsProperty<Float> = remember {
            MutableStateSettingsProperty(
                opacity_state,
                { stringResource(Res.string.widget_config_common_key_section_theme_opacity) },
                { null },
                getPropertyDefaultValue = { WidgetSectionTheme.DEFAULT_OPACITY }
            )
        }
        val opacity_item: SliderSettingsItem? = remember(theme.mode) {
            if (theme.mode.opacity_configurable) AppSliderItem(opacity_property)
            else null
        }

        OnChangedEffect(mode_state.value, opacity_state.value) {
            onChanged(theme.copy(mode = mode_state.value, opacity = opacity_state.value))
        }

        OnChangedEffect(theme.mode.opacity_configurable) {
            if (!theme.mode.opacity_configurable) {
                show_opacity_slider = false
            }
        }

        Column(modifier) {
            WithStickySize(Modifier.fillMaxWidth()) { size_modifier, size ->
                Row(
                    size_modifier.heightIn(min = size.height),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    mode_item.Item(Modifier.fillMaxWidth().weight(1f))

                    AnimatedVisibility(theme.mode.opacity_configurable) {
                        IconButton({ show_opacity_slider = !show_opacity_slider }) {
                            Crossfade(show_opacity_slider) { show ->
                                Icon(
                                    if (show) Icons.Default.Close
                                    else Icons.Default.Opacity,
                                    stringResource(Res.string.widget_config_common_key_section_theme_opacity)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(show_opacity_slider) {
                opacity_item?.Item(Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    protected fun ToggleItem(
        state: Boolean,
        title: StringResource,
        modifier: Modifier = Modifier,
        onChanged: (Boolean) -> Unit
    ) {
        val current_state: MutableState<Boolean> =
            remember { mutableStateOf(state) }

        val state_property: PlatformSettingsProperty<Boolean> = remember {
            MutableStateSettingsProperty(
                current_state,
                { stringResource(title) },
                { null }
            )
        }

        OnChangedEffect(current_state.value) {
            onChanged(current_state.value)
        }

        remember {
            ToggleSettingsItem(state_property)
        }.Item(modifier)
    }

    @Composable
    protected fun <T: Number> SliderItem(
        value: T,
        default_value: T,
        title: StringResource,
        modifier: Modifier = Modifier,
        range: ClosedFloatingPointRange<Float> = 0f .. 1f,
        getValueText: ((value: Number) -> String?)? = {
            if (it is Float) it.roundTo(2).toString()
            else it.toString()
        },
        onChanged: (T) -> Unit
    ) {
        val value_state: MutableState<T> =
            remember { mutableStateOf(value) }
        val value_property: PlatformSettingsProperty<T> = remember {
            MutableStateSettingsProperty(
                value_state,
                { stringResource(title) },
                { null },
                getPropertyDefaultValue = { default_value }
            )
        }

        OnChangedEffect(value_state.value) {
            onChanged(value_state.value)
        }

        remember {
            AppSliderItem(
                value_property,
                range = range,
                getValueText = getValueText
            )
        }.Item(modifier)
    }

    @Composable
    protected fun DropdownItem(
        value: Int,
        value_count: Int,
        title: StringResource,
        modifier: Modifier,
        getItemName: @Composable (Int) -> String,
        onChanged: (Int) -> Unit
    ) {
        val value_state: MutableState<Int> =
            remember { mutableStateOf(value) }

        val value_property: PlatformSettingsProperty<Int> = remember {
            MutableStateSettingsProperty(
                value_state,
                { stringResource(title) },
                { null }
            )
        }

        remember {
            DropdownSettingsItem(
                value_property,
                value_count,
                getItem = getItemName
            )
        }.Item(modifier)

        OnChangedEffect(value_state.value) {
            onChanged(value_state.value)
        }
    }

    @Composable
    protected inline fun <reified T: Enum<T>> DropdownItem(
        value: T,
        title: StringResource,
        modifier: Modifier,
        noinline getItemName: @Composable (T) -> String,
        crossinline onChanged: (T) -> Unit
    ) {
        DropdownItem(
            value.ordinal,
            enumValues<T>().size,
            title,
            modifier,
            getItemName = { getItemName(enumValues<T>()[it]) },
            onChanged = { onChanged(enumValues<T>()[it]) }
        )
    }

    @Composable
    protected inline fun <reified T: Enum<T>> NullableDropdownItem(
        value: T?,
        title: StringResource,
        modifier: Modifier,
        crossinline getItemName: @Composable (T?) -> String,
        crossinline onChanged: (T?) -> Unit
    ) {
        DropdownItem(
            value?.ordinal?.plus(1) ?: 0,
            enumValues<T>().size + 1,
            title,
            modifier,
            getItemName = {
                getItemName(
                    if (it == 0) null
                    else enumValues<T>()[it - 1]
                )
            },
            onChanged = {
                onChanged(
                    if (it == 0) null
                    else enumEntries<T>()[it - 1]
                )
            }
        )
    }
}
