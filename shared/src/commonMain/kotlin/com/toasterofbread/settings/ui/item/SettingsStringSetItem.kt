package com.toasterofbread.composesettings.ui.item

class SettingsStringSetItem(
    val state: BasicSettingsValueState<Set<String>>,
    val title: String?,
    val subtitle: String?
): SettingsItem() {
    
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
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
        TODO()
    }
}
