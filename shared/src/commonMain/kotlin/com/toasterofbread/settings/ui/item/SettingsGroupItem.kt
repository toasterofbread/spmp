package com.toasterofbread.composesettings.ui.item

class SettingsGroupItem(var title: String?): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(
        theme: Theme,
        openPage: (Int) -> Unit,
        openCustomPage: (SettingsPage) -> Unit
    ) {
        title?.also {
            Text(it, color = theme.vibrant_accent, fontSize = 20.sp, fontWeight = FontWeight.Light)
        }
    }
}
