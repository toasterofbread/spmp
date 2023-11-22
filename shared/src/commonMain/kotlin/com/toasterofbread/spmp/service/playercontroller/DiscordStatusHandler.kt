package com.toasterofbread.spmp.service.playercontroller

import SpMp
import com.toasterofbread.composekit.utils.common.launchSingle
import com.toasterofbread.db.Database
import com.toasterofbread.spmp.model.mediaitem.MediaItemThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.artist.ArtistRef
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.DiscordAuthSettings
import com.toasterofbread.spmp.model.settings.category.YoutubeAuthSettings
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.youtubeapi.impl.youtubemusic.getOrThrowHere
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

private const val DISCORD_APPLICATION_ID = "1081929293979992134"
private const val DISCORD_ASSET_ICON_PRIMARY = "1103702923852132372"

internal class DiscordStatusHandler(val player: PlayerServicePlayer, val context: AppContext) {
    private var discord_rpc: DiscordStatus? = null
    private val coroutine_scope = CoroutineScope(Dispatchers.Main)

    private var current_status_song: Song? = null
    private var current_status_title: String? = null

    fun release() {
        coroutine_scope.cancel()
        discord_rpc?.close()
    }

    fun onDiscordAccountTokenChanged() {
        discord_rpc?.close()

        val account_token = DiscordAuthSettings.Key.DISCORD_ACCOUNT_TOKEN.get<String>(context.getPrefs())
        if (!DiscordStatus.isSupported() || (account_token.isBlank() && DiscordStatus.isAccountTokenRequired())) {
            discord_rpc = null
            return
        }

        discord_rpc = DiscordStatus(
            context,
            account_token
        )

        updateDiscordStatus(null)
    }

    private fun Database.formatText(text: String, song: Song, title: String): String {
        val artist_id = songQueries.artistById(song.id).executeAsOne().artist
        val artist_title = artist_id?.let {
            mediaItemQueries.titleById(it).executeAsOne().title
        }

        return text
            .replace("\$artist", artist_title ?: getString("discord_status_unknown_artist_replacement"))
            .replace("\$song", title)
    }

    @Synchronized
    fun updateDiscordStatus(song: Song?): Unit = with(context.database) {
        val status_song: Song? = song ?: player.getSong()
        val song_title: String? = status_song?.getActiveTitle(context.database)

        if (status_song == current_status_song && song_title == current_status_title) {
            return
        }

        current_status_song = status_song
        current_status_title = song_title

        coroutine_scope.launchSingle {
            discord_rpc?.apply {
                if (status_song == null) {
                    close()
                    SpMp.Log.info("Discord status cancelled: No song")
                    return@apply
                }

                if (song_title == null) {
                    close()
                    SpMp.Log.info("Discord status cancelled: Song $status_song has no title")
                    return@apply
                }

                if (!shouldUpdateStatus()) {
                    close()
                    SpMp.Log.info("discord_rpc.shouldUpdateStatus() returned false")
                    return@apply
                }

                val name = formatText(DiscordSettings.Key.STATUS_NAME.get(), status_song, song_title)
                val text_a = formatText(DiscordSettings.Key.STATUS_TEXT_A.get(), status_song, song_title)
                val text_b = formatText(DiscordSettings.Key.STATUS_TEXT_B.get(), status_song, song_title)
                val text_c = formatText(DiscordSettings.Key.STATUS_TEXT_C.get(), status_song, song_title)

                val large_image: String?
                val small_image: String?

                SpMp.Log.info("Loading Discord status images for $status_song ($song_title)...")

                try {
                    val artist: ArtistRef? = status_song.Artist.get(context.database)

                    val images: List<String?> = getCustomImages(listOfNotNull(status_song, artist), MediaItemThumbnailProvider.Quality.LOW).getOrThrowHere()

                    large_image = images.getOrNull(0)
                    small_image = images.getOrNull(1)
                }
                catch (e: Throwable) {
                    SpMp.Log.warning("Failed loading Discord status images for $status_song ($song_title)\n${e.stackTraceToString()}")
                    return@apply
                }

                val buttons = mutableListOf<Pair<String, String>>().apply {
                    if (DiscordSettings.Key.SHOW_SONG_BUTTON.get()) {
                        add(DiscordSettings.Key.SONG_BUTTON_TEXT.get<String>() to status_song.getURL(context))
                    }
                    if (DiscordSettings.Key.SHOW_PROJECT_BUTTON.get()) {
                        add(DiscordSettings.Key.PROJECT_BUTTON_TEXT.get<String>() to getString("project_url"))
                    }
                }

                SpMp.Log.info("Setting Discord status for song $status_song ($song_title)...")

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
                        if (small_image != null) status_song.Artist.get(context.database)?.getActiveTitle(context.database)
                        else null,
                    application_id = DISCORD_APPLICATION_ID
                )

                SpMp.Log.info("Discord status set for song $status_song ($song_title)")
            }
        }
    }
}
