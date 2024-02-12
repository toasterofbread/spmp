package com.toasterofbread.spmp.platform

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.toasterofbread.spmp.service.playercontroller.PlayerState

actual fun PlayerState.getFormFactor(min_portrait_ratio: Float?): FormFactor =
    if (context.ctx.resources.configuration.orientation == ORIENTATION_PORTRAIT) FormFactor.PORTRAIT
    else FormFactor.LANDSCAPE
