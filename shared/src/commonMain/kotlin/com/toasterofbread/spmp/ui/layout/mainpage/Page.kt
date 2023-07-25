package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.SearchPage
import com.toasterofbread.spmp.ui.layout.library.LibraryMainPage

abstract class MainPage {
    @Composable
    abstract fun Page(
        multiselect_context: MediaItemMultiSelectContext,
        modifier: Modifier,
        content_padding: PaddingValues,
        close: () -> Unit
    )

    open fun onOpened() {}

    companion object {
        val Search = SearchPage()
        val Library = LibraryMainPage()
    }
}
