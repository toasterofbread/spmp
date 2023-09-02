package com.toasterofbread.spmp.service.playerservice

import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.Settings
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.toThumbnailProvider
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.resources.getStringTODO
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrThrowHere
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val DISCORD_APPLICATION_ID = "1081929293979992134"
private const val DISCORD_ASSET_ICON_PRIMARY = "1103702923852132372"

internal class DiscordStatusHandler(val player: PlayerService, val context: PlatformContext) {
    private var discord_rpc: DiscordStatus? = null
    private var discord_status_update_job: Job? = null
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)

    fun release() {
        coroutine_scope.cancel()
        discord_rpc?.close()
    }

    fun onDiscordAccountTokenChanged() {
        discord_rpc?.close()

        val account_token = Settings.KEY_DISCORD_ACCOUNT_TOKEN.get<String>(context.getPrefs())
        if (!DiscordStatus.isSupported() || (account_token.isBlank() && DiscordStatus.isAccountTokenRequired())) {
            discord_rpc = null
            return
        }

        discord_rpc = DiscordStatus(
            bot_token = ProjectBuildConfig.DISCORD_BOT_TOKEN,
            custom_images_channel_category_id = ProjectBuildConfig.DISCORD_CUSTOM_IMAGES_CHANNEL_CATEGORY,
            custom_images_channel_name_prefix = ProjectBuildConfig.DISCORD_CUSTOM_IMAGES_CHANNEL_NAME_PREFIX ?: "",
            account_token = account_token
        )

        updateDiscordStatus(null)
    }

    @Synchronized
    fun updateDiscordStatus(song: Song?): Unit = with(context.database) {
        discord_status_update_job?.cancel()
        discord_rpc?.apply {
            val status_song = song ?: player.getSong()
            if (status_song == null) {
                close()
                return
            }

            val song_title = status_song.Title.get(context.database)
            if (song_title == null) {
                close()
                return
            }

            discord_status_update_job = coroutine_scope.launch {
                fun formatText(text: String): String {
                    val artist_id = songQueries.artistById(status_song.id).executeAsOne().artist
                    val artist_title = artist_id?.let {
                        mediaItemQueries.titleById(it).executeAsOne().title
                    }

                    return text
                        .replace("\$artist", artist_title ?: getStringTODO("Unknown"))
                        .replace("\$song", song_title)
                }

                val name = formatText(Settings.KEY_DISCORD_STATUS_NAME.get())
                val text_a = formatText(Settings.KEY_DISCORD_STATUS_TEXT_A.get())
                val text_b = formatText(Settings.KEY_DISCORD_STATUS_TEXT_B.get())
                val text_c = formatText(Settings.KEY_DISCORD_STATUS_TEXT_C.get())

                val large_image: String?
                val small_image: String?

                try {
                    large_image = getCustomImage(status_song.id) {
                        val thumbnail_provider = mediaItemQueries.thumbnailProviderById(status_song.id).executeAsOne().toThumbnailProvider()
                            ?: return@getCustomImage null

                        MediaItemThumbnailLoader.loadItemThumbnail(status_song, thumbnail_provider, MediaItemThumbnailProvider.Quality.LOW, context).getOrThrowHere()
                    }.getOrNull()

                    val artist = songQueries.artistById(status_song.id).executeAsOne().artist?.let { ArtistRef(it) }
                    if (artist != null) {
                        small_image = getCustomImage(artist.id) {
                            val thumbnail_provider = mediaItemQueries.thumbnailProviderById(artist.id).executeAsOne().toThumbnailProvider()
                                ?: return@getCustomImage null

                            MediaItemThumbnailLoader.loadItemThumbnail(artist, thumbnail_provider, MediaItemThumbnailProvider.Quality.LOW, context).getOrThrowHere()
                        }.getOrNull()
                    } else {
                        small_image = null
                    }
                } catch (e: Throwable) {
                    return@launch
                }

                val buttons = mutableListOf<Pair<String, String>>().apply {
                    if (Settings.KEY_DISCORD_SHOW_BUTTON_SONG.get()) {
                        add(Settings.KEY_DISCORD_BUTTON_SONG_TEXT.get<String>() to status_song.getURL(context))
                    }
                    if (Settings.KEY_DISCORD_SHOW_BUTTON_PROJECT.get()) {
                        add(Settings.KEY_DISCORD_BUTTON_PROJECT_TEXT.get<String>() to getString("project_url"))
                    }
                }

                setActivity(
                    name = name,
                    type = DiscordStatus.Type.LISTENING,
                    details = text_a.ifEmpty { null },
                    state = text_b.ifEmpty { null },
                    buttons = buttons.ifEmpty { null },
                    large_image = large_image,
                    large_text = text_c.ifEmpty { null },
                    small_image = small_image,
                    small_text =
                    if (small_image != null) status_song.Artist.get(context.database)?.Title?.get(context.database)
                    else null,
                    application_id = DISCORD_APPLICATION_ID
                )
            }
        }
    }
}

