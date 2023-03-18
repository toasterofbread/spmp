package com.spectre7.spmp.api

import com.spectre7.spmp.model.MediaItem
import okhttp3.Request

private class RadioBuilderBrowseResponse(
    val contents: Contents,
    val frameworkUpdates: FrameworkUpdates
) {
    val items: List<SeedItem> get() =
        contents.singleColumnBrowseResultsRenderer.tabs.first().tabRenderer.content
            .sectionListRenderer.contents.first().itemSectionRenderer.contents.first()
            .elementRenderer.newElement.type.componentType.model.musicRadioBuilderModel.seedItems
    val mutations: List<Mutation> get() =
        frameworkUpdates.entityBatchUpdate.mutations

    class Contents(val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer)
    class SingleColumnBrowseResultsRenderer(val tabs: List<Tab>)
    class Tab(val tabRenderer: TabRenderer)
    class TabRenderer(val content: Content)
    class Content(val sectionListRenderer: SectionListRenderer)
    class SectionListRenderer(val contents: List<SectionListRendererContent>)
    class SectionListRendererContent(val itemSectionRenderer: ItemSectionRenderer)
    class ItemSectionRenderer(val contents: List<ItemSectionRendererContent>)
    class ItemSectionRendererContent(val elementRenderer: ElementRenderer)
    class ElementRenderer(val newElement: NewElement)
    class NewElement(val type: Type)
    class Type(val componentType: ComponentType)
    class ComponentType(val model: Model)
    class Model(val musicRadioBuilderModel: MusicRadioBuilderModel)
    class MusicRadioBuilderModel(val seedItems: List<SeedItem>)
    class SeedItem(val itemEntityKey: String, val musicThumbnail: MusicThumbnail, val title: String)
    class MusicThumbnail(val image: Image)
    class Image(val sources: List<MediaItem.ThumbnailProvider.Thumbnail>)

    class FrameworkUpdates(val entityBatchUpdate: EntityBatchUpdate)
    class EntityBatchUpdate(val mutations: List<Mutation>)
    class Mutation(val entityKey: String, val payload: Payload) {
        val token: String? get() = payload.musicFormBooleanChoice?.opaqueToken
    }
    class Payload(val musicFormBooleanChoice: MusicFormBooleanChoice? = null)
    class MusicFormBooleanChoice(val opaqueToken: String)
}

data class RadioBuilderArtist(
    val name: String,
    private val token: String,
    val thumbnail: MediaItem.ThumbnailProvider.Thumbnail
)

// https://gist.github.com/spectreseven1138/8982ffebfca5919cb51e8967e0122982
fun getRadioBuilderArtists(
    selectThumbnail: (List<MediaItem.ThumbnailProvider.Thumbnail>) -> MediaItem.ThumbnailProvider.Thumbnail
): Result<List<RadioBuilderArtist>> {
    val request = Request.Builder()
        .url("https://music.youtube.com/youtubei/v1/browse")
        .headers(DataApi.getYTMHeaders())
        .post(DataApi.getYoutubeiRequestBody("""{ "browseId": "FEmusic_radio_builder" }""", context = DataApi.Companion.YoutubeiContextType.ANDROID))
        .build()

    val result = DataApi.request(request)
    if (!result.isSuccess) {
        return result.cast()
    }

    val body = result.getOrThrowHere().body!!
    val parsed: RadioBuilderBrowseResponse = DataApi.klaxon.parse(body.charStream())!!
    body.close()

    return Result.success(parsed.items.zip(parsed.mutations).map { artist ->
        RadioBuilderArtist(artist.first.title, artist.second.token!!, selectThumbnail(artist.first.musicThumbnail.image.sources))
    })
}