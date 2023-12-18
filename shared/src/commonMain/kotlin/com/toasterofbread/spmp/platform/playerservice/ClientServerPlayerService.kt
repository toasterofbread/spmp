package com.toasterofbread.spmp.platform.playerservice

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
}
