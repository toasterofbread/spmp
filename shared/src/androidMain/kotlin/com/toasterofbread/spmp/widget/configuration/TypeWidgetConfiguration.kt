package com.toasterofbread.spmp.widget.configuration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import kotlinx.serialization.Serializable

@Serializable
sealed interface TypeWidgetConfiguration {
    @Composable
    fun ConfigurationItems(context: AppContext, item_modifier: Modifier, onChanged: (TypeWidgetConfiguration) -> Unit)
}
