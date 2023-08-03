package com.toasterofbread.spmp.api.model

data class GridRenderer(val items: List<YoutubeiShelfContentsItem>, override val header: GridHeader? = null): YoutubeiHeaderContainer
data class GridHeader(val gridHeaderRenderer: HeaderRenderer): YoutubeiHeader {
    override val header_renderer: HeaderRenderer?
        get() = gridHeaderRenderer
}
