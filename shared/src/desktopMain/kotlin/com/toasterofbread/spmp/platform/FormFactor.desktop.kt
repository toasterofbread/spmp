package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

private const val MIN_PORTRAIT_RATIO: Float = 1f / 2f

actual fun PlayerState.getFormFactor(min_portrait_ratio: Float?): FormFactor =
    if ((screen_size.width / screen_size.height) <= (min_portrait_ratio ?: MIN_PORTRAIT_RATIO)) FormFactor.PORTRAIT
    else FormFactor.LANDSCAPE
