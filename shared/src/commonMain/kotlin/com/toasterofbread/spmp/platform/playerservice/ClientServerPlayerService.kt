package com.toasterofbread.spmp.platform.playerservice

import com.toasterofbread.spmp.model.mediaitem.artist.Artist
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.platform.download.DownloadStatus
import okhttp3.Headers
import spms.socketapi.shared.SpMsClientInfo

interface ClientServerPlayerService: PlayerService {
    data class ServerInfo(
        val ip: String,
        val port: Int,
        val protocol: String,
        val name: String,
        val device_name: String,
        val machine_id: String,
        val spms_git_commit_hash: String
    )
    
    val connected_server: ServerInfo?
    
    suspend fun getPeers(): Result<List<SpMsClientInfo>>
    suspend fun sendAuthInfoToPlayers(ytm_auth: Pair<Artist?, Headers>?): Result<Unit>

    fun onSongFilesAdded(songs: List<DownloadStatus>)
    fun onSongFilesDeleted(songs: List<Song>)
    fun onLocalSongsSynced(songs: List<DownloadStatus>)
}
