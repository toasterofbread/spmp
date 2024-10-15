package com.toasterofbread.spmp.widget.configuration

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

@Serializable
sealed interface SpMpWidgetConfiguration {
    val theme_index: Int

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onChanged: (SpMpWidgetConfiguration) -> Unit) {
        Text("Base items", item_modifier)
    }
}
