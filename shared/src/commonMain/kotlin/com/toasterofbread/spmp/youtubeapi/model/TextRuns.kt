package com.toasterofbread.spmp.youtubeapi.model

import com.google.gson.annotations.SerializedName

data class TextRuns(
    @SerializedName("runs")
    val _runs: List<TextRun>? = null
) {
    val runs: List<TextRun>? get() = _runs?.filter { it.text != " \u2022 " }
    val first_text: String get() = runs!![0].text

    fun firstTextOrNull(): String? = runs?.getOrNull(0)?.text
}

data class TextRun(val text: String, val strapline: TextRuns? = null, val navigationEndpoint: NavigationEndpoint? = null) {
    val browse_endpoint_type: String? get() = navigationEndpoint?.browseEndpoint?.getPageType()
}
