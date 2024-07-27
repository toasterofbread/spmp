package com.toasterofbread.spmp.model.mediaitem.song

import dev.toastbits.composekit.platform.lazyAssert
import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer

class SongRef(override val id: String): Song, MediaItemRef() {
    override fun toString(): String = "SongRef($id)"
    override val property_rememberer: PropertyRememberer = PropertyRememberer()
    override fun getReference(): SongRef = this
}
