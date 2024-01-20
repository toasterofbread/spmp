package com.toasterofbread.spmp.youtubeapi.model

import com.toasterofbread.spmp.youtubeapi.radio.YoutubeiNextResponse

data class Header(
    val musicCarouselShelfBasicHeaderRenderer: HeaderRenderer?,
    val musicImmersiveHeaderRenderer: HeaderRenderer?,
    val musicVisualHeaderRenderer: HeaderRenderer?,
    val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
    val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer?,
    val musicCardShelfHeaderBasicRenderer: HeaderRenderer?
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
    class MusicDetailHeaderRenderer(
        val menu: Menu,
        title: TextRuns? = null,
        strapline: TextRuns? = null,
        subscriptionButton: SubscriptionButton? = null,
        playButton: MoreContentButton? = null,
        description: TextRuns? = null,
        thumbnail: Thumbnails? = null,
        foregroundThumbnail: Thumbnails? = null,
        subtitle: TextRuns? = null,
        secondSubtitle: TextRuns? = null,
        moreContentButton: MoreContentButton? = null
    ): HeaderRenderer(
        title,
        strapline,
        subscriptionButton,
        playButton,
        description,
        thumbnail,
        foregroundThumbnail,
        subtitle,
        secondSubtitle,
        moreContentButton
    )
    data class Menu(val menuRenderer: MenuRenderer)
    data class MenuRenderer(val topLevelButtons: List<TopLevelButton>? = null)
    data class TopLevelButton(val buttonRenderer: TopLevelButtonRenderer?)
    data class TopLevelButtonRenderer(val icon: YoutubeiNextResponse.MenuIcon? = null)

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
