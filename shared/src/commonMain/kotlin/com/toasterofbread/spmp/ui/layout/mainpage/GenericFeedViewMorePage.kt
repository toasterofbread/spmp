package com.toasterofbread.spmp.ui.layout.mainpage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.toasterofbread.spmp.model.mediaitem.MediaItemHolder
import com.toasterofbread.spmp.ui.layout.GenericFeedViewMorePage

data class GenericFeedViewMorePage(private val browse_id: String, private val title: String?): PlayerOverlayPage {
    @Composable
    override fun Page(previous_item: MediaItemHolder?, bottom_padding: Dp, close: () -> Unit) {
        GenericFeedViewMorePage(browse_id, Modifier.fillMaxSize(), bottom_padding = bottom_padding, title = title)
    }
}