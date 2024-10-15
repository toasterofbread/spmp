package com.toasterofbread.spmp.widget.configuration

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.toastbits.composekit.settings.ui.item.MultipleChoiceSettingsItem
import dev.toastbits.composekit.settings.ui.item.ToggleSettingsItem
import kotlinx.serialization.Serializable

@Serializable
internal data class LyricsSpMpWidgetConfiguration(
    override val theme_index: Int = 0,
    val show_furigana: Boolean? = null
): BaseSpMpWidgetConfiguration {
    @Composable
    override fun ConfigurationItems(
        item_modifier: Modifier,
        onChanged: (BaseSpMpWidgetConfiguration) -> Unit
    ) {
        MultipleChoiceSettingsItem()

        Switch(
            show_furigana,
            onCheckedChange = {
                onChanged(this.copy(show_furigana = it))
            },
            modifier = item_modifier
        )
    }
}
