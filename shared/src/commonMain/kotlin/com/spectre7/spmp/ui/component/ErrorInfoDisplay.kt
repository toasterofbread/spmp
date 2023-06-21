package com.spectre7.spmp.ui.component

@Composable
fun ErrorInfoDisplay(error: Throwable, modifier: Modifier = Modifier) {
    var expanded: Boolean by remember { mutableStateOf(false) }

    Column(
        modifier
            .animateContentSize()
            .background(RoundedCornerShape(16.dp), Theme.current.accent_provider)
            .clickable {
                expanded = !expanded
            }
    ) {
        val message = if (expanded) null else error.message?.let { " - $it" }
        Text(error::class.java.simpleName + (message ?: ""))

        if (expanded) {
            Text(error.stackTraceToString())
        }
    }
}
