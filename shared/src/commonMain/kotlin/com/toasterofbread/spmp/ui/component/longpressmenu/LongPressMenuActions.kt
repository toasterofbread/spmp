package com.toasterofbread.spmp.ui.component.longpressmenu

@Composable
internal fun LongPressMenuActions(data: LongPressMenuData, accent_colour: Color, onAction: () -> Unit) {
    val accent_colour_provider = remember (accent_colour) { { accent_colour } }

    // Data-provided actions
    data.Actions(
        LongPressMenuActionProvider(
            Theme.current.on_background_provider,
            accent_colour_provider,
            Theme.current.background_provider,
            onAction
        ),
        MENU_ITEM_SPACING.dp
    )

    data.item.url?.also { url ->
        // Share
        if (SpMp.context.canShare()) {
            LongPressMenuActionProvider.ActionButton(Icons.Filled.Share, getString("lpm_action_share"), accent_colour_provider, onClick = {
                SpMp.context.shareText(url, if (data.item is Song) data.item.title else null)
            }, onAction = onAction)
        }

        // Open
        if (SpMp.context.canOpenUrl()) {
            LongPressMenuActionProvider.ActionButton(Icons.Filled.OpenWith, getString("lpm_action_open_external"), accent_colour_provider, onClick = {
                SpMp.context.openUrl(url)
            }, onAction = onAction)
        }
    }
}
