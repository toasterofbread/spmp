package com.toasterofbread.composesettings.ui.item

// TODO Styling
class SettingsTextFieldItem(
    val state: BasicSettingsValueState<String>,
    val title: String?,
    val subtitle: String?,
    val single_line: Boolean = true
): SettingsItem() {
    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {
        state.init(prefs, default_provider)
    }

    override fun resetValues() {
        state.reset()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetItem(theme: Theme, openPage: (Int) -> Unit, openCustomPage: (SettingsPage) -> Unit) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ItemTitleText(title, theme)
            ItemText(subtitle, theme)
            TextField(state.value, { state.value = it }, Modifier.fillMaxWidth(), singleLine = single_line)
        }
    }
}
