package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface BaseSpMpWidgetConfiguration {
    val theme_index: Int

    @Composable
    fun ConfigurationItems(item_modifier: Modifier, onChanged: (BaseSpMpWidgetConfiguration) -> Unit)
}
