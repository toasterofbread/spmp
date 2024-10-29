package com.toasterofbread.spmp.widget.configuration.type

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.widget.action.TypeWidgetClickAction
import com.toasterofbread.spmp.widget.action.WidgetClickAction
import com.toasterofbread.spmp.widget.configuration.WidgetConfig
import com.toasterofbread.spmp.widget.configuration.enum.WidgetSectionTheme
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.DropdownSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.SliderSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.ToggleSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import dev.toastbits.composekit.utils.composable.WithStickySize
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.widget_config_common_key_click_action
import spmp.shared.generated.resources.widget_config_common_key_section_theme_opacity
import spmp.shared.generated.resources.widget_config_common_option_section_theme_mode_accent
import spmp.shared.generated.resources.widget_config_common_option_section_theme_mode_background
import spmp.shared.generated.resources.widget_config_common_option_section_theme_mode_transparent
import spmp.shared.generated.resources.widget_config_song_queue_next_songs_to_show

@Serializable
sealed class TypeWidgetConfig<A: TypeWidgetClickAction>: WidgetConfig() {
    abstract val click_action: WidgetClickAction<A>

    @Composable
    abstract fun getTypeName(): String

    fun LazyListScope.ConfigItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<A>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>) -> Unit
    ) {
        SubConfigurationItems(context, defaults_mask, item_modifier, onChanged) {
            @Suppress("UNCHECKED_CAST")
            onDefaultsMaskChanged(it as TypeConfigurationDefaultsMask<TypeWidgetConfig<A>>)
        }

        configItem(
            defaults_mask?.click_action,
            item_modifier,
            { onDefaultsMaskChanged(defaults_mask!!.setClickAction(it)) }
        ) { modifier, onItemChanged ->
            ClickActionItem(
                click_action,
                Res.string.widget_config_common_key_click_action,
                modifier
            ) {
                onChanged(setClickAction(it))
                onItemChanged()
            }
        }
    }

    protected abstract fun LazyListScope.SubConfigurationItems(
        context: AppContext,
        defaults_mask: TypeConfigurationDefaultsMask<out TypeWidgetConfig<A>>?,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfig<A>) -> Unit,
        onDefaultsMaskChanged: (TypeConfigurationDefaultsMask<out TypeWidgetConfig<A>>) -> Unit
    )

    protected abstract fun getActions(): List<A>

    protected abstract fun getActionNameResource(action: A): StringResource

    protected abstract fun setClickAction(click_action: WidgetClickAction<A>): TypeWidgetConfig<A>

    @Composable
    protected fun SectionThemeItem(
        theme: WidgetSectionTheme,
        title: StringResource,
        modifier: Modifier = Modifier,
        onChanged: (WidgetSectionTheme) -> Unit
    ) {
        var show_opacity_slider: Boolean by remember { mutableStateOf(false) }

        val mode_state: MutableState<WidgetSectionTheme.Mode> = remember { mutableStateOf(theme.mode) }
        val mode_property: PreferencesProperty<WidgetSectionTheme.Mode> = remember {
            MutableStatePreferencesProperty(
                mode_state,
                { stringResource(title) },
                { null }
            )
        }
        val mode_item: DropdownSettingsItem = remember {
            DropdownSettingsItem(
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
        val opacity_property: PreferencesProperty<Float> = remember {
            MutableStatePreferencesProperty(
                opacity_state,
                { stringResource(Res.string.widget_config_common_key_section_theme_opacity) },
                { null },
                getPropertyDefaultValue = { WidgetSectionTheme.DEFAULT_OPACITY },
                getPropertyDefaultValueComposable = { WidgetSectionTheme.DEFAULT_OPACITY }
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
    protected fun ClickActionItem(
        action: WidgetClickAction<A>,
        title: StringResource,
        modifier: Modifier = Modifier,
        onChanged: (WidgetClickAction<A>) -> Unit
    ) {
        val actions: List<WidgetClickAction<A>> = remember {
            WidgetClickAction.CommonWidgetClickAction.entries + getActions().map {
                WidgetClickAction.Type(it)
            }
        }

        val click_action_state: MutableState<Int> =
            remember { mutableIntStateOf(actions.indexOf(action)) }
        val click_action_property: PreferencesProperty<Int> = remember {
            MutableStatePreferencesProperty(
                click_action_state,
                { stringResource(title) },
                { null }
            )
        }

        remember {
            DropdownSettingsItem(
                click_action_property,
                actions.size
            ) {
                val action: WidgetClickAction<A> = actions[it]
                val resource: StringResource = when (action) {
                    is WidgetClickAction.CommonWidgetClickAction -> action.nameResource
                    is WidgetClickAction.Type -> getActionNameResource(action.actionEnum)
                }
                return@DropdownSettingsItem stringResource(resource)
            }
        }.Item(modifier)

        OnChangedEffect(click_action_state.value) {
            onChanged(actions[click_action_state.value])
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

        val state_property: PreferencesProperty<Boolean> = remember {
            MutableStatePreferencesProperty(
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
        onChanged: (T) -> Unit
    ) {
        val value_state: MutableState<T> =
            remember { mutableStateOf(value) }
        val value_property: PreferencesProperty<T> = remember {
            MutableStatePreferencesProperty(
                value_state,
                { stringResource(title) },
                { null },
                getPropertyDefaultValue = { default_value },
                getPropertyDefaultValueComposable = { default_value }
            )
        }

        OnChangedEffect(value_state.value) {
            onChanged(value_state.value)
        }

        remember {
            AppSliderItem(
                value_property,
                range = range
            )
        }.Item(modifier)
    }
}
