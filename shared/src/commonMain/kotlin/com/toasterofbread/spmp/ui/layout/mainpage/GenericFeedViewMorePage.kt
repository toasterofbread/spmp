package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.ui.layout.GenericFeedViewMorePage

data class GenericFeedViewMorePage(private val browse_id: String, private val title: String?): OverlayPage {
    @Composable
    override fun Page(previous_item: MediaItem?, close: () -> Unit) {
        GenericFeedViewMorePage(browse_id, Modifier.fillMaxSize(), content_padding = getContentPadding(), title = title)
    }
}