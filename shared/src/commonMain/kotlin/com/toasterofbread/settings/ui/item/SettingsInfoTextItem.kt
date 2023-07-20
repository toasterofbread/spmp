package com.toasterofbread.composesettings.ui.item

class SettingsItemInfoText(val text: String): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        Text(text)
    }
}
