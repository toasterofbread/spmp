package com.toasterofbread.spmp.api.model

data class Header(
    val musicCarouselShelfBasicHeaderRenderer: HeaderRenderer? = null,
    val musicImmersiveHeaderRenderer: HeaderRenderer? = null,
    val musicVisualHeaderRenderer: HeaderRenderer? = null,
    val musicDetailHeaderRenderer: HeaderRenderer? = null,
    val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer? = null,
    val musicCardShelfHeaderBasicRenderer: HeaderRenderer? = null
): YoutubeiHeader {
    fun getRenderer(): HeaderRenderer? {
        return musicCarouselShelfBasicHeaderRenderer
            ?: musicImmersiveHeaderRenderer
            ?: musicVisualHeaderRenderer
            ?: musicDetailHeaderRenderer
            ?: musicCardShelfHeaderBasicRenderer
            ?: musicEditablePlaylistDetailHeaderRenderer?.header?.getRenderer()
    }

    data class MusicEditablePlaylistDetailHeaderRenderer(val header: Header)

    override val header_renderer: HeaderRenderer?
        get() = getRenderer()
}

//val thumbnails = (header.obj("thumbnail") ?: header.obj("foregroundThumbnail")!!)
//    .obj("musicThumbnailRenderer")!!
//    .obj("thumbnail")!!
//    .array<JsonObject>("thumbnails")!!

interface YoutubeiHeaderContainer {
    val header: YoutubeiHeader?
}
interface YoutubeiHeader {
    val header_renderer: HeaderRenderer?
}
