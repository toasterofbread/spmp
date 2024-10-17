package com.toasterofbread.spmp.widget.configuration

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.widget.SpMpWidgetType
import kotlinx.serialization.Serializable

@Serializable
sealed interface TypeWidgetConfiguration {
    @Composable
    fun getName(): String

    fun LazyListScope.ConfigurationItems(context: AppContext, item_modifier: Modifier, onChanged: (TypeWidgetConfiguration) -> Unit)
}
