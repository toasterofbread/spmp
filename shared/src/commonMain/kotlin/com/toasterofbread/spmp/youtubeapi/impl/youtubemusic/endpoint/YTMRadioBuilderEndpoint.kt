package com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.endpoint

import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylistData
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemLoader
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.youtubeapi.RadioBuilderArtist
import com.toasterofbread.spmp.youtubeapi.RadioBuilderEndpoint
import com.toasterofbread.spmp.youtubeapi.RadioBuilderModifier
import com.toasterofbread.spmp.youtubeapi.YoutubeApi
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.YoutubeMusicApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class YTMRadioBuilderEndpoint(override val api: YoutubeMusicApi): RadioBuilderEndpoint() {
    // https://gist.github.com/toasterofbread/8982ffebfca5919cb51e8967e0122982
    override suspend fun getRadioBuilderArtists(
        selectThumbnail: (List<MediaItemThumbnailProvider.Thumbnail>) -> MediaItemThumbnailProvider.Thumbnail
    ): Result<List<RadioBuilderArtist>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .endpointUrl("/youtubei/v1/browse")
            .addAuthApiHeaders()
            .postWithBody(
                mapOf("browseId" to "FEmusic_radio_builder"),
                YoutubeApi.PostBodyContext.ANDROID
            )
            .build()

        val result = api.performRequest(request)
        val parsed: RadioBuilderBrowseResponse = result.parseJsonResponse {
            return@withContext Result.failure(it)
        }

        return@withContext Result.success(parsed.items.zip(parsed.mutations).map { artist ->
            RadioBuilderArtist(artist.first.title, artist.second.token!!, selectThumbnail(artist.first.musicThumbnail.image.sources))
        })
    }

    override fun buildRadioToken(artists: Set<RadioBuilderArtist>, modifiers: Set<RadioBuilderModifier?>): String {
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

    override suspend fun getBuiltRadio(radio_token: String, context: PlatformContext): Result<RemotePlaylistData?> {
        require(radio_token.startsWith("VLRDAT"))
        require(radio_token.contains('E'))

        val playlist_result = MediaItemLoader.loadRemotePlaylist(
            RemotePlaylistData(radio_token).apply {
                playlist_type = PlaylistType.RADIO
            },
            api.context
        )
        val playlist = playlist_result.getOrNull() ?: return playlist_result

        val thumb_url = playlist.thumbnail_provider?.getThumbnailUrl(MediaItemThumbnailProvider.Quality.HIGH)
        if (thumb_url?.contains("fallback") == true) {
            return Result.success(null)
        }

        return Result.success(playlist)
    }
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
