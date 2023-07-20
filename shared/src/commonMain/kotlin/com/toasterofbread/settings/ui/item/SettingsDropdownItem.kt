package com.toasterofbread.composesettings.ui.item

class SettingsDropdownItem(
    val state: BasicSettingsValueState<Int>,
    val title: String,
    val subtitle: String?,
    val item_count: Int,
    val getButtonItem: ((Int) -> String)? = null,
    val getItem: (Int) -> String,
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

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ItemTitleText(title, theme)
                ItemText(subtitle, theme)
            }

            var open by remember { mutableStateOf(false) }

            Button(
                { open = !open },
                Modifier.requiredHeight(40.dp),
                shape = SETTINGS_ITEM_ROUNDED_SHAPE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.vibrant_accent,
                    contentColor = theme.on_accent
                )
            ) {
                Text(getButtonItem?.invoke(state.value) ?: getItem(state.value))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    tint = theme.on_accent
                )
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = SETTINGS_ITEM_ROUNDED_SHAPE)
                ){
                    LargeDropdownMenu(
                        open,
                        { open = false },
                        item_count,
                        state.value,
                        getItem,
                        selected_item_colour = Theme.current.vibrant_accent
                    ) {
                        state.value = it
                        open = false
                    }
                }
            }
        }
    }
}
