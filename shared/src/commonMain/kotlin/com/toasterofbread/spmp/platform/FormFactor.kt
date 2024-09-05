package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.service.playercontroller.PlayerState
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.unit.dp
import LocalPlayerState
import androidx.compose.ui.unit.DpSize

enum class FormFactor {
    PORTRAIT,
    LANDSCAPE;

    val is_large: Boolean get() = this != PORTRAIT

    companion object {
        private var form_factor_override: FormFactor? by mutableStateOf(null)

        fun setOverride(form_factor_override: FormFactor?) {
            this.form_factor_override = form_factor_override
        }

        fun getCurrent(screen_size: DpSize): FormFactor {
            form_factor_override?.also {
                return it
            }

            if (screen_size.width >= 500.dp) {
                return FormFactor.LANDSCAPE
            }

            return (
                if (screen_size.width > screen_size.height) FormFactor.LANDSCAPE
                else FormFactor.PORTRAIT
            )
        }

        fun getCurrent(player: PlayerState): FormFactor = getCurrent(player.screen_size)

        @Composable
        fun observe(min_portrait_ratio: Float? = null): State<FormFactor> {
            val screen_size: DpSize by LocalPlayerState.current.screen_size_state
            return remember { derivedStateOf {
                form_factor_override ?: getCurrent(screen_size)
            } }
        }
    }
}
