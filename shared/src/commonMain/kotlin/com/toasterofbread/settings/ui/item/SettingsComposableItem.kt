package com.toasterofbread.composesettings.ui.item

class SettingsComposableItem(val composable: @Composable () -> Unit): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}
    override fun resetValues() {}

    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        composable()
    }
}
