package com.toasterofbread.composesettings.ui.item

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.composable.RecomposeOnInterval
import com.toasterofbread.utils.setAlpha

class SettingsToggleItem(
    val state: BasicSettingsValueState<Boolean>,
    val title: String?,
    val subtitle: String?,
    val checker: ((target: Boolean, setLoading: (Boolean) -> Unit, (allow_change: Boolean) -> Unit) -> Unit)? = null
): SettingsItem() {
    private var loading: Boolean by mutableStateOf(false)

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun releaseValueStates(prefs: ProjectPreferences) {
        state.release(prefs)
    }

    override fun resetValues() {
        state.reset()
    }

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme)
            }

            Crossfade(loading) {
                if (it) {
                    CircularProgressIndicator(color = theme.on_background)
                }
                else {
                    Switch(
                        state.get(),
                        onCheckedChange = null,
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (checker == null) {
                                state.set(!state.get())
                                return@clickable
                            }

                            checker.invoke(
                                !state.get(),
                                { l ->
                                    loading = l
                                }
                            ) { allow_change ->
                                if (allow_change) {
                                    state.set(!state.get())
                                }
                                loading = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.vibrant_accent,
                            checkedTrackColor = theme.vibrant_accent.setAlpha(0.5f)
                        )
                    )
                }
            }
        }
    }
}
