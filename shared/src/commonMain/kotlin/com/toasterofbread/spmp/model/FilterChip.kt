package com.toasterofbread.spmp.model

import com.toasterofbread.spmp.resources.uilocalisation.LocalisedYoutubeString

data class FilterChip(
    val text: LocalisedYoutubeString,
    val params: String
)
