package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.AppSliderItem
import com.toasterofbread.spmp.widget.SpMpWidget
import com.toasterofbread.spmp.widget.SpMpWidgetType
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settings.ui.component.item.SliderSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable

@Serializable
internal data class LyricsWidgetConfiguration(
    val furigana_mode: FuriganaMode = FuriganaMode.APP_DEFAULT,
    val font_size: Float = 1f
): TypeWidgetConfiguration {
    @Composable
    override fun getName(): String = "LYRICS"

    enum class FuriganaMode {
        APP_DEFAULT,
        SHOW,
        HIDE
    }

    override fun LazyListScope.ConfigurationItems(
        context: AppContext,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfiguration) -> Unit
    ) {
        item {
            FuriganaModeItem(context, item_modifier, onChanged)
        }
        item {
            FontSizeItem(context, item_modifier, onChanged)
        }
    }

    @Composable
    private fun FuriganaModeItem(context: AppContext, modifier: Modifier, onChanged: (TypeWidgetConfiguration) -> Unit) {
        val furigana_mode_state: MutableState<FuriganaMode> = remember { mutableStateOf(furigana_mode) }
        val furigana_mode_property: PreferencesProperty<FuriganaMode> = remember {
            MutableStatePreferencesProperty(
                furigana_mode_state,
                { "FURIGANA MODE" },
                { null }
            )
        }

        OnChangedEffect(furigana_mode_state.value) {
            onChanged(this.copy(furigana_mode = furigana_mode_state.value))
        }

        remember {
            MultipleChoiceSettingsItem(furigana_mode_property) { mode ->
                when (mode) {
                    FuriganaMode.APP_DEFAULT -> "App default"
                    FuriganaMode.SHOW -> "Show"
                    FuriganaMode.HIDE -> "Hide"
                }
            }
        }.Item(modifier)
    }

    @Composable
    private fun FontSizeItem(context: AppContext, modifier: Modifier, onChanged: (TypeWidgetConfiguration) -> Unit) {
        val font_size_state: MutableState<Float> = remember { mutableFloatStateOf(font_size) }
        val font_size_property: PreferencesProperty<Float> = remember {
            MutableStatePreferencesProperty(
                font_size_state,
                { "FONT SIZE" },
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
}
