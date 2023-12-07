package com.toasterofbread.spmp.platform

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

internal actual fun PlayerState.isPortrait(min_portrait_ratio: Float?): Boolean =
    context.ctx.resources.configuration.orientation == ORIENTATION_PORTRAIT
