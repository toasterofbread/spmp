package com.toasterofbread.spmp.ui.component.mediaitempreview

@Composable
fun MediaItemPreviewSquare(
    item: MediaItem,
    params: MediaItemPreviewParams,
    long_press_menu_data: LongPressMenuData
) {
    Column(
        params.modifier.mediaItemPreviewInteraction(item, long_press_menu_data),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            item.Thumbnail(
                MediaItemThumbnailProvider.Quality.LOW,
                Modifier.longPressMenuIcon(long_press_menu_data, params.enable_long_press_menu).aspectRatio(1f),
                contentColourProvider = params.contentColour
            )

            params.multiselect_context?.also { ctx ->
                ctx.SelectableItemOverlay(item, Modifier.fillMaxSize(), key = queue_index)
            }
        }

        Text(
            item.title ?: "",
            fontSize = 12.sp,
            color = params.contentColour?.invoke() ?: Color.Unspecified,
            maxLines = params.square_item_max_text_rows ?: 1,
            lineHeight = 14.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}
