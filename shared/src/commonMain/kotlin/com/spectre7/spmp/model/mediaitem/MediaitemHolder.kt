package com.toasterofbread.spmp.model.mediaitem

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}
