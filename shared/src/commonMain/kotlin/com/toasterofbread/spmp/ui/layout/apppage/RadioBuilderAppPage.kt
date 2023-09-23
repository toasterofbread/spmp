package com.toasterofbread.spmp.ui.layout.apppage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.radiobuilder.RadioBuilderPage

class RadioBuilderAppPage(override val state: AppPageState): AppPage() {
    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        RadioBuilderPage(
            content_padding,
            Modifier.fillMaxSize(),
            close
        )
    }
}
