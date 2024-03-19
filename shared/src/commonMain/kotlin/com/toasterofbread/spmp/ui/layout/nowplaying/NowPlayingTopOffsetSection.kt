package com.toasterofbread.spmp.ui.layout.nowplaying

enum class NowPlayingTopOffsetSection {
    MULTISELECT,
    PILL_MENU,
    SEARCH_PAGE_SUGGESTIONS,
    SEARCH_PAGE_BAR,
    PAGE_BAR,
    LAYOUT_SLOT;

    fun shouldIgnoreSection(other: NowPlayingTopOffsetSection): Boolean =
        (other == this && isMerged())
        || when (this) {
            MULTISELECT -> other == SEARCH_PAGE_SUGGESTIONS
            else -> false
        }

    fun isMerged(): Boolean =
        when (this) {
            SEARCH_PAGE_SUGGESTIONS -> true
            SEARCH_PAGE_BAR -> true
            LAYOUT_SLOT -> true
            else -> false
        }
}
