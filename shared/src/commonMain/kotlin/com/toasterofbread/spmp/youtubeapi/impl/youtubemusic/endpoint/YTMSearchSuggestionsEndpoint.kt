package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.youtubeapi.endpoint.SearchSuggestion
import com.toasterofbread.spmp.youtubeapi.endpoint.SearchSuggestionsEndpoint
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMSearchSuggestionsEndpoint(override val api: YoutubeMusicApi): SearchSuggestionsEndpoint() {
    override suspend fun getSearchSuggestions(query: String): Result<List<SearchSuggestion>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/music/get_search_suggestions")
            .addAuthApiHeaders()
            .postWithBody(mapOf("input" to query))
            .build()

        val result = api.performRequest(request)
        val parsed: YoutubeiSearchSuggestionsResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        val suggestions = parsed.getSuggestions()
        if (suggestions == null) {
            return@withContext Result.failure(NullPointerException("Suggestions is null ($parsed)"))
        }

        return@withContext Result.success(suggestions)
    }
}

private data class YoutubeiSearchSuggestionsResponse(
    val contents: List<Content>?
) {
    fun getSuggestions(): List<SearchSuggestion>? =
        contents?.firstOrNull()
            ?.searchSuggestionsSectionRenderer
            ?.contents
            ?.mapNotNull { suggestion ->
                if (suggestion.searchSuggestionRenderer != null) {
                    return@mapNotNull SearchSuggestion(suggestion.searchSuggestionRenderer.navigationEndpoint.searchEndpoint.query, false)
                }
                else if (suggestion.historySuggestionRenderer != null) {
                    return@mapNotNull SearchSuggestion(suggestion.historySuggestionRenderer.navigationEndpoint.searchEndpoint.query, true)
                }
                return@mapNotNull null
            }

    data class Content(val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer?)
    data class SearchSuggestionsSectionRenderer(val contents: List<Suggestion>)
    data class Suggestion(val searchSuggestionRenderer: SearchSuggestionRenderer?, val historySuggestionRenderer: SearchSuggestionRenderer?)
    data class SearchSuggestionRenderer(val navigationEndpoint: NavigationEndpoint)
    data class NavigationEndpoint(val searchEndpoint: SearchEndpoint)
    data class SearchEndpoint(val query: String)
}
