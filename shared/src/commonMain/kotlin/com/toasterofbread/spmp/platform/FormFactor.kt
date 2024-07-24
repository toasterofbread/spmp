package com.toasterofbread.spmp.platform

import com.toasterofbread.spmp.model.state.OldPlayerStateImpl
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.unit.dp
import LocalPlayerState
import com.toasterofbread.spmp.model.state.UiState

enum class FormFactor {
    PORTRAIT,
    LANDSCAPE;

    val is_large: Boolean get() = this != PORTRAIT

    companion object {
        private var form_factor_override: FormFactor? by mutableStateOf(null)

        fun setOverride(form_factor_override: FormFactor?) {
            this.form_factor_override = form_factor_override
        }

        @Composable
        fun observe(min_portrait_ratio: Float? = null): State<FormFactor> {
            val player: OldPlayerStateImpl = LocalPlayerState.current
            return remember { derivedStateOf {
                getCurrent(player)
            } }
        }

        fun getCurrent(ui_state: UiState): FormFactor {
            form_factor_override?.also {
                return it
            }

            return (
                if (ui_state.screen_size.width < 500.dp) PORTRAIT
                else LANDSCAPE
            )
        }
    }
}
