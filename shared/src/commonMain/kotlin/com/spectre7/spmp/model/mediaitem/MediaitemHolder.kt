package com.spectre7.spmp.model.mediaitem

interface MediaItemHolder {
    // If item is null, consider it deleted
    val item: MediaItem?
}
