package com.spectre7.spmp.ui.component

@Composable
fun MediaItemTitleEditDialog(item: MediaItem, show: Boolean, modifier: Modifier = Modifier) {
    if (show) {
        var edited_title: String by remember { mutableStateOf(data.item.title ?: "") }
        PlatformAlertDialog(
            { show_title_edit_dialog = false },
            { Button({
                item.editRegistry {
                    it.title = edited_title
                }
                show_title_edit_dialog = false
            }) {
                Text(getString("action_confirm_action"))
            } },
            dismissButton = {
                Button({ show_title_edit_dialog = false }) {
                    Text(getString("action_cancel"))
                }
            },
            title = {
                Text(getString("edit_\$x_title_dialog_title").replace("\$x", item.type.getReadable(false)))
            },
            text = {
                TextField(
                    edited_title,
                    { edited_title = it }
                )
            }
        )
    }
}
