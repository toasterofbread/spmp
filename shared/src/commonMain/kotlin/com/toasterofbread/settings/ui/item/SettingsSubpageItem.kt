package com.toasterofbread.composesettings.ui.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.toasterofbread.composesettings.ui.SettingsPage
import com.toasterofbread.spmp.platform.ProjectPreferences
import com.toasterofbread.spmp.ui.theme.Theme

class SettingsSubpageItem(
    val title: String,
    val subtitle: String?,
    val target_page: Int,
): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun releaseValueStates(prefs: ProjectPreferences) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        Button(
            { openPage(target_page) },
            Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.vibrant_accent,
                contentColor = theme.on_accent
            )
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = theme.on_accent)
                ItemText(subtitle, theme.on_accent)
            }
        }
    }
}
