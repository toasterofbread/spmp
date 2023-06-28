package com.spectre7.spmp.ui.layout.artistpage

@Composable
fun DescriptionCard(description_text: String, backgroundColourProvider: () -> Color, accentColourProvider: () -> Color?, toggleInfo: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var can_expand by remember { mutableStateOf(false) }
    val small_text_height = 200.dp
    val small_text_height_px = with ( LocalDensity.current ) { small_text_height.toPx().toInt() }

    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Theme.current.on_background.setAlpha(0.05f)
        )
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AssistChip(
                    toggleInfo,
                    {
                        Text(getString("artist_info_label"), style = MaterialTheme.typography.labelLarge)
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, null)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = backgroundColourProvider(),
                        labelColor = Theme.current.on_background,
                        leadingIconContentColor = accentColourProvider() ?: Color.Unspecified
                    )
                )

                if (can_expand) {
                    NoRipple {
                        IconButton(
                            { expanded = !expanded }
                        ) {
                            Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,null)
                        }
                    }
                }
            }

            LinkifyText(
                description_text,
                Modifier
                    .onSizeChanged { size ->
                        if (size.height == small_text_height_px) {
                            can_expand = true
                        }
                    }
                    .animateContentSize()
                    .then(
                        if (expanded) Modifier else Modifier.height(200.dp)
                    ),
                Theme.current.on_background.setAlpha(0.8f),
                Theme.current.on_background,
                MaterialTheme.typography.bodyMedium
            )
        }
    }
}
