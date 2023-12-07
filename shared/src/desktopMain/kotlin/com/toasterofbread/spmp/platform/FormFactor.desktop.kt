package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

private const val MIN_PORTRAIT_RATIO: Float = 1f / 2f

internal actual fun PlayerState.isPortrait(min_portrait_ratio: Float?): Boolean =
    (screen_size.width / screen_size.height) <= (min_portrait_ratio ?: MIN_PORTRAIT_RATIO)
