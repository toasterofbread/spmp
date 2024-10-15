package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.platform.MutableStatePreferencesProperty
import dev.toastbits.composekit.platform.PreferencesProperty
import dev.toastbits.composekit.settings.ui.component.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.utils.composable.OnChangedEffect
import kotlinx.serialization.Serializable

@Serializable
internal data class LyricsWidgetConfiguration(
    val furigana_mode: FuriganaMode = FuriganaMode.APP_DEFAULT
): TypeWidgetConfiguration {
    enum class FuriganaMode {
        APP_DEFAULT,
        SHOW,
        HIDE
    }

    @Composable
    override fun ConfigurationItems(
        context: AppContext,
        item_modifier: Modifier,
        onChanged: (TypeWidgetConfiguration) -> Unit
    ) {
        val furigana_mode_state: MutableState<FuriganaMode> = remember { mutableStateOf(furigana_mode) }
        val furigana_mode_property: PreferencesProperty<FuriganaMode> = remember {
            MutableStatePreferencesProperty(
                furigana_mode_state,
                { "" },
                { null }
            )
        }

        OnChangedEffect(furigana_mode_state.value) {
            onChanged(this.copy(furigana_mode = furigana_mode_state.value))
        }

        MultipleChoiceSettingsItem(furigana_mode_property) { mode ->
            when (mode) {
                FuriganaMode.APP_DEFAULT -> "App default"
                FuriganaMode.SHOW -> "Show"
                FuriganaMode.HIDE -> "Hide"
            }
        }.Item(item_modifier)
    }
}
