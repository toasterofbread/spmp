package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable

@Serializable
internal data class LyricsSpMpWidgetConfiguration(
    override val theme_index: Int = 0,
    val furigana_mode: FuriganaMode = FuriganaMode.APP_DEFAULT
): SpMpWidgetConfiguration {
    enum class FuriganaMode {
        APP_DEFAULT,
        SHOW,
        HIDE
    }

    @Composable
    override fun ConfigurationItems(
        item_modifier: Modifier,
        onChanged: (SpMpWidgetConfiguration) -> Unit
    ) {
        super.ConfigurationItems(item_modifier, onChanged)

        val current_state: MutableState<FuriganaMode> = remember { mutableStateOf(furigana_mode) }

        OnChangedEffect(current_state.value) {
            onChanged(this.copy(furigana_mode = current_state.value))
        }

        MultipleChoiceSettingsItem(
            MutableStatePreferencesProperty(
                current_state,
                { "" },
                { null }
            )
        ) { mode ->
            when (mode) {
                FuriganaMode.APP_DEFAULT -> "App default"
                FuriganaMode.SHOW -> "Show"
                FuriganaMode.HIDE -> "Hide"
            }
        }.Item(item_modifier)
    }
}
