package com.spectre7.spmp.api

import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.model.Playlist
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

class RadioBuilderArtist(
    val name: String,
    val token: String,
    val thumbnail: MediaItem.ThumbnailProvider.Thumbnail
)

interface RadioBuilderModifier {
    val string: String?

    enum class Variety: RadioBuilderModifier {
        LOW, MEDIUM, HIGH;
        override val string: String? get() = when (this) {
            LOW -> "rX"
            MEDIUM -> null
            HIGH -> "rZ"
        }
    }

    enum class SelectionType: RadioBuilderModifier {
        FAMILIAR, BLEND, DISCOVER;
        override val string: String? get() = when (this) {
            FAMILIAR -> "iY"
            BLEND -> null
            DISCOVER -> "iX"
        }
    }

    enum class FilterA: RadioBuilderModifier {
        POPULAR, HIDDEN, NEW;
        override val string: String? get() = when (this) {
            POPULAR -> "pY"
            HIDDEN -> "pX"
            NEW -> "dX"
        }
    }

    enum class FilterB: RadioBuilderModifier {
        PUMP_UP, CHILL, UPBEAT, DOWNBEAT, FOCUS;
        override val string: String? get() = when (this) {
            PUMP_UP -> "mY"
            CHILL -> "mX"
            UPBEAT -> "mb"
            DOWNBEAT -> "mc"
            FOCUS -> "ma"
        }
    }

    companion object {
        fun fromString(modifier: String): RadioBuilderModifier? {
            return when (modifier) {
                "iY" -> SelectionType.FAMILIAR
                "iX" -> SelectionType.DISCOVER
                "pY" -> FilterA.POPULAR
                "pX" -> FilterA.HIDDEN
                "dX" -> FilterA.NEW
                "mY" -> FilterB.PUMP_UP
                "mX" -> FilterB.CHILL
                "mb" -> FilterB.UPBEAT
                "mc" -> FilterB.DOWNBEAT
                "ma" -> FilterB.FOCUS
                "rX" -> Variety.LOW
                "rZ" -> Variety.HIGH
                else -> null
            }
        }
    }
}

fun getBuiltRadio(radio_token: String): Result<Playlist?> {
    require(radio_token.startsWith("VLRDAT"))
    require(radio_token.contains('E'))

    val playlist = Playlist.fromId(radio_token)
    val result = playlist.loadData(true)

    if (result.isFailure) {
        return result.cast()
    }

    val thumb_url = playlist.thumbnail_provider?.getThumbnail(MediaItem.ThumbnailQuality.HIGH)
    if (thumb_url?.contains("fallback") == true) {
        return Result.success(null)
    }

    return result.cast()
}

fun buildRadioToken(artists: Set<RadioBuilderArtist>, modifiers: Set<RadioBuilderModifier?>): String {
    require(artists.isNotEmpty())
    var radio_token: String = "VLRDAT"

    var modifier_added = false
    for (modifier in listOf(
        modifiers.singleOrNull { it is RadioBuilderModifier.FilterB },
        modifiers.singleOrNull { it is RadioBuilderModifier.FilterA },
        modifiers.singleOrNull { it is RadioBuilderModifier.SelectionType },
        modifiers.singleOrNull { it is RadioBuilderModifier.Variety }
    )) {
        modifier?.string?.also {
            radio_token += it
            modifier_added = true
        }
    }

    for (artist in artists.withIndex()) {
        val formatted_token = artist.value.token.removePrefix("RDAT")
            .let { token ->
                if (token.first() == 'a' && artist.index != 0) {
                    'I' + token.substring(1)
                } else token
            }
            .let { token ->
                if (artists.size == 1 && !modifier_added) {
                    token
                }
                else if (artist.index + 1 == artists.size) {
                    token.take(token.lastIndexOf('E') + 1)
                }
                else {
                    token.take(token.lastIndexOf('E'))
                }
            }

        radio_token += formatted_token
    }

    return radio_token
}

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