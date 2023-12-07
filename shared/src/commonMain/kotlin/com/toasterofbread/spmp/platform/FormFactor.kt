package com.toasterofbread.spmp.platform

import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

enum class FormFactor {
    PORTRAIT,
    LANDSCAPE,
    DESKTOP;

    val is_large: Boolean get() = this != PORTRAIT
}

fun PlayerState.getFormFactor(min_portrait_ratio: Float? = null): FormFactor {
    if (isPortrait(min_portrait_ratio)) {
        return FormFactor.PORTRAIT
    }

    return when (Platform.current) {
        Platform.ANDROID -> FormFactor.LANDSCAPE
        Platform.DESKTOP -> FormFactor.DESKTOP
    }
}

val PlayerState.form_factor: FormFactor
    get() = getFormFactor()

internal expect fun PlayerState.isPortrait(min_portrait_ratio: Float? = null): Boolean
