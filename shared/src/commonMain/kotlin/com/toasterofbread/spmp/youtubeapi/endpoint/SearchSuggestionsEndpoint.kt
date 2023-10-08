package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.youtubeapi.YoutubeApi

data class SearchSuggestion(
    val text: String,
    val is_from_history: Boolean
)

abstract class SearchSuggestionsEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getSearchSuggestions(query: String): Result<List<SearchSuggestion>>
}
