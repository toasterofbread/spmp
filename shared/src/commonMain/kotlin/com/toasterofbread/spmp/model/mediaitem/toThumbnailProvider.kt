package com.toasterofbread.spmp.model.mediaitem

import com.toasterofbread.spmp.db.mediaitem.ThumbnailProviderById
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.model.external.ThumbnailProviderImpl

fun ThumbnailProviderById.toThumbnailProvider(): ThumbnailProvider? =
    if (thumb_url_a == null) null
    else ThumbnailProviderImpl(thumb_url_a, thumb_url_b)
