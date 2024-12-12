package com.toasterofbread.spmp.platform.playerservice.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.annotation.OptIn
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import app.cash.sqldelight.Query
import com.toasterofbread.spmp.model.mediaitem.loader.MediaItemThumbnailLoader
import com.toasterofbread.spmp.model.mediaitem.loader.SongLikedLoader
import com.toasterofbread.spmp.model.mediaitem.playlist.RemotePlaylist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.platform.PlayerListener
import com.toasterofbread.spmp.platform.playerservice.ForegroundPlayerService
import com.toasterofbread.spmp.platform.playerservice.formatMediaNotificationImage
import com.toasterofbread.spmp.platform.playerservice.toSong
import com.toasterofbread.spmp.shared.R
import dev.toastbits.composekit.context.isAppInForeground
import dev.toastbits.composekit.util.platform.launchSingle
import dev.toastbits.spms.socketapi.shared.SpMsPlayerState
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerServiceNotificationManager(
    private val context: AppContext,
    private val media_session: MediaSession,
    private val notification_manager: NotificationManager,
    private val service: ForegroundPlayerService,
    private val player: Player
) {
    private var current_song: Song? = null
    private val thumbnail_load_scope: CoroutineScope = CoroutineScope(Job())
    private val auth_state_observe_scope: CoroutineScope = CoroutineScope(Job())
    private val song_liked_load_scope: CoroutineScope = CoroutineScope(Job())

    private val metadata_builder: MediaMetadata.Builder = MediaMetadata.Builder()
    private val state: NotificationStateManager = NotificationStateManager(media_session)

    private val notification_listener: PlayerNotificationManager.NotificationListener =
        object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                ongoing: Boolean
            ) {
                service.startForeground(notificationId, notification)
            }

            override fun onNotificationCancelled(
                notificationId: Int,
                dismissedByUser: Boolean
            ) {
                if (!service.isAppInForeground()) {
                    service.stop()
                }
            }
        }

    private val player_listener: PlayerListener =
        object : PlayerListener() {
            override fun onSongTransition(song: Song?, manual: Boolean) {
                if (current_song == song) {
                    return
                }

                current_song?.also { current ->
                    context.database.songQueries.likedById(current.id).removeListener(song_liked_listener)
                }

                current_song = song
                state.update(
                    current_liked_status = song?.Liked?.get(context.database),
                    position_ms = player.currentPosition
                )

                updateMetadata {
                    putString(MediaMetadata.METADATA_KEY_TITLE, song?.getActiveTitle(context.database))
                    putString(MediaMetadata.METADATA_KEY_ARTIST, song?.Artists?.get(context.database)?.firstOrNull()?.getActiveTitle(context.database))
                    putString(MediaMetadata.METADATA_KEY_ART_URI, song?.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH))

                    val album: RemotePlaylist? = song?.Album?.get(context.database)
                    putString(MediaMetadata.METADATA_KEY_ALBUM, album?.getActiveTitle(context.database))
                    putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, album?.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH))
                }

                if (song != null) {
                    context.database.songQueries.likedById(song.id).addListener(song_liked_listener)

                    song_liked_load_scope.launchSingle {
                        SongLikedLoader.loadSongLiked(
                            song.id,
                            context,
                            context.ytapi.user_auth_state?.SongLiked
                        ).onFailure {
                            RuntimeException("Ignoring exception while loading song (${song.id}) liked status for player notification", it).printStackTrace()
                        }
                    }
                }
            }

            override fun onSeeked(position_ms: Long) {
                state.update(position_ms = position_ms)
            }

            override fun onPlayingChanged(is_playing: Boolean) {
                state.update(paused = !is_playing)
            }

            override fun onEvents() {
                state.update(position_ms = player.currentPosition)
            }

            override fun onStateChanged(state: SpMsPlayerState) {
                this@PlayerServiceNotificationManager.state.update(
                    playback_state =
                        when (state) {
                            SpMsPlayerState.IDLE -> PlaybackState.STATE_NONE
                            SpMsPlayerState.BUFFERING -> PlaybackState.STATE_BUFFERING
                            SpMsPlayerState.READY -> null
                            SpMsPlayerState.ENDED -> PlaybackState.STATE_STOPPED
                        }
                )
            }

            override fun onDurationChanged(duration_ms: Long) {
                updateMetadata {
                    putLong(MediaMetadata.METADATA_KEY_DURATION, duration_ms)
                }
            }
        }

    private val song_liked_listener: Query.Listener = Query.Listener {
        state.update(current_liked_status = current_song?.Liked?.get(context.database))
    }

    init {
        service.addListener(player_listener)

        ensureNotificationChannel()

        val manager: PlayerNotificationManager = createNotificationManager()
        manager.setUseFastForwardAction(false)
        manager.setUseRewindAction(false)
        manager.setUsePlayPauseActions(true)
        manager.setPlayer(player)
        manager.setMediaSessionToken(media_session.sessionToken)
        manager.setSmallIcon(R.drawable.ic_spmp)

        snapshotFlow { context.ytapi.user_auth_state }
            .onEach {
                state.update(authenticated = it != null)
            }
            .launchIn(auth_state_observe_scope)
    }

    fun release() {
        service.removeListener(player_listener)
        state.release()
        thumbnail_load_scope.cancel()
        auth_state_observe_scope.cancel()
    }

    private fun onThumbnailLoaded(image: Bitmap?) {
        updateMetadata {
            putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, image)
        }
        context.onNotificationThumbnailLoaded(image)
    }

    private fun updateMetadata(update: MediaMetadata.Builder.() -> Unit) = synchronized(metadata_builder) {
        update(metadata_builder)
        media_session.setMetadata(metadata_builder.build())
    }

    private suspend fun loadThumbnail(song: Song): Bitmap? {
        for (quality in ThumbnailProvider.Quality.byQuality()) {
            val image: ImageBitmap =
                MediaItemThumbnailLoader.loadItemThumbnail(song, quality, context).getOrNull()
                ?: continue

            val formatted_image: Bitmap =
                formatMediaNotificationImage(
                    image.asAndroidBitmap(),
                    song,
                    context
                )

            return formatted_image
        }

        return null
    }

    private fun ensureNotificationChannel() {
        if (notification_manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return
        }

        notification_manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Playing media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
        )
    }

    private fun createNotificationManager(): PlayerNotificationManager {
        val manager_builder: PlayerNotificationManager.Builder =
            PlayerNotificationManager.Builder(
                context.ctx,
                NOTIFICATION_ID,
                NOTIFICATION_CHANNEL_ID
            )

        manager_builder.setMediaDescriptionAdapter(
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence =
                    player.currentMediaItem?.toSong()?.getActiveTitle(context.database).orEmpty()

                override fun getCurrentContentText(player: Player): CharSequence =
                    player.currentMediaItem?.toSong()?.Artists?.get(context.database)?.firstOrNull()
                        ?.getActiveTitle(context.database).orEmpty()

                override fun createCurrentContentIntent(player: Player): PendingIntent =
                    PendingIntent.getActivity(
                        context.ctx, 0,
                        AppContext.getMainActivityIntent(context.ctx),
                        PendingIntent.FLAG_IMMUTABLE
                    )

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    val song: Song? = player.currentMediaItem?.toSong()
                    if (song != null) {
                        thumbnail_load_scope.launch {
                            val image: Bitmap? = loadThumbnail(song)
                            onThumbnailLoaded(image)

                            if (image != null) {
                                callback.onBitmap(image)
                            }
                        }
                    }
                    return null
                }
            }
        )

        manager_builder.setNotificationListener(notification_listener)

        return manager_builder.build()
    }

    companion object {
        const val NOTIFICATION_ID: Int = 10
        const val NOTIFICATION_CHANNEL_ID: String = "media_channel_id"
    }
}
