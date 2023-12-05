package com.toasterofbread.spmp.ui.layout.apppage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext

class NotificationsAppPage(override val state: AppPageState): AppPage() {
    @Composable
    override fun ColumnScope.Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit,
    ) {
        Text("TODO")
    }
}
