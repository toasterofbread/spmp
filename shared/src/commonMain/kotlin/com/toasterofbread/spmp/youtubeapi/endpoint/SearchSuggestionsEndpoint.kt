package com.toasterofbread.spmp.youtubeapi.endpoint

import com.toasterofbread.spmp.youtubeapi.YoutubeApi

abstract class SearchSuggestionsEndpoint: YoutubeApi.Endpoint() {
    abstract suspend fun getSearchSuggestions(query: String): Result<List<String>>
}
