package com.toasterofbread.spmp.api

import com.toasterofbread.spmp.api.Api.Companion.addYtHeaders
import com.toasterofbread.spmp.api.Api.Companion.getStream
import com.toasterofbread.spmp.api.Api.Companion.ytUrl
import com.toasterofbread.spmp.model.mediaitem.MediaItemLoader
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.resources.getString
import okhttp3.Request

suspend fun getBuiltRadio(radio_token: String): Result<Playlist?> {
    require(radio_token.startsWith("VLRDAT"))
    require(radio_token.contains('E'))

    val playlist_result = MediaItemLoader.loadPlaylist(
        PlaylistData(radio_token, playlist_type = PlaylistType.RADIO)
    )
    val playlist = playlist_result.getOrNull() ?: return playlist_result

    val thumb_url = playlist.thumbnail_provider?.getThumbnailUrl(MediaItemThumbnailProvider.Quality.HIGH)
    if (thumb_url?.contains("fallback") == true) {
        return Result.success(null)
    }

    return Result.success(playlist)
}

fun buildRadioToken(artists: Set<RadioBuilderArtist>, modifiers: Set<RadioModifier?>): String {
    require(artists.isNotEmpty())
    var radio_token: String = "VLRDAT"

    var modifier_added = false
    for (modifier in listOf(
        modifiers.singleOrNull { it is RadioModifier.FilterB },
        modifiers.singleOrNull { it is RadioModifier.FilterA },
        modifiers.singleOrNull { it is RadioModifier.SelectionType },
        modifiers.singleOrNull { it is RadioModifier.Variety }
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

// https://gist.github.com/toasterofbread/8982ffebfca5919cb51e8967e0122982
fun getRadioBuilderArtists(
    selectThumbnail: (List<MediaItemThumbnailProvider.Thumbnail>) -> MediaItemThumbnailProvider.Thumbnail
): Result<List<RadioBuilderArtist>> {
    val request = Request.Builder()
        .ytUrl("/youtubei/v1/browse")
        .addYtHeaders()
        .post(Api.getYoutubeiRequestBody(mapOf("browseId" to "FEmusic_radio_builder"), context = Api.Companion.YoutubeiContextType.ANDROID))
        .build()

    val result = Api.request(request)
    if (!result.isSuccess) {
        return result.cast()
    }

    val strean = result.getOrThrow().getStream()
    val parsed: RadioBuilderBrowseResponse = Api.klaxon.parse(strean)!!
    strean.close()

    return Result.success(parsed.items.zip(parsed.mutations).map { artist ->
        RadioBuilderArtist(artist.first.title, artist.second.token!!, selectThumbnail(artist.first.musicThumbnail.image.sources))
    })
}

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
    class Image(val sources: List<MediaItemThumbnailProvider.Thumbnail>)

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
    val thumbnail: MediaItemThumbnailProvider.Thumbnail
)

interface RadioModifier {
    val string: String?
    fun getReadable(): String

    enum class Variety: RadioModifier {
        LOW, MEDIUM, HIGH;
        override val string: String? get() = when (this) {
            LOW -> "rX"
            MEDIUM -> null
            HIGH -> "rZ"
        }
        override fun getReadable(): String = getString(when (this) {
            LOW -> "radio_builder_modifier_variety_low"
            MEDIUM -> "radio_builder_modifier_variety_medium"
            HIGH -> "radio_builder_modifier_variety_high"
        })
    }

    enum class SelectionType: RadioModifier {
        FAMILIAR, BLEND, DISCOVER;
        override val string: String? get() = when (this) {
            FAMILIAR -> "iY"
            BLEND -> null
            DISCOVER -> "iX"
        }
        override fun getReadable(): String = getString(when (this) {
            FAMILIAR -> "radio_builder_modifier_selection_type_familiar"
            BLEND -> "radio_builder_modifier_selection_type_blend"
            DISCOVER -> "radio_builder_modifier_selection_type_discover"
        })
    }

    enum class FilterA: RadioModifier {
        POPULAR, HIDDEN, NEW;
        override val string: String? get() = when (this) {
            POPULAR -> "pY"
            HIDDEN -> "pX"
            NEW -> "dX"
        }
        override fun getReadable(): String = getString(when (this) {
            POPULAR -> "radio_builder_modifier_filter_a_popular"
            HIDDEN -> "radio_builder_modifier_filter_a_hidden"
            NEW -> "radio_builder_modifier_filter_a_new"
        })
    }

    enum class FilterB: RadioModifier {
        PUMP_UP, CHILL, UPBEAT, DOWNBEAT, FOCUS;
        override val string: String? get() = when (this) {
            PUMP_UP -> "mY"
            CHILL -> "mX"
            UPBEAT -> "mb"
            DOWNBEAT -> "mc"
            FOCUS -> "ma"
        }
        override fun getReadable(): String = getString(when (this) {
            PUMP_UP -> "radio_builder_modifier_filter_pump_up" // 熱
            CHILL -> "radio_builder_modifier_filter_chill" // 冷
            UPBEAT -> "radio_builder_modifier_filter_upbeat" // 明るい
            DOWNBEAT -> "radio_builder_modifier_filter_downbeat" // 重い
            FOCUS -> "radio_builder_modifier_filter_focus"
        })
    }

    companion object {
        fun fromString(modifier: String): RadioModifier? {
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
