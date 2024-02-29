package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.*

enum class FormFactor {
    PORTRAIT,
    LANDSCAPE;

    val is_large: Boolean get() = this != PORTRAIT

    companion object {
        var form_factor_override: FormFactor? by mutableStateOf(null)
    }
}

expect fun PlayerState.getFormFactor(min_portrait_ratio: Float? = null): FormFactor

val PlayerState.form_factor: FormFactor
    get() = FormFactor.form_factor_override ?: getFormFactor()
