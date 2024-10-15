package com.toasterofbread.spmp.service.playercontroller

import dev.toastbits.composekit.utils.common.launchSingle
import com.toasterofbread.spmp.db.Database
import com.toasterofbread.spmp.ProjectBuildConfig
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.platform.playerservice.PlayerServicePlayer
import dev.toastbits.composekit.utils.common.associateNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import spmp.shared.generated.resources.Res
import spmp.shared.generated.resources.discord_status_unknown_artist_replacement
import spmp.shared.generated.resources.project_url
import kotlin.math.absoluteValue

internal class DiscordStatusHandler(val player: PlayerServicePlayer, val context: AppContext) {
    private var discord_rpc: DiscordStatus? = null
    private val coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val load_coroutine_scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private val status_update_lock: Mutex = Mutex()

    private data class StatusInfo(
        val song: Song?,
        val title: String?,
        val timestamps: Timestamps?,
        val large_image_source: DiscordSettings.ImageSource,
        val small_image_source: DiscordSettings.ImageSource
    )

    private data class Timestamps(
        val start: Long,
        val end: Long
    ) {
        fun toPair(): Pair<Long, Long> = start to end

        override fun equals(other: Any?): Boolean = (
            other is Timestamps
            && (start - other.start).absoluteValue < 500
            && (end - other.end).absoluteValue < 500
        )
    }

    private var current_status: StatusInfo? = null

    fun release() {
        coroutine_scope.cancel()
        discord_rpc?.close()
    }

    suspend fun onDiscordAccountTokenChanged() {
        discord_rpc?.close()

        val account_token: String = context.settings.discord_auth.DISCORD_ACCOUNT_TOKEN.get()
        if (!DiscordStatus.isSupported() || (account_token.isBlank() && DiscordStatus.isAccountTokenRequired())) {
            discord_rpc = null
            return
        }

        discord_rpc = DiscordStatus(
            context = context,
            application_id = ProjectBuildConfig.DISCORD_APPLICATION_ID,
            account_token = account_token
        )

        updateDiscordStatus(null)
    }

    private suspend fun Database.formatText(text: String, song: Song, title: String): String {
        val artist_ids: List<String>? = songQueries.artistsById(song.id).executeAsOne().artists?.let { Json.decodeFromString(it) }
        val artist_title: String? = artist_ids?.firstOrNull()?.let {
            mediaItemQueries.titleById(it).executeAsOne().title
        }

        return text
            .replace("\$artist", artist_title ?: getString(Res.string.discord_status_unknown_artist_replacement))
            .replace("\$song", title)
    }

    fun updateDiscordStatus(song: Song?): Unit = with(context.database) { load_coroutine_scope.launch {
        status_update_lock.withLock {
            val status_song: Song? = song ?: player.getSong()
            val song_title: String? = status_song?.getActiveTitle(context.database)

            val status_info: StatusInfo = withContext(Dispatchers.Main) {
                StatusInfo(
                    status_song,
                    song_title,
                    if (player.is_playing && player.duration_ms > 0L)
                        Timestamps(
                            System.currentTimeMillis() - player.current_position_ms,
                            System.currentTimeMillis() + player.duration_ms - player.current_position_ms
                        )
                    else null,
                    context.settings.discord.LARGE_IMAGE_SOURCE.get(),
                    context.settings.discord.SMALL_IMAGE_SOURCE.get()
                )
            }

            if (status_info == current_status) {
                return@launch
            }

            current_status = status_info

            coroutine_scope.launchSingle {
                discord_rpc?.apply {
                    if (status_song == null) {
                        close()
    //                    SpMp.Log.info("Discord status cancelled: No song")
                        return@apply
                    }

                    if (song_title == null) {
                        close()
    //                    SpMp.Log.info("Discord status cancelled: Song $status_song has no title")
                        return@apply
                    }

                    if (!shouldUpdateStatus()) {
                        close()
    //                    SpMp.Log.info("discord_rpc.shouldUpdateStatus() returned false")
                        return@apply
                    }

                    val name: String = formatText(context.settings.discord.STATUS_NAME.get(), status_song, song_title)
                    val text_a: String = formatText(context.settings.discord.STATUS_TEXT_A.get(), status_song, song_title)
                    val text_b: String = formatText(context.settings.discord.STATUS_TEXT_B.get(), status_song, song_title)
                    val text_c: String = formatText(context.settings.discord.STATUS_TEXT_C.get(), status_song, song_title)

                    val large_image: String?
                    val small_image: String?

    //                SpMp.Log.info("Loading Discord status images for $status_song ($song_title)...")

                    try {
                        val needed_images: Map<DiscordSettings.ImageSource, MediaItem> =
                            listOf(status_info.large_image_source, status_info.small_image_source)
                                .filter { it != DiscordSettings.ImageSource.ALT }
                                .distinct()
                                .associateNotNull { source ->
                                    val item: MediaItem =
                                        when (source) {
                                            DiscordSettings.ImageSource.SONG -> status_song
                                            DiscordSettings.ImageSource.ARTIST -> status_song.Artists.get(context.database)?.firstOrNull()
                                            DiscordSettings.ImageSource.ALBUM -> status_song.Album.get(context.database)
                                            DiscordSettings.ImageSource.ALT -> null
                                        } ?: return@associateNotNull null

                                    return@associateNotNull source to item
                                }

                        val images: List<String?> =
                            getCustomImages(
                                needed_images.values.toList(),
                                ThumbnailProvider.Quality.LOW
                            ).getOrThrow()

                        large_image = images.getOrNull(needed_images.keys.indexOf(status_info.large_image_source))
                        small_image = images.getOrNull(needed_images.keys.indexOf(status_info.small_image_source))
                    }
                    catch (e: Throwable) {
                        RuntimeException("WARNING: Failed loading Discord status images for $status_song ($song_title)", e).printStackTrace()
                        return@apply
                    }

                    val buttons: MutableList<Pair<String, String>> = mutableListOf<Pair<String, String>>().apply {
                        if (context.settings.discord.SHOW_SONG_BUTTON.get()) {
                            add(context.settings.discord.SONG_BUTTON_TEXT.get() to status_song.getUrl(context))
                        }
                        if (context.settings.discord.SHOW_PROJECT_BUTTON.get()) {
                            add(context.settings.discord.PROJECT_BUTTON_TEXT.get() to getString(Res.string.project_url))
                        }
                    }

    //                SpMp.Log.info("Setting Discord status for song $status_song ($song_title)...")

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
                            if (small_image != null) status_song.Artists.get(context.database)?.firstOrNull()?.getActiveTitle(context.database)
                            else null,
                        timestamps = status_info.timestamps?.toPair()
                    )

    //                SpMp.Log.info("Discord status set for song $status_song ($song_title)")
                }
            }
        }
    } }
}
