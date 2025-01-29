package com.toasterofbread.spmp.model.mediaitem.artist

import com.toasterofbread.spmp.platform.AppContext
import dev.toastbits.composekit.util.isJa

fun formatArtistTitles(titles: List<String?>, context: AppContext): String? {
    val filtered_titles: List<String> = titles.filterNotNull()
    if (filtered_titles.isEmpty()) {
        return null
    }

    val separator: String
    if (filtered_titles.any { title -> title.any { char -> char.isJa() } }) {
        separator = "、"
    }
    else {
        separator = ", "
    }

    return filtered_titles.joinToString(separator)
}
