package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState

enum class FormFactor {
    PORTRAIT,
    LANDSCAPE;

    val is_large: Boolean get() = this != PORTRAIT
}

expect fun PlayerState.getFormFactor(min_portrait_ratio: Float? = null): FormFactor

val PlayerState.form_factor: FormFactor
    get() = getFormFactor()
