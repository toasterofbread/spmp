package com.toasterofbread.spmp.model.mediaitem.song

import com.toasterofbread.spmp.model.mediaitem.MediaItemRef
import com.toasterofbread.spmp.model.mediaitem.PropertyRememberer
import com.toasterofbread.utils.common.lazyAssert

class SongRef(override val id: String): Song, MediaItemRef() {
    override fun toString(): String = "SongRef($id)"
    override val property_rememberer: PropertyRememberer = PropertyRememberer()
}
