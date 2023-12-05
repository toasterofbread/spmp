package com.toasterofbread.spmp.platform

import com.toasterofbread.composekit.platform.Platform
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

private const val MIN_PORTRAIT_RATIO: Float = 1f / 2f

enum class FormFactor {
    PORTRAIT,
    LANDSCAPE,
    DESKTOP;

    val is_large: Boolean get() = this != PORTRAIT
}

val PlayerState.form_factor: FormFactor
    get() {
        if (isPortrait()) {
            return FormFactor.PORTRAIT
        }

        return when (Platform.current) {
            Platform.ANDROID -> FormFactor.LANDSCAPE
            Platform.DESKTOP -> FormFactor.DESKTOP
        }
    }

private fun PlayerState.isPortrait(): Boolean =
    (screen_size.width / screen_size.height) <= MIN_PORTRAIT_RATIO
