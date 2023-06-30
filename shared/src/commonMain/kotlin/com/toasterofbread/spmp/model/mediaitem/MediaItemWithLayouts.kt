package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.ui.component.MediaItemLayout

interface MediaItemWithLayoutsData {
    fun supplyFeedLayouts(value: List<MediaItemLayout>?, certain: Boolean, cached: Boolean = false)
}

interface MediaItemWithLayouts {
    @get:Composable
    val feed_layouts: List<MediaItemLayout>?
    suspend fun getFeedLayouts(): Result<List<MediaItemLayout>>
}
