package com.toasterofbread.composesettings.ui.item

class SettingsSubpageItem(
    val title: String,
    val subtitle: String?,
    val target_page: Int,
): SettingsItem() {

    override fun initialiseValueStates(prefs: ProjectPreferences, default_provider: (String) -> Any) {}

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
