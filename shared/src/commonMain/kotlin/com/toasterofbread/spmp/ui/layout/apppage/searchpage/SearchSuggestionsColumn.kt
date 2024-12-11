package com.toasterofbread.spmp.ui.layout.apppage.searchpage

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import dev.toastbits.composekit.util.composable.AlignableCrossfade
import dev.toastbits.ytmkt.endpoint.SearchSuggestion

@Composable
internal fun SearchAppPage.SearchSuggestionsColumn(
    suggestions: List<SearchSuggestion>,
    alignment: Alignment.Vertical,
    modifier: Modifier = Modifier
) {
    var current_suggestions: List<SearchSuggestion> by remember { mutableStateOf(suggestions) }
    LaunchedEffect(suggestions) {
        if (suggestions.isNotEmpty()) {
            current_suggestions = suggestions
        }
    }

    AlignableCrossfade(
        current_suggestions,
        modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp, alignment)
        ) {
            for (suggestion in it) {
                SearchSuggestion(suggestion) {
                    if (!search_in_progress) {
                        current_query = suggestion.text
                        current_suggestions = emptyList()
                        performSearch()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAppPage.SearchSuggestion(
    suggestion: SearchSuggestion,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit,
) {
    Row(
        modifier
            .clickable(onClick = onSelected)
            .clip(SEARCH_BAR_SHAPE)
            .background(context.theme.background)
            .border(2.dp, context.theme.accent, SEARCH_BAR_SHAPE)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            suggestion.text,
            Modifier.weight(1f, false),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = context.theme.onBackground
        )

        if (suggestion.is_from_history) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.alpha(0.75f),
                tint = context.theme.onBackground
            )
        }
    }
}
