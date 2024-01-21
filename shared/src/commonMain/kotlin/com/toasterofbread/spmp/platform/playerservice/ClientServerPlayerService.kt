package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.DownloadStatus
import okhttp3.Headers

interface ClientServerPlayerService: PlayerService {
    data class ServerInfo(
        val ip: String,
        val port: Int,
        val protocol: String,
        val name: String,
        val device_name: String,
        val spms_git_commit_hash: String
    )
    
    val connected_server: ServerInfo?
    
    suspend fun getPeers(): Result<List<SpMsClientInfo>>
    suspend fun sendStatusToPlayers(ytm_auth: Pair<Artist?, Headers>?, local_files: Map<String, String>): Result<Unit>

    fun onSongFileAdded(download_status: DownloadStatus)
    fun onSongFileDeleted(song: Song)
    fun onLocalSongsSynced(songs: Map<String, DownloadStatus>)
}
